package com.example.citizenreportai.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun LocationPreviewMap(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx -> buildPreviewMap(ctx, latitude, longitude) },
        update = { mapView ->
            mapView.overlays.clear()
            mapView.overlays.add(buildMarker(mapView, latitude, longitude))
            mapView.controller.setCenter(GeoPoint(latitude, longitude))
            mapView.invalidate()
        }
    )
}

private fun buildPreviewMap(ctx: Context, latitude: Double, longitude: Double): MapView {
    return MapView(ctx).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(false)
        controller.setZoom(17.5)
        controller.setCenter(GeoPoint(latitude, longitude))
        overlays.add(buildMarker(this, latitude, longitude))
        setOnTouchListener { _, _ -> true }
    }
}

private fun buildMarker(mapView: MapView, latitude: Double, longitude: Double): Marker {
    val markerBitmap = Bitmap.createBitmap(70, 90, Bitmap.Config.ARGB_8888).apply {
        val canvas = Canvas(this)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val pinColor = android.graphics.Color.parseColor("#111827")
        val shadowColor = android.graphics.Color.parseColor("#33000000")

        paint.color = shadowColor
        canvas.drawOval(25f, 75f, 45f, 85f, paint)

        paint.color = pinColor
        val path = Path().apply {
            moveTo(35f, 80f)
            cubicTo(35f, 80f, 5f, 45f, 5f, 30f)
            cubicTo(5f, 10f, 20f, 5f, 35f, 5f)
            cubicTo(50f, 5f, 65f, 10f, 65f, 30f)
            cubicTo(65f, 45f, 35f, 80f, 35f, 80f)
            close()
        }
        canvas.drawPath(path, paint)

        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(35f, 30f, 12f, paint)
    }

    return Marker(mapView).apply {
        position = GeoPoint(latitude, longitude)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        icon = BitmapDrawable(mapView.context.resources, markerBitmap)
    }
}
