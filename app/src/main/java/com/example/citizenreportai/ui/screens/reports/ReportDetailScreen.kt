package com.example.citizenreportai.ui.screens.reports

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.SubcomposeAsyncImage
import com.example.citizenreportai.BuildConfig
import com.example.citizenreportai.data.model.Report
import com.example.citizenreportai.data.model.ReportStatus
import com.example.citizenreportai.data.repository.ReportRepository
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    repository: ReportRepository,
    reportId: String,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<Report?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reportId) {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null
                report = repository.fetchReportById(reportId)
                if (report == null) {
                    errorMessage = "No se encontró el reporte."
                }
            } catch (e: Exception) {
                errorMessage = "No se pudo cargar el reporte. Intenta nuevamente."
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Reporte", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = {
                            scope.launch {
                                try {
                                    isLoading = true
                                    errorMessage = null
                                    report = repository.fetchReportById(reportId)
                                    if (report == null) errorMessage = "No se encontró el reporte."
                                } catch (e: Exception) {
                                    errorMessage = "No se pudo cargar el reporte. Intenta nuevamente."
                                } finally {
                                    isLoading = false
                                }
                            }
                        }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            report != null -> {
                ReportDetailContent(
                    report = report!!,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun ReportDetailContent(report: Report, modifier: Modifier = Modifier) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    val statusColor = when (report.status) {
        ReportStatus.PENDIENTE -> Color(0xFFF97316)
        ReportStatus.EN_REVISION -> Color(0xFF3B82F6)
        ReportStatus.RESUELTO -> Color(0xFF22C55E)
        ReportStatus.RECHAZADO -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.outline
    }

    val statusLabel = when (report.status) {
        ReportStatus.PENDIENTE -> "Pendiente"
        ReportStatus.EN_REVISION -> "En Revisión"
        ReportStatus.PROGRAMADO -> "Programado"
        ReportStatus.RESUELTO -> "Resuelto"
        ReportStatus.RECHAZADO -> "Rechazado"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: Category + Status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = report.category.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = statusLabel,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        HorizontalDivider()

        // Date
        DetailRow(label = "Fecha de reporte", value = dateFormat.format(report.dateReported))

        // Description
        report.content?.description?.let { description ->
            if (description.isNotBlank()) {
                DetailSection(title = "Descripción") {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Closing comment (only when resolved)
        if (report.status == ReportStatus.RESUELTO) {
            report.content?.closingComment?.let { comment ->
                if (comment.isNotBlank()) {
                    DetailSection(title = "Comentario de cierre") {
                        Text(
                            text = comment,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Location
        DetailSection(title = "Ubicación") {
            ReportLocationMap(
                latitude = report.latitude,
                longitude = report.longitude,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Lat: ${"%.6f".format(report.latitude)},  Lon: ${"%.6f".format(report.longitude)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Photos
        if (report.photos.isNotEmpty()) {
            DetailSection(title = "Fotos (${report.photos.size})") {
                report.photos.forEach { photo ->
                    val resolvedUrl = resolvePhotoUrl(photo.photoUrl)
                    SubcomposeAsyncImage(
                        model = resolvedUrl,
                        contentDescription = "Foto del reporte",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.BrokenImage,
                                    contentDescription = "No se pudo cargar la imagen",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

private fun resolvePhotoUrl(photoUrl: String): String {
    val trimmedUrl = photoUrl.trim()
    if (trimmedUrl.isBlank()) {
        return trimmedUrl
    }

    val normalizedUrl = trimmedUrl.replace("\\", "/")
    if (normalizedUrl.startsWith("http://", ignoreCase = true) ||
        normalizedUrl.startsWith("https://", ignoreCase = true) ||
        normalizedUrl.startsWith("content://", ignoreCase = true) ||
        normalizedUrl.startsWith("file://", ignoreCase = true) ||
        normalizedUrl.startsWith("data:", ignoreCase = true)
    ) {
        return normalizedUrl
    }

    val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')
    return "$baseUrl/${normalizedUrl.trimStart('/')}"
}

@Composable
private fun ReportLocationMap(
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
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(false)
                controller.setZoom(18.0)
                controller.setCenter(GeoPoint(latitude, longitude))

                val markerBitmap = Bitmap.createBitmap(70, 90, Bitmap.Config.ARGB_8888).apply {
                    val canvas = Canvas(this)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

                    val pinColor = android.graphics.Color.parseColor("#E63946")
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

                val customIcon = BitmapDrawable(ctx.resources, markerBitmap)

                val marker = Marker(this).apply {
                    position = GeoPoint(latitude, longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = customIcon
                    title = "Ubicación del reporte"
                }
                overlays.add(marker)

                setOnTouchListener { _, _ -> true }
            }
        },
        update = { mapView ->
            mapView.controller.setCenter(GeoPoint(latitude, longitude))
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}
