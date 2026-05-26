package com.example.citizenreportai.ui.screens.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List as ListIcon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.citizenreportai.data.model.Report
import com.example.citizenreportai.data.repository.ReportRepository
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.example.citizenreportai.data.model.UserRole
import com.google.android.gms.location.LocationServices
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: ReportRepository,
    onNavigateToCreateReport: () -> Unit,
    onNavigateToMyReports: () -> Unit,
    onNavigateToProfile: () -> Unit,
    userRole: UserRole?
) {
    val reports by repository.reports.collectAsState(initial = emptyList())

    // Cargar reportes al iniciar
    LaunchedEffect(Unit) {
        repository.fetchReports()
    }

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(if (selectedTab == 0) Icons.Default.Home else Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Explorar") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { 
                        selectedTab = 1
                        onNavigateToMyReports()
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.ListIcon, contentDescription = "Mis Reportes") },
                    label = { Text("Actividad") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { 
                        selectedTab = 2
                        onNavigateToProfile()
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") }
                )
            }
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onNavigateToCreateReport,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo Reporte", modifier = Modifier.size(36.dp))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // El mapa ocupa todo el fondo
            ReportsMapComponent(reports = reports)

            // Superposición de UI sobre el mapa
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                // Barra Superior Estilo Glassmorphism
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Citizen Report AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Reportes en tiempo real",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        IconButton(
                            onClick = { repository.fetchReports() },
                            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refrescar", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                // Estadísticas Rápidas (Solo si no hay un reporte seleccionado para no saturar)
                // Se ocultan cuando se selecciona un marcador (esto se maneja en el componente del mapa o con un estado compartido)
            }

            // Tarjeta de Resumen en la parte superior (Stats)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 100.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem(reports.size.toString(), "Total", MaterialTheme.colorScheme.primary)
                    StatItem(reports.count { it.status.name == "PENDIENTE" }.toString(), "Pendientes", Color(0xFFF97316))
                    StatItem(reports.count { it.status.name == "RESUELTO" }.toString(), "Resueltos", Color(0xFF22C55E))
                }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ReportsMapComponent(reports: List<Report>) {
    val context = LocalContext.current
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

                // Si el usuario toca el mapa general, cerramos la tarjeta
                mapView.setOnClickListener {
                    selectedReport = null
                }

                mapView
            },
            update = { mapView ->
                mapView.overlays.clear()

                // Mostrar ubicacion actual si hay permiso
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
                            ContextCompat.checkSelfPermission(
                                mapView.context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(
                                    mapView.context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED

                        if (hasRuntimeLocationPermission) {
                                try {
                                    fusedLocationClient.lastLocation
                                        .addOnSuccessListener { location ->
                                            if (location != null) {
                                                maybeCenter(location.latitude, location.longitude)
                                            } else if (!hasCenteredOnUser) {
                                                centerFromOverlay()
                                            }
                                        }
                                        .addOnFailureListener {
                                            if (!hasCenteredOnUser) {
                                                centerFromOverlay()
                                            }
                                        }
                                } catch (securityException: SecurityException) {
                                    if (!hasCenteredOnUser) {
                                        centerFromOverlay()
                                    }
                                }
                        } else {
                                centerFromOverlay()
                        }
                    }
                }

                // Crear un Bitmap en cache para no recrearlo por cada reporte
                val markerBitmap = Bitmap.createBitmap(70, 90, Bitmap.Config.ARGB_8888).apply {
                    val canvas = Canvas(this)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

                    // Color principal del marcador (Rojo profesional)
                    val pinColor = android.graphics.Color.parseColor("#E63946")
                    val shadowColor = android.graphics.Color.parseColor("#33000000")

                    // Sombra de la punta
                    paint.color = shadowColor
                    canvas.drawOval(25f, 75f, 45f, 85f, paint)

                    // Cuerpo del marcador (Teardrop)
                    paint.color = pinColor
                    val path = Path().apply {
                        moveTo(35f, 80f) // Punta inferior
                        cubicTo(35f, 80f, 5f, 45f, 5f, 30f)
                        cubicTo(5f, 10f, 20f, 5f, 35f, 5f)
                        cubicTo(50f, 5f, 65f, 10f, 65f, 30f)
                        cubicTo(65f, 45f, 35f, 80f, 35f, 80f)
                        close()
                    }
                    canvas.drawPath(path, paint)

                    // Circulo blanco interior para contraste
                    paint.color = android.graphics.Color.WHITE
                    canvas.drawCircle(35f, 30f, 12f, paint)
                }

                val customIcon = BitmapDrawable(mapView.context.resources, markerBitmap)

                reports.forEach { report ->
                    val marker = Marker(mapView)
                    marker.position = GeoPoint(report.latitude, report.longitude)
                    marker.icon = customIcon
                    // Ajustar el ancla para que la punta apunte correctamente a la coordenada
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    // Al hacer click, mostramos nuestro componente en Jetpack Compose
                    // y evitamos que Osmdroid muestre su burbuja gris anticuada
                    marker.setOnMarkerClickListener { _, _ ->
                        selectedReport = report
                        mapView.controller.animateTo(GeoPoint(report.latitude, report.longitude))
                        true // true significa que consumimos el evento
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

        // Tarjeta flotante moderna encima del mapa (Jetpack Compose puro)
        AnimatedVisibility(
            visible = selectedReport != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            selectedReport?.let { report ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = report.category.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { selectedReport = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        report.content?.description?.let { desc ->
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val statusColor = when (report.status.name) {
                            "PENDIENTE" -> Color(0xFFF97316)
                            "EN_REVISION" -> Color(0xFF3B82F6)
                            "RESUELTO" -> Color(0xFF22C55E)
                            "RECHAZADO" -> Color(0xFFEF4444)
                            else -> MaterialTheme.colorScheme.outline
                        }

                        Surface(
                            color = statusColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = report.status.name,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
