package com.example.citizenreportai.ui.screens.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.citizenreportai.util.LocationUtils
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.citizenreportai.data.model.Report
import com.example.citizenreportai.data.model.UserRole
import com.example.citizenreportai.data.repository.ReportRepository
import com.example.citizenreportai.ui.components.CategoryChip
import com.example.citizenreportai.ui.components.StatusChip
import com.example.citizenreportai.ui.theme.AppTheme
import com.example.citizenreportai.ui.theme.Success500
import com.example.citizenreportai.ui.theme.Warning500
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: ReportRepository,
    onNavigateToCreateReport: () -> Unit,
    onNavigateToMyReports: () -> Unit,
    onNavigateToProfile: () -> Unit,
    userRole: UserRole?,
    userFirstName: String?
) {
    val reports by repository.reports.collectAsState(initial = emptyList())
    val spacing = AppTheme.spacing

    LaunchedEffect(Unit) { repository.fetchReports() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AppBottomBar(
                selected = 0,
                onSelectMyReports = onNavigateToMyReports,
                onSelectProfile = onNavigateToProfile
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreateReport,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Reportar", style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GreetingHeader(
                userFirstName = userFirstName,
                userRole = userRole,
                onNavigateToProfile = onNavigateToProfile
            )

            StatsRow(reports = reports)

            Spacer(Modifier.height(spacing.lg))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = spacing.lg)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                    .clipToBounds()
            ) {
                ReportsMapComponent(reports = reports)
            }

            Spacer(Modifier.height(spacing.md))
        }
    }
}

@Composable
private fun GreetingHeader(
    userFirstName: String?,
    userRole: UserRole?,
    onNavigateToProfile: () -> Unit
) {
    val spacing = AppTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hola${userFirstName?.let { ", ${it.trim()}" } ?: ""}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = roleSubtitle(userRole),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AvatarButton(
            initials = (userFirstName?.firstOrNull()?.uppercase() ?: "U"),
            onClick = onNavigateToProfile
        )
    }
}

private fun roleSubtitle(role: UserRole?) = when (role) {
    UserRole.ADMIN       -> "Panel de administración"
    UserRole.FUNCIONARIO -> "Gestión de reportes"
    UserRole.USER, null  -> "Reporta lo que pasa en tu ciudad"
}

