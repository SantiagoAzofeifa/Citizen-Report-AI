package com.example.citizenreportai.data.model

import java.util.Date

enum class UserRole {
    ADMIN, USER, FUNCIONARIO
}

enum class ReportStatus {
    PENDIENTE, EN_REVISION, PROGRAMADO, RESUELTO, RECHAZADO
}

enum class ReportCategory {
    BACHE, ALUMBRADO, BASURA, SEGURIDAD, PARQUES, OTROS
}

enum class NotificationType {
    ALERTA, ACTUALIZACION, INFO
}

data class User(
    val id: Int,
    val firstName: String,
    val lastName: String?,
    val phone: String,
    val email: String,
    val role: UserRole,
    val identifier: String, // CI or other ID
    val createdAt: Date,
    val updatedAt: Date
)

data class Report(
    val id: Int,
    val userId: Int?, // Can be null if user was deleted
    val dateReported: Date,
    val status: ReportStatus,
    val category: ReportCategory,
    val latitude: Double,
    val longitude: Double,
    val content: ReportContent,
    val photos: List<Photo>
)

data class ReportContent(
    val reportId: Int,
    val description: String,
    val closingComment: String?
)

data class Photo(
    val id: Int,
    val reportId: Int,
    val photoUrl: String,
    val createdAt: Date
)

data class Notification(
    val id: Int,
    val userId: Int,
    val info: String,
    val category: NotificationType,
    val isRead: Boolean,
    val dateSent: Date
)

