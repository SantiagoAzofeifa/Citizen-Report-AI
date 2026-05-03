package com.example.citizenreportai.data.model

import com.google.gson.annotations.SerializedName
import java.util.Date

enum class UserRole { ADMIN, USER, FUNCIONARIO }
enum class ReportStatus { PENDIENTE, EN_REVISION, PROGRAMADO, RESUELTO, RECHAZADO }
enum class ReportCategory { BACHE, ALUMBRADO, BASURA, SEGURIDAD, PARQUES, OTROS }
enum class NotificationType { ALERTA, ACTUALIZACION, INFO }

data class User(
    @SerializedName("id") val id: String,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String?,
    @SerializedName("phone") val phone: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: UserRole,
    @SerializedName("identifier") val identifier: String,
    @SerializedName("createdAt") val createdAt: Date,
    @SerializedName("updatedAt") val updatedAt: Date
)

data class Report(
    @SerializedName("id") val id: String? = null,
    @SerializedName("userId") val userId: String?,
    @SerializedName("dateReported") val dateReported: Date,
    @SerializedName("status") val status: ReportStatus,
    @SerializedName("category") val category: ReportCategory,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("content") val content: ReportContent?,
    @SerializedName("photos") val photos: List<Photo>
)

data class ReportContent(
    @SerializedName("reportId") val reportId: String? = null,
    @SerializedName("description") val description: String,
    @SerializedName("closingComment") val closingComment: String? = null
)

data class Photo(
    @SerializedName("id") val id: String? = null,
    @SerializedName("reportId") val reportId: String? = null,
    @SerializedName("photoUrl") val photoUrl: String,
    @SerializedName("createdAt") val createdAt: Date
)

data class Notification(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("info") val info: String,
    @SerializedName("category") val category: NotificationType,
    @SerializedName("isRead") val isRead: Boolean,
    @SerializedName("dateSent") val dateSent: Date
)
