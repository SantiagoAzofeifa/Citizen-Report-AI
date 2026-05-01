package com.example.citizenreportai.data.repository

import com.example.citizenreportai.data.model.*
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Implementación de prueba del repositorio de reportes.
 */
class MockReportRepository : ReportRepository {

    private val _reports = MutableStateFlow<List<Report>>(listOf(
        Report(
            id = "1",
            userId = "1",
            dateReported = Date(),
            status = ReportStatus.PENDIENTE,
            category = ReportCategory.BACHE,
            latitude = -16.5,
            longitude = -68.1,
            content = ReportContent(reportId = "1", description = "Enorme bache en la principal", closingComment = null),
            photos = listOf(Photo(id = "1", reportId = "1", photoUrl = "https://example.com/foto1.jpg", createdAt = Date()))
        ),
        Report(
            id = "2",
            userId = "2",
            dateReported = Date(),
            status = ReportStatus.EN_REVISION,
            category = ReportCategory.BASURA,
            latitude = -16.51,
            longitude = -68.12,
            content = ReportContent(reportId = "2", description = "Basura acumulada en la esquina", closingComment = null),
            photos = listOf()
        )
    ))

    override val reports: StateFlow<List<Report>> = _reports

    override suspend fun fetchReports() {
        // No-op para mock, ya está en memoria
    }

    override suspend fun addReport(
        userId: String,
        category: ReportCategory,
        description: String,
        latitude: Double,
        longitude: Double,
        photoUrl: String?
    ) {
        val maxId = _reports.value.mapNotNull { it.id.toIntOrNull() }.maxOfOrNull { it } ?: 0
        val newId = (maxId + 1).toString()
        
        val newReport = Report(
            id = newId,
            userId = userId,
            dateReported = Date(),
            status = ReportStatus.PENDIENTE,
            category = category,
            latitude = latitude,
            longitude = longitude,
            content = ReportContent(reportId = newId, description = description, closingComment = null),
            photos = photoUrl?.let { listOf(Photo(id = newId, reportId = newId, photoUrl = it, createdAt = Date())) } ?: emptyList()
        )
        _reports.value = _reports.value + newReport
    }
}
