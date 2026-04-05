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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
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
        AndroidView(
            modifier = modifier.fillMaxSize(),
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
                mapView.onResume()
                mapView
            },
            onRelease = { view ->
                view.onPause()
            }
        )
    } else {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Se requiere permiso de ubicación para mostrar el mapa.")
        }
    }
}


