package com.example.citizenreportai.ui.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.citizenreportai.data.model.Report
import com.example.citizenreportai.data.model.ReportStatus
import com.example.citizenreportai.data.repository.ReportRepository
import kotlinx.coroutines.launch
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
                    Text(
                        text = photo.photoUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
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
