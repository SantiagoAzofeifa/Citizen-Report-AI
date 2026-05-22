package com.example.citizenreportai.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.google.android.gms.location.LocationServices

@Composable
fun OpenStreetMapComponent(
    modifier: Modifier = Modifier,
    onLocationDetermined: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Configurar Osmdroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasCenteredOnUser by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasLocationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    if (hasLocationPermission) {
        Card(
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            var isMoving by remember { mutableStateOf(false) }

            val scale by animateFloatAsState(
                targetValue = if (isMoving) 1.5f else 1.2f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "LocationPinScale"
            )

            val offset by animateDpAsState(
                targetValue = if (isMoving) (-10).dp else 0.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "LocationPinOffset"
            )

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val buttonScale by animateFloatAsState(
                targetValue = if (isPressed) 0.92f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "ButtonScale"
            )

            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val mapView = MapView(ctx)
                        mapView.setTileSource(TileSourceFactory.MAPNIK)
                        mapView.setMultiTouchControls(true)
                        mapView.controller.setZoom(18.0)

                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), mapView)

                        // Crear un bitmap transparente de 1x1 pixel
                        val transparentBitmap = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888).apply {
                            setPixel(0, 0, android.graphics.Color.TRANSPARENT)
                        }

                        // Asignamos el bitmap transparente a los íconos de la librería
                        locationOverlay.setPersonIcon(transparentBitmap)
                        locationOverlay.setDirectionArrow(transparentBitmap, transparentBitmap)

                        locationOverlay.enableMyLocation()
                        // Not enabling follow because we want the centered pin approach
                        // locationOverlay.enableFollowLocation()
                        mapView.overlays.add(locationOverlay)

                        if (!hasCenteredOnUser) {
                            fun centerFromOverlay() {
                                locationOverlay.runOnFirstFix {
                                    val location = locationOverlay.myLocation
                                    if (location != null) {
                                        mapView.post {
                                            mapView.controller.animateTo(GeoPoint(location.latitude, location.longitude))
                                            onLocationDetermined(location.latitude, location.longitude)
                                            hasCenteredOnUser = true
                                        }
                                    }
                                }
                            }

                            val hasRuntimeLocationPermission =
                                ContextCompat.checkSelfPermission(
                                    ctx,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED ||
                                    ContextCompat.checkSelfPermission(
                                        ctx,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                            if (hasRuntimeLocationPermission) {
                                fusedLocationClient.lastLocation
                                    .addOnSuccessListener { location ->
                                        if (location != null && !hasCenteredOnUser) {
                                            mapView.post {
                                                mapView.controller.animateTo(GeoPoint(location.latitude, location.longitude))
                                                onLocationDetermined(location.latitude, location.longitude)
                                                hasCenteredOnUser = true
                                            }
                                        } else if (!hasCenteredOnUser) {
                                            centerFromOverlay()
                                        }
                                    }
                                    .addOnFailureListener {
                                        if (!hasCenteredOnUser) {
                                            centerFromOverlay()
                                        }
                                    }
                            } else {
                                centerFromOverlay()
                            }
                        }

                        mapView.addMapListener(object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean {
                                val center = mapView.mapCenter
                                onLocationDetermined(center.latitude, center.longitude)
                                return true
                            }

                            override fun onZoom(event: ZoomEvent?): Boolean {
                                return true
                            }
                        })

                        mapView.setOnTouchListener { v, motionEvent ->
                            when (motionEvent.action) {
                                android.view.MotionEvent.ACTION_DOWN -> {
                                    isMoving = true
                                    v.performClick()
                                }
                                android.view.MotionEvent.ACTION_MOVE -> {
                                    isMoving = true
                                }
                                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                                    isMoving = false
                                }
                            }
                            false // let the map handle it
                        }

                        mapView
                    },
                    update = { _ ->
                        // Puedes manejar actualizaciones aquí
                    }
                )

                // Animated modern pin overlay
                Box(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    // Círculo de sombra/indicador del suelo
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                color = if (isMoving) Color.Black.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.3f)
                            )
                            .align(Alignment.Center)
                            .graphicsLayer {
                                val shadowScale = if (isMoving) 0.6f else 1f
                                scaleX = shadowScale
                                scaleY = shadowScale
                            }
                    )

                    // Pin principal
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = "Posición seleccionada",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(bottom = 42.dp) // Alineación precisa para la punta
                            .offset(y = offset)
                            .size(42.dp) // Tamaño ligeramente más grande y profesional
                            .align(Alignment.Center)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1.0f)
                            }
                    )

                    // Punto interir del Pin para estilo de marcador de alta precisión
                    Box(
                        modifier = Modifier
                            .padding(bottom = 58.dp) // Alineado con el centro cóncavo del `Place`
                            .offset(y = offset)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary)
                            .align(Alignment.Center)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                    )
                }

                // Instructions pill
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "Mueve el mapa para ubicar el reporte",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Button(
                    onClick = { /* Acción del botón */ },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .graphicsLayer {
                            scaleX = buttonScale
                            scaleY = buttonScale
                        }
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Enviar Reporte")
                }
            }
        }
    } else {
        Card(
            modifier = modifier.fillMaxSize(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Se requiere permiso de ubicación para mostrar el mapa.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
