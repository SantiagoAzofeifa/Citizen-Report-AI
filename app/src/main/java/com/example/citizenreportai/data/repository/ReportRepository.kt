package com.example.citizenreportai.data.repository

import com.example.citizenreportai.data.model.Report
import com.example.citizenreportai.data.model.ReportCategory
import com.example.citizenreportai.data.model.ReportStatus
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

    /** Actualiza solo el estado de un reporte. Devuelve true si tuvo éxito. */
    suspend fun updateReportStatus(id: String, newStatus: ReportStatus): Boolean
}
