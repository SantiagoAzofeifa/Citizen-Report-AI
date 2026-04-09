package com.example.citizenreportai.data.repository

import com.example.citizenreportai.data.model.Notification
import kotlinx.coroutines.flow.StateFlow

interface NotificationRepository {
    val notifications: StateFlow<List<Notification>>
    suspend fun fetchNotifications()
}
