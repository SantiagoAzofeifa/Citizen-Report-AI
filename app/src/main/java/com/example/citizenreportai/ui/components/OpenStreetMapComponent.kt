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
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn

@Composable
fun OpenStreetMapComponent(
    modifier: Modifier = Modifier,
    onLocationDetermined: (Double, Double) -> Unit
) {
    val context = LocalContext.current

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
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val mapView = MapView(ctx)
                        mapView.setTileSource(TileSourceFactory.MAPNIK)
                        mapView.setMultiTouchControls(true)
                        mapView.controller.setZoom(18.0)

                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), mapView)
                        locationOverlay.enableMyLocation()
                        locationOverlay.enableFollowLocation()

                        locationOverlay.runOnFirstFix {
                            val location = locationOverlay.myLocation
                            if (location != null) {
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    mapView.controller.animateTo(location)
                                    onLocationDetermined(location.latitude, location.longitude)
                                }
                            }
                        }

                        mapView.overlays.add(locationOverlay)

                        mapView.addMapListener(object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean {
                                val center = mapView.mapCenter
                                onLocationDetermined(center.latitude, center.longitude)
                                return false
                            }

                            override fun onZoom(event: ZoomEvent?): Boolean {
                                return false
                            }
                        })

                        // Asegurarnos de que el mapa responde correctamente a redimensiones
                        mapView.setPadding(0, 0, 0, 0)
                        mapView.onResume()
                        mapView
                    },
                    onRelease = { view ->
                        view.onPause()
                    }
                )

                // Un puntero en el centro indicando dónde se ubica el reporte
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Marker",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .offset(y = (-18).dp) // Elevarlo ligeramente
                )
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
