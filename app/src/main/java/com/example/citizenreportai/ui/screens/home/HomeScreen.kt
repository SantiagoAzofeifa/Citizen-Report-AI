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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: ReportRepository,
    onNavigateToCreateReport: () -> Unit,
    onNavigateToMyReports: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val reports by repository.reports.collectAsState(initial = emptyList())

    // Cargar reportes al iniciar
    LaunchedEffect(Unit) {
        repository.fetchReports()
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { 
                        selectedTab = 1
                        onNavigateToMyReports()
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.ListIcon, contentDescription = "Mis Reportes") },
                    label = { Text("Mis Reportes") }
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
            FloatingActionButton(
                onClick = onNavigateToCreateReport,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(50) // Circular
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo Reporte", modifier = Modifier.size(32.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .systemBarsPadding()
        ) {
            // Cabecera superior
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Mapa de Reportes",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 56.dp),
                        placeholder = { Text("Buscar ubicación...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = Color.Transparent
                        )
                    )

                    IconButton(
                        onClick = { /* Todo: Filters */ },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Filtro",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Área del Mapa y la Tarjeta de stats
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
            ) {
                ReportsMapComponent(reports = reports)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .align(Alignment.TopCenter)
                        .shadow(8.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatItem(reports.size.toString(), "Total", MaterialTheme.colorScheme.primary)
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        StatItem(reports.count { it.status.name == "PENDIENTE" }.toString(), "Pendientes", Color(0xFFF97316))
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        StatItem(reports.count { it.status.name == "RESUELTO" }.toString(), "Resueltos", Color(0xFF22C55E))
                    }
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

                // Crear un Bitmap en caché para no recrearlo por cada reporte
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

                    // Círculo blanco interior para contraste
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

                if (reports.isNotEmpty() && selectedReport == null) {
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
