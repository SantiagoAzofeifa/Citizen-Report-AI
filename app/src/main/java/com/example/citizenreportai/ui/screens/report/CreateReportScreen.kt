package com.example.citizenreportai.ui.screens.report

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.citizenreportai.data.model.ReportCategory
import com.example.citizenreportai.data.remote.ModerationResult
import com.example.citizenreportai.data.remote.PhotoUploader
import com.example.citizenreportai.data.remote.ReportModerator
import com.example.citizenreportai.data.repository.ReportRepository
import com.example.citizenreportai.ui.components.AppTextField
import com.example.citizenreportai.ui.components.CategoryChip
import com.example.citizenreportai.ui.components.LocationPreviewMap
import com.example.citizenreportai.ui.components.OpenStreetMapComponent
import com.example.citizenreportai.ui.components.PrimaryButton
import com.example.citizenreportai.ui.components.SecondaryButton
import com.example.citizenreportai.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun createImageFile(context: android.content.Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir("Pictures")
    return File.createTempFile("REPORTE_${timeStamp}_", ".jpg", storageDir)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportScreen(
    repository: ReportRepository,
    userId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val spacing = AppTheme.spacing

    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ReportCategory.BACHE) }
    var reportLatitude by remember { mutableStateOf<Double?>(null) }
    var reportLongitude by remember { mutableStateOf<Double?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var validating by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    // Análisis de la foto con IA (se dispara automáticamente al tomar la foto).
    var photoAnalysis by remember { mutableStateOf<ModerationResult?>(null) }
    var analyzingPhoto by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Analiza la foto con la IA: describe lo que ve y decide si es válida.
    fun analyzePhoto(uri: Uri) {
        scope.launch {
            analyzingPhoto = true
            photoAnalysis = null
            uploadError = null
            val result = ReportModerator.moderate(
                context = context,
                photoUri = uri,
                description = description,
                category = selectedCategory
            )
            // Si la IA sugiere una categoría más adecuada, la aplicamos.
            if (result is ModerationResult.Approved) {
                result.suggestedCategory?.let { selectedCategory = it }
            }
            photoAnalysis = result
            analyzingPhoto = false
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val taken = pendingPhotoUri
            photoUri = taken
            taken?.let { analyzePhoto(it) }
        }
        pendingPhotoUri = null
    }

    fun launchCamera() {
        val imageFile = createImageFile(context)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        pendingPhotoUri = uri
        takePictureLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) launchCamera()
    }

    fun handleTakePhoto() {
        if (!hasCameraPermission) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        else launchCamera()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Nuevo reporte",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Atrás",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.lg, vertical = spacing.md)
                ) {
                    if (uploadError != null) {
                        Text(
                            text = uploadError!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = spacing.sm)
                        )
                    }
                    PrimaryButton(
                        text = when {
                            analyzingPhoto -> "Analizando foto…"
                            validating -> "Validando con IA…"
                            submitting && photoUri != null -> "Subiendo foto…"
                            else -> "Enviar reporte"
                        },
                        onClick = {
                            scope.launch {
                                uploadError = null

                                // 1) Validación con IA antes de enviar.
                                // La foto ya se analizó al tomarla: si fue rechazada, no se envía.
                                val analysis = photoAnalysis
                                if (photoUri != null && analysis is ModerationResult.Rejected) {
                                    uploadError = "Foto rechazada: ${analysis.reason}"
                                    return@launch
                                }
                                // Sin foto: validamos la descripción con la IA en este momento.
                                if (photoUri == null) {
                                    validating = true
                                    val moderation = ReportModerator.moderate(
                                        context = context,
                                        photoUri = null,
                                        description = description,
                                        category = selectedCategory
                                    )
                                    validating = false
                                    if (moderation is ModerationResult.Rejected) {
                                        uploadError = "Reporte rechazado: ${moderation.reason}"
                                        return@launch
                                    }
                                    if (moderation is ModerationResult.Approved) {
                                        moderation.suggestedCategory?.let { selectedCategory = it }
                                    }
                                }

                                // 2) Subida de foto y creación del reporte.
                                submitting = true
                                val uploadedUrl = photoUri?.let { local ->
                                    PhotoUploader.upload(context, local)
                                }
                                if (photoUri != null && uploadedUrl == null) {
                                    submitting = false
                                    uploadError = "No se pudo subir la foto. Revisa tu conexión o vuelve a intentar."
                                    return@launch
                                }
                                repository.addReport(
                                    userId = userId,
                                    category = selectedCategory,
                                    description = description,
                                    latitude = reportLatitude ?: 0.0,
                                    longitude = reportLongitude ?: 0.0,
                                    photoUrl = uploadedUrl
                                )
                                onNavigateBack()
                            }
                        },
                        loading = submitting || validating || analyzingPhoto,
                        enabled = reportLatitude != null &&
                            !analyzingPhoto &&
                            photoAnalysis !is ModerationResult.Rejected,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.lg)
                .padding(bottom = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.xl)
        ) {
            // ── Categoría ────────────────────────────────
            SectionBlock(
                title = "¿Qué quieres reportar?",
                subtitle = "Selecciona la categoría que mejor describe el problema."
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    ReportCategory.entries.forEach { category ->
                        CategoryChip(
                            category = category,
                            selected = category == selectedCategory,
                            onClick = { selectedCategory = category }
                        )
                    }
                }
            }

            // ── Descripción ──────────────────────────────
            SectionBlock(
                title = "Descripción (opcional)",
                subtitle = "Describe el problema con detalle. Cuanta más información, mejor."
            ) {
                AppTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "",
                    placeholder = "Ej: Hay un bache grande en el carril derecho que está causando daños a los vehículos…",
                    singleLine = false,
                    modifier = Modifier.heightIn(min = 120.dp)
                )
            }

            // ── Ubicación ────────────────────────────────
            SectionBlock(
                title = "Ubicación",
                subtitle = "Pulsa para abrir el mapa y fijar el punto exacto."
            ) {
                if (reportLatitude == null || reportLongitude == null) {
                    LocationPlaceholder(onClick = { showLocationPicker = true })
                } else {
                    SelectedLocationCard(
                        lat = reportLatitude!!,
                        lon = reportLongitude!!,
                        onChange = { showLocationPicker = true }
                    )
                }
            }

            // ── Foto ─────────────────────────────────────
            SectionBlock(
                title = "Foto (opcional)",
                subtitle = "Una imagen ayuda al equipo a entender el problema."
            ) {
                if (photoUri == null) {
                    PhotoPlaceholder(onTakePhoto = ::handleTakePhoto)
                } else {
                    PhotoPreview(uri = photoUri!!, onRetake = ::handleTakePhoto)
                    PhotoAnalysisCard(analyzing = analyzingPhoto, result = photoAnalysis)
                }
            }
        }
    }

    if (showLocationPicker) {
        LocationPickerDialog(
            initialLat = reportLatitude,
            initialLon = reportLongitude,
            onDismiss = { showLocationPicker = false },
            onConfirm = { lat, lon ->
                reportLatitude = lat
                reportLongitude = lon
                showLocationPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationPickerDialog(
    initialLat: Double?,
    initialLon: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit
) {
    val spacing = AppTheme.spacing
    var currentLat by remember { mutableStateOf(initialLat) }
    var currentLon by remember { mutableStateOf(initialLon) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Selecciona la ubicación", style = MaterialTheme.typography.titleLarge) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Outlined.Close, contentDescription = "Cerrar")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = spacing.lg, vertical = spacing.md)
                        ) {
                            PrimaryButton(
                                text = "Confirmar ubicación",
                                onClick = {
                                    val lat = currentLat
                                    val lon = currentLon
                                    if (lat != null && lon != null) onConfirm(lat, lon)
                                },
                                enabled = currentLat != null && currentLon != null,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    OpenStreetMapComponent(
                        modifier = Modifier.fillMaxSize(),
                        onLocationDetermined = { lat, lon ->
                            currentLat = lat
                            currentLon = lon
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationPlaceholder(onClick: () -> Unit) {
    val spacing = AppTheme.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(spacing.sm))
            Text(
                text = "Seleccionar ubicación",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Pulsa para abrir el mapa",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SelectedLocationCard(lat: Double, lon: Double, onChange: () -> Unit) {
    val spacing = AppTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
        ) {
            LocationPreviewMap(
                latitude = lat,
                longitude = lon,
                modifier = Modifier.fillMaxSize()
            )
        }
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Ubicación fijada · %.5f, %.5f".format(lat, lon),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        SecondaryButton(
            text = "Cambiar ubicación",
            onClick = onChange,
            leadingIcon = Icons.Outlined.Edit,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SectionBlock(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val spacing = AppTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        content()
    }
}

@Composable
private fun PhotoPlaceholder(onTakePhoto: () -> Unit) {
    val spacing = AppTheme.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onTakePhoto
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(spacing.sm))
            Text(
                text = "Tomar foto",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Pulsa para abrir la cámara",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PhotoPreview(uri: Uri, onRetake: () -> Unit) {
    val spacing = AppTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "Foto del reporte",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(spacing.sm),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Foto añadida",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        SecondaryButton(
            text = "Volver a tomar",
            onClick = onRetake,
            leadingIcon = Icons.Outlined.Refresh,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Muestra el resultado del análisis de la foto por la IA: lo que ve en la imagen y
 * si es válida o no. Mientras analiza muestra un indicador de progreso.
 */
@Composable
private fun PhotoAnalysisCard(analyzing: Boolean, result: ModerationResult?) {
    val spacing = AppTheme.spacing

    // Estado de carga mientras la IA analiza la foto recién tomada.
    if (analyzing) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Analizando la foto con IA…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    // Resolvemos colores, icono, título y textos según el resultado.
    data class CardStyle(
        val container: androidx.compose.ui.graphics.Color,
        val onContainer: androidx.compose.ui.graphics.Color,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val title: String,
        val seen: String?,
        val note: String?
    )

    val style = when (result) {
        is ModerationResult.Approved -> CardStyle(
            container = MaterialTheme.colorScheme.secondaryContainer,
            onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
            icon = Icons.Outlined.CheckCircle,
            title = "Foto válida",
            seen = result.imageDescription,
            note = null
        )
        is ModerationResult.Rejected -> CardStyle(
            container = MaterialTheme.colorScheme.errorContainer,
            onContainer = MaterialTheme.colorScheme.onErrorContainer,
            icon = Icons.Outlined.Warning,
            title = "Foto no válida",
            seen = result.imageDescription,
            note = result.reason
        )
        is ModerationResult.Skipped -> CardStyle(
            container = MaterialTheme.colorScheme.surfaceVariant,
            onContainer = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = Icons.Outlined.Info,
            title = "No se pudo analizar la foto",
            seen = null,
            note = "${result.reason}. Puedes enviar el reporte de todos modos."
        )
        null -> return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = style.container
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Icon(
                style.icon,
                contentDescription = null,
                tint = style.onContainer,
                modifier = Modifier.size(18.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = style.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = style.onContainer
                )
                if (style.seen != null) {
                    Text(
                        text = "La IA ve: ${style.seen}",
                        style = MaterialTheme.typography.bodySmall,
                        color = style.onContainer
                    )
                }
                if (style.note != null) {
                    Text(
                        text = style.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = style.onContainer
                    )
                }
            }
        }
    }
}
