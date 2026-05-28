package com.example.citizenreportai.ui.screens.reports

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.citizenreportai.data.model.Report
import com.example.citizenreportai.data.model.ReportStatus
import com.example.citizenreportai.data.repository.ReportRepository
import com.example.citizenreportai.ui.components.CategoryChip
import com.example.citizenreportai.ui.components.EmptyState
import com.example.citizenreportai.ui.components.LoadingState
import com.example.citizenreportai.ui.components.PrimaryButton
import com.example.citizenreportai.ui.components.StatusChip
import com.example.citizenreportai.ui.theme.AppTheme
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
                if (report == null) errorMessage = "No se encontró el reporte."
            } catch (_: Exception) {
                errorMessage = "No se pudo cargar el reporte. Intenta nuevamente."
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Detalle del reporte",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> LoadingState(message = "Cargando reporte…")
                errorMessage != null -> EmptyState(
                    icon = Icons.Outlined.BrokenImage,
                    title = "No pudimos cargar el reporte",
                    description = errorMessage!!,
                    modifier = Modifier.fillMaxSize(),
                    action = {
                        PrimaryButton(
                            text = "Reintentar",
                            onClick = {
                                scope.launch {
                                    try {
                                        isLoading = true
                                        errorMessage = null
                                        report = repository.fetchReportById(reportId)
                                        if (report == null) errorMessage = "No se encontró el reporte."
                                    } catch (_: Exception) {
                                        errorMessage = "No se pudo cargar el reporte. Intenta nuevamente."
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        )
                    }
                )
                report != null -> ReportDetailContent(report = report!!)
            }
        }
    }
}

@Composable
private fun ReportDetailContent(report: Report) {
    val spacing = AppTheme.spacing
    val dateFormat = remember { SimpleDateFormat("d 'de' MMMM, yyyy · HH:mm", Locale("es")) }
    var fullscreenPhoto by remember { mutableStateOf<String?>(null) }

    if (fullscreenPhoto != null) {
        FullscreenPhotoDialog(
            url = fullscreenPhoto!!,
            onDismiss = { fullscreenPhoto = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.lg)
            .padding(bottom = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.xl)
    ) {
        // ── Header (chips + fecha) ───────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                CategoryChip(category = report.category)
                StatusChip(status = report.status)
            }
            Text(
                text = "Reportado el ${dateFormat.format(report.dateReported)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ── Timeline de estado ───────────────────────────
        StatusTimeline(currentStatus = report.status)

        // ── Foto (si hay) ────────────────────────────────
        if (report.photos.isNotEmpty()) {
            SectionBlock(title = "Evidencia") {
                report.photos.forEach { photo ->
                    SubcomposeAsyncImage(
                        model = photo.photoUrl,
                        contentDescription = "Foto del reporte",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(MaterialTheme.shapes.large)
                            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
                            .clickable(enabled = photo.photoUrl.startsWith("http", ignoreCase = true)) {
                                fullscreenPhoto = photo.photoUrl
                            },
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(strokeWidth = 2.dp)
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.BrokenImage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    )
                    Spacer(Modifier.height(spacing.sm))
                }
            }
        }

        // ── Descripción ──────────────────────────────────
        report.content?.description?.takeIf { it.isNotBlank() }?.let { description ->
            SectionBlock(title = "Descripción") {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(spacing.lg)
                    )
                }
            }
        }

        // ── Comentario de cierre (solo resueltos) ────────
        if (report.status == ReportStatus.RESUELTO) {
            report.content?.closingComment?.takeIf { it.isNotBlank() }?.let { comment ->
                SectionBlock(title = "Resolución") {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(spacing.lg),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                        ) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = comment,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        // ── Ubicación ────────────────────────────────────
        SectionBlock(title = "Ubicación") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(MaterialTheme.shapes.large)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            ) {
                ReportLocationMap(
                    latitude = report.latitude,
                    longitude = report.longitude,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(spacing.sm))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "%.5f, %.5f".format(report.latitude, report.longitude),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusTimeline(currentStatus: ReportStatus) {
    val spacing = AppTheme.spacing
    // Final state can be RESUELTO or RECHAZADO; PROGRAMADO is between EN_REVISION and RESUELTO.
    val steps = listOf(
        ReportStatus.PENDIENTE   to "Recibido",
        ReportStatus.EN_REVISION to "En revisión",
        ReportStatus.PROGRAMADO  to "Programado",
        ReportStatus.RESUELTO    to "Resuelto"
    )
    val order = listOf(ReportStatus.PENDIENTE, ReportStatus.EN_REVISION, ReportStatus.PROGRAMADO, ReportStatus.RESUELTO)
    val rejected = currentStatus == ReportStatus.RECHAZADO
    val currentIdx = order.indexOf(currentStatus).takeIf { it >= 0 } ?: 0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(spacing.lg)) {
            Text(
                text = if (rejected) "Estado del reporte" else "Progreso",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(spacing.md))

            if (rejected) {
                RejectedState()
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    steps.forEachIndexed { idx, (status, label) ->
                        val isActive = idx <= currentIdx
                        val isCurrent = idx == currentIdx
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            StepDot(active = isActive, current = isCurrent)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isActive) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (idx < steps.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .weight(0.5f)
                                    .height(2.dp)
                                    .padding(top = 11.dp)
                                    .background(
                                        if (idx < currentIdx) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepDot(active: Boolean, current: Boolean) {
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(if (current) 24.dp else 20.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        if (active && !current) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(12.dp)
            )
        }
        if (current) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary)
            )
        }
    }
}

@Composable
private fun RejectedState() {
    val spacing = AppTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "!",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onError
            )
        }
        Column {
            Text(
                text = "Reporte rechazado",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Este reporte no pudo ser procesado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
    val spacing = AppTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        content()
    }
}

@Composable
private fun FullscreenPhotoDialog(url: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            var scale by remember { mutableStateOf(1f) }
            var offsetX by remember { mutableStateOf(0f) }
            var offsetY by remember { mutableStateOf(0f) }

            SubcomposeAsyncImage(
                model = url,
                contentDescription = "Foto del reporte en pantalla completa",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    ),
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                },
                error = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.BrokenImage,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White
                )
            }
        }
    }
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

                val customIcon = BitmapDrawable(ctx.resources, markerBitmap)
                val marker = Marker(this).apply {
                    position = GeoPoint(latitude, longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = customIcon
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
