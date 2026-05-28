package com.example.citizenreportai.ui.screens.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.citizenreportai.data.model.Notification
import com.example.citizenreportai.data.model.NotificationType
import com.example.citizenreportai.data.repository.NotificationRepository
import com.example.citizenreportai.ui.components.EmptyState
import com.example.citizenreportai.ui.theme.AppTheme
import com.example.citizenreportai.ui.theme.Info500
import com.example.citizenreportai.ui.theme.InfoSoft
import com.example.citizenreportai.ui.theme.Warning500
import com.example.citizenreportai.ui.theme.WarningSoft
import com.example.citizenreportai.ui.theme.Success500
import com.example.citizenreportai.ui.theme.SuccessSoft
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    repository: NotificationRepository,
    onNavigateBack: () -> Unit
) {
    val notifications by repository.notifications.collectAsState()
    val spacing = AppTheme.spacing

    LaunchedEffect(Unit) { repository.fetchNotifications() }

    val unreadCount = notifications.count { !it.isRead }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Notificaciones", style = MaterialTheme.typography.titleLarge)
                        if (unreadCount > 0) {
                            Text(
                                text = "$unreadCount sin leer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (notifications.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.NotificationsNone,
                    title = "Sin notificaciones",
                    description = "Aquí verás actualizaciones sobre tus reportes y alertas de tu ciudad.",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = spacing.lg,
                        end = spacing.lg,
                        top = spacing.sm,
                        bottom = spacing.xl
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    items(notifications) { notification ->
                        NotificationItem(notification = notification)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(notification: Notification) {
    val spacing = AppTheme.spacing
    val dateFormat = remember { SimpleDateFormat("d MMM · HH:mm", Locale("es")) }
    val (icon, accentColor, accentSoft) = iconAndColor(notification.category)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (notification.isRead) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (notification.isRead) MaterialTheme.colorScheme.outline
            else accentColor.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accentSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = categoryLabel(notification.category),
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor
                    )
                    Text(
                        text = dateFormat.format(notification.dateSent),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = notification.info,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(8.dp)
                        .background(accentColor, CircleShape)
                )
            }
        }
    }
}

private fun iconAndColor(type: NotificationType): Triple<ImageVector, Color, Color> = when (type) {
    NotificationType.ALERTA        -> Triple(Icons.Outlined.WarningAmber, Warning500, WarningSoft)
    NotificationType.ACTUALIZACION -> Triple(Icons.Outlined.Update,       Success500, SuccessSoft)
    NotificationType.INFO          -> Triple(Icons.Outlined.Info,         Info500,    InfoSoft)
}

private fun categoryLabel(type: NotificationType) = when (type) {
    NotificationType.ALERTA        -> "Alerta"
    NotificationType.ACTUALIZACION -> "Actualización"
    NotificationType.INFO          -> "Información"
}
