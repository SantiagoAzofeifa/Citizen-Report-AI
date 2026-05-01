package com.example.citizenreportai.data.model

import com.google.gson.annotations.SerializedName
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
    val id: String,
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
    val id: String = "0",
    val userId: String?, // Gson convertirá el 1 (Int) en "1" (String) automáticamente
    val dateReported: Date,
    val status: ReportStatus,
    val category: ReportCategory,
    val latitude: Double,
    val longitude: Double,
    val content: ReportContent,
    val photos: List<Photo>
)

data class ReportContent(
    @SerializedName("reportId")
    val reportId: String?,
    val description: String,
    val closingComment: String?
)

data class Photo(
    val id: String,
    @SerializedName("reportId")
    val reportId: String,
    val photoUrl: String,
    val createdAt: Date
)

data class Notification(
    val id: String,
    val userId: String,
    val info: String,
    val category: NotificationType,
    val isRead: Boolean,
    val dateSent: Date
)
