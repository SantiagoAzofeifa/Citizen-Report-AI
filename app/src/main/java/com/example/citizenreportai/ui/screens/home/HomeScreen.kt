package com.example.citizenreportai.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.citizenreportai.data.model.Report
import com.example.citizenreportai.data.repository.MockReportRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: MockReportRepository,
    onNavigateToCreateReport: () -> Unit
) {
    val reports by repository.reports.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes Ciudadanos") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreateReport) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo Reporte")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(reports) { report ->
                ReportItem(report)
            }
        }
    }
}

@Composable
fun ReportItem(report: Report) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO: Navigate to detail */ }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Categoría: ${report.category}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Estado: ${report.status}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = report.content.description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
