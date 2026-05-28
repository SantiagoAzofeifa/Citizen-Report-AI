package com.example.citizenreportai.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.citizenreportai.util.LocationUtils
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun OpenStreetMapComponent(
    modifier: Modifier = Modifier,
    onLocationDetermined: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

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

    var isGpsEnabled by remember { mutableStateOf(LocationUtils.isLocationEnabled(context)) }

    // Re-check GPS state when the screen comes back from settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGpsEnabled = LocationUtils.isLocationEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

    when {
        !hasLocationPermission -> {
            LocationDisabledState(
                title = "Permiso de ubicación requerido",
                description = "Necesitamos tu ubicación para fijar el punto exacto del reporte.",
                actionLabel = "Conceder permiso",
                onAction = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                modifier = modifier
            )
        }
        !isGpsEnabled -> {
            LocationDisabledState(
                title = "Activa la ubicación",
                description = "Tu GPS está apagado. Actívalo para que podamos mostrar tu posición actual.",
                actionLabel = "Abrir ajustes",
                onAction = { LocationUtils.openLocationSettings(context) },
                modifier = modifier
            )
        }
        else -> {
            MapWithPin(
                modifier = modifier,
                fusedLocationClient = fusedLocationClient,
                onLocationDetermined = onLocationDetermined
            )
        }
    }
}

@Composable
private fun MapWithPin(
    modifier: Modifier,
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onLocationDetermined: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    var isMoving by remember { mutableStateOf(false) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var hasCenteredOnUser by remember { mutableStateOf(false) }

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

    fun centerOnUser() {
        val map = mapViewRef ?: return
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        if (!hasPerm) return
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        map.controller.animateTo(GeoPoint(location.latitude, location.longitude))
                        onLocationDetermined(location.latitude, location.longitude)
                        hasCenteredOnUser = true
                    }
                }
        } catch (_: SecurityException) { /* no-op */ }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val mapView = MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(18.0)
                }

                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), mapView)
                // Hide built-in person/arrow icons (we use our own central pin)
                val transparentBitmap = android.graphics.Bitmap
                    .createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
                    .apply { setPixel(0, 0, android.graphics.Color.TRANSPARENT) }
                locationOverlay.setPersonIcon(transparentBitmap)
                locationOverlay.setDirectionArrow(transparentBitmap, transparentBitmap)
                locationOverlay.enableMyLocation()
                mapView.overlays.add(locationOverlay)

                // First-fix fallback if lastLocation is null
                locationOverlay.runOnFirstFix {
                    val loc = locationOverlay.myLocation
                    if (loc != null && !hasCenteredOnUser) {
                        mapView.post {
                            mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
                            onLocationDetermined(loc.latitude, loc.longitude)
                            hasCenteredOnUser = true
                        }
                    }
                }

                mapView.addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?): Boolean {
                        val center = mapView.mapCenter
                        onLocationDetermined(center.latitude, center.longitude)
                        return true
                    }
                    override fun onZoom(event: ZoomEvent?): Boolean = true
                })

                mapView.setOnTouchListener { v, motionEvent ->
                    when (motionEvent.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            isMoving = true
                            v.performClick()
                        }
                        android.view.MotionEvent.ACTION_MOVE -> isMoving = true
                        android.view.MotionEvent.ACTION_UP,
                        android.view.MotionEvent.ACTION_CANCEL -> isMoving = false
                    }
                    false
                }

                mapViewRef = mapView
                mapView
            }
        )

        // ── Pin central ──────────────────────────────
        Box(modifier = Modifier.align(Alignment.Center)) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isMoving) Color.Black.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.3f))
                    .align(Alignment.Center)
                    .graphicsLayer {
                        val shadowScale = if (isMoving) 0.6f else 1f
                        scaleX = shadowScale
                        scaleY = shadowScale
                    }
            )
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = "Posición seleccionada",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(bottom = 42.dp)
                    .size(42.dp)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationY = offset.toPx()
                        transformOrigin = TransformOrigin(0.5f, 1.0f)
                    }
            )
            Box(
                modifier = Modifier
                    .padding(bottom = 58.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationY = offset.toPx()
                    }
            )
        }

        // ── Pill de instrucciones ────────────────────
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
        ) {
            Text(
                text = "Mueve el mapa para ubicar el reporte",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        // ── Botón flotante "centrar en mí" ───────────
        FloatingActionButton(
            onClick = { centerOnUser() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(48.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = CircleShape
        ) {
            Icon(
                Icons.Outlined.MyLocation,
                contentDescription = "Centrar en mi ubicación",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun LocationDisabledState(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.LocationOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            PrimaryButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
