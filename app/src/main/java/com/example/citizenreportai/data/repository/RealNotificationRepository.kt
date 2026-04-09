package com.example.citizenreportai.data.repository

import com.example.citizenreportai.data.model.Notification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RealNotificationRepository : NotificationRepository {
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    override val notifications: StateFlow<List<Notification>> = _notifications

    override suspend fun fetchNotifications() {
        // Deshabilitado temporalmente por falta de recursos en MockAPI
        _notifications.value = emptyList()
    }
}
