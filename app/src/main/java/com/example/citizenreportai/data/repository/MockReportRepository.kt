package com.example.citizenreportai.data.repository

import com.example.citizenreportai.data.model.*
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MockReportRepository {

    private val currentUser = User(
        id = 1,
        firstName = "Juan",
        lastName = "Perez",
        phone = "12345678",
        email = "juan@example.com",
        role = UserRole.USER,
        identifier = "12345678",
        createdAt = Date(),
        updatedAt = Date()
    )

    private val _reports = MutableStateFlow<List<Report>>(listOf(
        Report(
            id = 1,
            userId = 1,
            dateReported = Date(),
            status = ReportStatus.PENDIENTE,
            category = ReportCategory.BACHE,
            latitude = -16.5,
            longitude = -68.1,
            content = ReportContent(1, "Enorme bache en la principal", null),
            photos = listOf(Photo(1, 1, "https://example.com/foto1.jpg", Date()))
        ),
        Report(
            id = 2,
            userId = 1,
            dateReported = Date(),
            status = ReportStatus.EN_REVISION,
            category = ReportCategory.BASURA,
            latitude = -16.51,
            longitude = -68.12,
            content = ReportContent(2, "Basura acumulada en la esquina", null),
            photos = listOf()
        )
    ))

    val reports: StateFlow<List<Report>> = _reports

    fun getReports(): List<Report> {
        return _reports.value
    }

    fun addReport(
        category: ReportCategory,
        description: String,
        latitude: Double,
        longitude: Double,
        photoUrl: String?
    ) {
        val newId = (_reports.value.maxOfOrNull { it.id } ?: 0) + 1
        val newReport = Report(
            id = newId,
            userId = currentUser.id,
            dateReported = Date(),
            status = ReportStatus.PENDIENTE,
            category = category,
            latitude = latitude,
            longitude = longitude,
            content = ReportContent(newId, description, null),
            photos = photoUrl?.let { listOf(Photo(newId, newId, it, Date())) } ?: emptyList()
        )
        _reports.value = _reports.value + newReport
    }

    fun getCurrentUser() = currentUser
}

