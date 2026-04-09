package com.example.citizenreportai.data.repository

import com.example.citizenreportai.data.model.*
import com.example.citizenreportai.data.remote.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Date

class RealReportRepository : ReportRepository {

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    override val reports: StateFlow<List<Report>> = _reports

    override suspend fun fetchReports() {
        try {
            val response = RetrofitInstance.api.getReports()
            _reports.value = response
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun addReport(
        userId: String,
        category: ReportCategory,
        description: String,
        latitude: Double,
        longitude: Double,
        photoUrl: String?
    ) {
        try {
            val newReport = Report(
                id = "0",
                userId = userId,
                dateReported = Date(),
                status = ReportStatus.PENDIENTE,
                category = category,
                latitude = latitude,
                longitude = longitude,
                content = ReportContent(reportId = null, description = description, closingComment = null),
                photos = photoUrl?.let { listOf(Photo(id = "0", reportId = "0", photoUrl = it, createdAt = Date())) } ?: emptyList()
            )
            val createdReport = RetrofitInstance.api.createReport(newReport)
            _reports.value = _reports.value + createdReport
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
