package com.example.citizenreportai.ui.screens.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
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
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(
    repository: ReportRepository,
    userId: String,
    onNavigateBack: () -> Unit,
    onReportClick: (String) -> Unit,
    canManageAll: Boolean = false,
    reporterNames: Map<String, String> = emptyMap()
) {
    val allReports by repository.reports.collectAsState()
    val scope = rememberCoroutineScope()
    val spacing = AppTheme.spacing

    var isInitialLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf<ReportStatus?>(null) }

    // El funcionario gestiona todos los reportes; el ciudadano solo ve los suyos.
    val myReports = remember(allReports, userId, canManageAll) {
        if (canManageAll) allReports else allReports.filter { it.userId == userId }
    }
    val filteredReports = remember(myReports, selectedFilter) {
        if (selectedFilter == null) myReports else myReports.filter { it.status == selectedFilter }
    }

    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            try {
                repository.fetchReports()
                errorMessage = null
            } catch (_: Exception) {
                errorMessage = "No se pudieron actualizar los reportes."
            } finally {
                pullToRefreshState.endRefresh()
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isInitialLoading = true
                errorMessage = null
                repository.fetchReports()
            } catch (_: Exception) {
                errorMessage = "No se pudieron cargar los reportes. Intenta nuevamente."
            } finally {
                isInitialLoading = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (canManageAll) "Gestión de reportes" else "Mis reportes",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (myReports.isNotEmpty()) {
                            Text(
                                text = "${myReports.size} en total",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (myReports.isNotEmpty()) {
                    StatusFilterRow(
                        selected = selectedFilter,
                        onSelect = { selectedFilter = it },
                        myReports = myReports
                    )
                }

                when {
                    isInitialLoading -> {
                        LoadingState(message = "Cargando tus reportes…")
                    }
                    errorMessage != null && myReports.isEmpty() -> {
                        EmptyState(
                            icon = Icons.AutoMirrored.Outlined.Assignment,
                            title = "No pudimos cargar tus reportes",
                            description = errorMessage ?: "Revisa tu conexión e intenta de nuevo.",
                            modifier = Modifier.fillMaxSize(),
                            action = {
                                PrimaryButton(
                                    text = "Reintentar",
                                    onClick = {
                                        scope.launch {
                                            try {
                                                isInitialLoading = true
                                                errorMessage = null
                                                repository.fetchReports()
                                            } catch (_: Exception) {
                                                errorMessage = "No se pudieron cargar los reportes. Intenta nuevamente."
                                            } finally {
                                                isInitialLoading = false
                                            }
                                        }
                                    }
                                )
                            }
                        )
                    }
                    myReports.isEmpty() -> {
                        EmptyState(
                            icon = Icons.AutoMirrored.Outlined.Assignment,
                            title = if (canManageAll) "No hay reportes" else "Aún no tienes reportes",
                            description = if (canManageAll) "Cuando los ciudadanos reporten problemas aparecerán aquí."
                                          else "Cuando reportes un problema en tu ciudad aparecerá aquí.",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    filteredReports.isEmpty() -> {
                        EmptyState(
                            icon = Icons.AutoMirrored.Outlined.Assignment,
                            title = "Sin reportes en este estado",
                            description = "Prueba con otro filtro para ver más resultados.",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = spacing.lg,
                                end = spacing.lg,
                                top = spacing.sm,
                                bottom = spacing.xl
                            ),
                            verticalArrangement = Arrangement.spacedBy(spacing.md)
                        ) {
                            items(filteredReports) { report ->
                                ReportListItem(
                                    report = report,
                                    reporterName = if (canManageAll) report.userId?.let { reporterNames[it] } else null,
                                    onClick = { report.id?.let { onReportClick(it) } }
                                )
                            }
                        }
                    }
                }
            }

            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )

            if (errorMessage != null && myReports.isNotEmpty()) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(spacing.lg)
                ) {
                    Text(text = errorMessage!!)
                }
            }
        }
    }
}

@Composable
private fun StatusFilterRow(
    selected: ReportStatus?,
    onSelect: (ReportStatus?) -> Unit,
    myReports: List<Report>
) {
    val spacing = AppTheme.spacing
    val items = listOf(
        null                            to "Todos",
        ReportStatus.PENDIENTE          to "Pendientes",
        ReportStatus.EN_REVISION        to "En revisión",
        ReportStatus.PROGRAMADO         to "Programados",
        ReportStatus.RESUELTO           to "Resueltos",
        ReportStatus.RECHAZADO          to "Rechazados"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        items.forEach { (status, label) ->
            val count = if (status == null) myReports.size else myReports.count { it.status == status }
            FilterPill(
                label = label,
                count = count,
                selected = selected == status,
                onClick = { onSelect(status) }
            )
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = fg
            )
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = fg
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportListItem(report: Report, onClick: () -> Unit, reporterName: String? = null) {
    val spacing = AppTheme.spacing
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale("es")) }
    val photoUrl = report.photos.firstOrNull()?.photoUrl?.takeIf { it.startsWith("http", ignoreCase = true) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            ReportThumbnail(photoUrl = photoUrl)

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        CategoryChip(category = report.category)
                        StatusChip(status = report.status)
                    }
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                report.content?.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Spacer(Modifier.height(spacing.sm))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                }

                if (reporterName != null) {
                    Spacer(Modifier.height(spacing.sm))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = reporterName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.height(spacing.sm))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Icon(
                        Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = dateFormat.format(report.dateReported),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportThumbnail(photoUrl: String?) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl != null) {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    CircularProgressIndicator(
                        strokeWidth = 1.5.dp,
                        modifier = Modifier.size(20.dp)
                    )
                },
                error = {
                    Icon(
                        Icons.Outlined.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            )
        } else {
            Icon(
                Icons.Outlined.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