@Composable
private fun AvatarButton(initials: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsRow(reports: List<Report>) {
    val spacing = AppTheme.spacing
    val total     = reports.size
    val pending   = reports.count { it.status.name == "PENDIENTE" }
    val resolved  = reports.count { it.status.name == "RESUELTO" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        StatTile(value = total.toString(),    label = "Total",      accent = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        StatTile(value = pending.toString(),  label = "Pendientes", accent = Warning500, modifier = Modifier.weight(1f))
        StatTile(value = resolved.toString(), label = "Resueltos",  accent = Success500, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AppBottomBar(
    selected: Int,
    onSelectMyReports: () -> Unit,
    onSelectProfile: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        val itemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            indicatorColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        NavigationBarItem(
            selected = selected == 0,
            onClick = { /* already here */ },
            icon = { Icon(Icons.Outlined.Home, contentDescription = "Inicio") },
            label = { Text("Inicio", style = MaterialTheme.typography.labelSmall) },
            colors = itemColors
        )
        NavigationBarItem(
            selected = selected == 1,
            onClick = onSelectMyReports,
            icon = { Icon(Icons.AutoMirrored.Outlined.List, contentDescription = "Mis reportes") },
            label = { Text("Reportes", style = MaterialTheme.typography.labelSmall) },
            colors = itemColors
        )
        NavigationBarItem(
            selected = selected == 2,
            onClick = onSelectProfile,
            icon = { Icon(Icons.Outlined.Person, contentDescription = "Perfil") },
            label = { Text("Perfil", style = MaterialTheme.typography.labelSmall) },
            colors = itemColors
        )
    }
}

@Composable
fun ReportsMapComponent(reports: List<Report>) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isGpsEnabled by remember { mutableStateOf(LocationUtils.isLocationEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGpsEnabled = LocationUtils.isLocationEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasLocationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    var selectedReport by remember { mutableStateOf<Report?>(null) }
    var hasCenteredOnUser by remember { mutableStateOf(false) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    fun centerOnUser() {
        val map = mapViewRef ?: return
        if (!hasLocationPermission || !isGpsEnabled) return
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    map.controller.animateTo(GeoPoint(location.latitude, location.longitude))
                    hasCenteredOnUser = true
                }
            }
        } catch (_: SecurityException) { /* no-op */ }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val mapView = MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(17.0)
                    if (reports.isEmpty()) {
                        controller.setCenter(GeoPoint(40.4168, -3.7038))
                    }
                }
                mapView.setOnClickListener { selectedReport = null }
                mapViewRef = mapView
                mapView
            },
            update = { mapView ->
                mapView.overlays.clear()

                if (hasLocationPermission) {
                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(mapView.context), mapView)
                    locationOverlay.enableMyLocation()
                    mapView.overlays.add(locationOverlay)

                    if (!hasCenteredOnUser) {
                        fun maybeCenter(latitude: Double, longitude: Double) {
                            mapView.post {
                                if (!hasCenteredOnUser) {
                                    mapView.controller.animateTo(GeoPoint(latitude, longitude))
                                    hasCenteredOnUser = true
                                }
                            }
                        }

                        fun centerFromOverlay() {
                            locationOverlay.runOnFirstFix {
                                val location = locationOverlay.myLocation
                                if (location != null) {
                                    maybeCenter(location.latitude, location.longitude)
                                }
                            }
                        }

                        val hasRuntimeLocationPermission =
                            ContextCompat.checkSelfPermission(mapView.context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(mapView.context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                        if (hasRuntimeLocationPermission) {
                            try {
                                fusedLocationClient.lastLocation
                                    .addOnSuccessListener { location ->
                                        if (location != null) maybeCenter(location.latitude, location.longitude)
                                        else if (!hasCenteredOnUser) centerFromOverlay()
                                    }
                                    .addOnFailureListener {
                                        if (!hasCenteredOnUser) centerFromOverlay()
                                    }
                            } catch (_: SecurityException) {
                                if (!hasCenteredOnUser) centerFromOverlay()
                            }
                        } else {
                            centerFromOverlay()
                        }
                    }
                }

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

                val customIcon = BitmapDrawable(mapView.context.resources, markerBitmap)

                reports.forEach { report ->
                    val marker = Marker(mapView)
                    marker.position = GeoPoint(report.latitude, report.longitude)
                    marker.icon = customIcon
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.setOnMarkerClickListener { _, _ ->
                        selectedReport = report
                        mapView.controller.animateTo(GeoPoint(report.latitude, report.longitude))
                        true
                    }
                    mapView.overlays.add(marker)
                }

                if (reports.isNotEmpty() && selectedReport == null && !hasCenteredOnUser) {
                    val lastReport = reports.last()
                    mapView.controller.setCenter(GeoPoint(lastReport.latitude, lastReport.longitude))
                }

                mapView.invalidate()
            }
        )

        // Banner discreto cuando GPS está apagado
        if (hasLocationPermission && !isGpsEnabled) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.errorContainer,
                onClick = { LocationUtils.openLocationSettings(context) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.LocationOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Ubicación apagada · toca para activar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Botón "centrar en mí" - top-end para no chocar con la FAB "Reportar"
        if (hasLocationPermission && isGpsEnabled) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 2.dp,
                onClick = { centerOnUser() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.MyLocation,
                        contentDescription = "Centrar en mi ubicación",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = selectedReport != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            selectedReport?.let { report ->
                SelectedReportCard(report = report, onClose = { selectedReport = null })
            }
        }
    }
}

@Composable
private fun SelectedReportCard(report: Report, onClose: () -> Unit) {
    val spacing = AppTheme.spacing
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    CategoryChip(category = report.category)
                    StatusChip(status = report.status)
                }
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            report.content?.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(Modifier.height(spacing.sm))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
