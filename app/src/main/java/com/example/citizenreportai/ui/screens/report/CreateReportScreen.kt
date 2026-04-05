package com.example.citizenreportai.ui.screens.report

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.citizenreportai.data.model.ReportCategory
import com.example.citizenreportai.data.repository.MockReportRepository
import com.example.citizenreportai.ui.components.OpenStreetMapComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportScreen(
    repository: MockReportRepository,
    onNavigateBack: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ReportCategory.BACHE) }
    var expanded by remember { mutableStateOf(false) }

    // variables para ubicación
    var reportLatitude by remember { mutableStateOf<Double?>(null) }
    var reportLongitude by remember { mutableStateOf<Double?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Reporte") },
                navigationIcon = {
                    Button(onClick = onNavigateBack) {
                        Text("Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción del problema") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Category Dropdown
            Box {
                Button(onClick = { expanded = true }) {
                    Text("Categoría: $selectedCategory")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ReportCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                selectedCategory = category
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Mapa de Ubicación con OSM
            Text(
                text = "Ubicación del incidente:",
                style = MaterialTheme.typography.labelLarge
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Para que ocupe el espacio disponible
            ) {
                OpenStreetMapComponent(
                    onLocationDetermined = { lat, lon ->
                        reportLatitude = lat
                        reportLongitude = lon
                    }
                )
            }

            Button(
                onClick = { /* TODO: Use actual location and camera */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Adjuntar Fotografía")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    repository.addReport(
                        category = selectedCategory,
                        description = description,
                        latitude = reportLatitude ?: 0.0,
                        longitude = reportLongitude ?: 0.0,
                        photoUrl = null
                    )
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = description.isNotBlank() && reportLatitude != null
            ) {
                Text("Enviar Reporte")
            }
        }
    }
}
