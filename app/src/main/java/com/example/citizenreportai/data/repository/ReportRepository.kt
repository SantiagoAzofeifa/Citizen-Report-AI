package com.example.citizenreportai.data.repository

import com.example.citizenreportai.data.model.Report
import com.example.citizenreportai.data.model.ReportCategory
import kotlinx.coroutines.flow.StateFlow

interface ReportRepository {
    val reports: StateFlow<List<Report>>
    suspend fun fetchReports()
    suspend fun fetchReportById(id: String): Report?
    suspend fun addReport(
        userId: String,
        category: ReportCategory,
        description: String,
        latitude: Double,
        longitude: Double,
        photoUrl: String?
    )
}
