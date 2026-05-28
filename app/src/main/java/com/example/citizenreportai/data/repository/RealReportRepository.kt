package com.example.citizenreportai.data.repository

import com.example.citizenreportai.data.model.*
import com.example.citizenreportai.data.remote.NetworkRetry
import com.example.citizenreportai.data.remote.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Date

class RealReportRepository : ReportRepository {

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    override val reports: StateFlow<List<Report>> = _reports

    // Reports added locally but not yet confirmed by the server (use a temp id prefix)
    private val pendingLocalIds = mutableSetOf<String>()

    override suspend fun fetchReports() {
        try {
            val response = NetworkRetry.withRetry { RetrofitInstance.api.getReports() }
            // Keep locally-added reports that the server doesn't know about yet
            val pendingReports = _reports.value.filter { it.id != null && it.id in pendingLocalIds }
            _reports.value = response + pendingReports
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun fetchReportById(id: String): Report? {
        // If it's a pending local report, return it from the cache without hitting the network
        if (id in pendingLocalIds) {
            return _reports.value.find { it.id == id }
        }
        return try {
            NetworkRetry.withRetry { RetrofitInstance.api.getReportById(id) }
        } catch (e: Exception) {
            e.printStackTrace()
            _reports.value.find { it.id == id }
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
        // Optimistic local id so the report appears in MyReports immediately
        val tempId = "local_${java.util.UUID.randomUUID()}"
        val optimisticReport = Report(
            id = tempId,
            userId = userId,
            dateReported = Date(),
            status = ReportStatus.PENDIENTE,
            category = category,
            latitude = latitude,
            longitude = longitude,
            content = ReportContent(reportId = tempId, description = description, closingComment = null),
            photos = photoUrl?.let {
                listOf(Photo(id = null, reportId = tempId, photoUrl = it, createdAt = Date()))
            } ?: emptyList()
        )
        pendingLocalIds.add(tempId)
        _reports.value = _reports.value + optimisticReport

        try {
            val newReport = Report(
                id = null,
                userId = userId,
                dateReported = Date(),
                status = ReportStatus.PENDIENTE,
                category = category,
                latitude = latitude,
                longitude = longitude,
                content = ReportContent(reportId = null, description = description, closingComment = null),
                photos = photoUrl?.let {
                    listOf(Photo(id = null, reportId = null, photoUrl = it, createdAt = Date()))
                } ?: emptyList()
            )
            val createdReport = NetworkRetry.withRetry { RetrofitInstance.api.createReport(newReport) }
            // Replace the optimistic entry with the server-confirmed report
            pendingLocalIds.remove(tempId)
            _reports.value = _reports.value.map { if (it.id == tempId) createdReport else it }
        } catch (e: Exception) {
            e.printStackTrace()
            // Keep the optimistic report so it still shows in MyReports
        }
    }
}
