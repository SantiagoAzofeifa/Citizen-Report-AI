package com.example.citizenreportai.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.citizenreportai.data.model.UserRole
import com.example.citizenreportai.data.repository.AuthRepository
import com.example.citizenreportai.ui.components.LoadingState
import com.example.citizenreportai.ui.components.SecondaryButton
import com.example.citizenreportai.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val user by authRepository.currentUser.collectAsState()
    val spacing = AppTheme.spacing

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mi perfil", style = MaterialTheme.typography.titleLarge) },
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
        user?.let { currentUser ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.lg)
                    .padding(bottom = spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(spacing.lg))

                // ── Avatar grande con iniciales ─────────────
                Surface(
                    modifier = Modifier.size(96.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials(currentUser.firstName, currentUser.lastName),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(Modifier.height(spacing.lg))

                Text(
                    text = "${currentUser.firstName.trim()} ${currentUser.lastName?.trim().orEmpty()}".trim(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(spacing.xs))
                RoleBadge(role = currentUser.role)

                Spacer(Modifier.height(spacing.xxl))

                // ── Card de información ─────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column {
                        InfoRow(
                            icon = Icons.Outlined.Email,
                            label = "Correo electrónico",
                            value = currentUser.email
                        )
                        RowDivider()
                        InfoRow(
                            icon = Icons.Outlined.Phone,
                            label = "Teléfono",
                            value = currentUser.phone
                        )
                        RowDivider()
                        InfoRow(
                            icon = Icons.Outlined.Badge,
                            label = "Identificador",
                            value = currentUser.identifier
                        )
                    }
                }

                Spacer(Modifier.height(spacing.xxl))

                // ── Botón cerrar sesión (secundario, no destructivo prominente) ──
                SecondaryButton(
                    text = "Cerrar sesión",
                    onClick = {
                        authRepository.logout()
                        onLogout()
                    },
                    leadingIcon = Icons.AutoMirrored.Outlined.Logout,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(spacing.lg))
                Text(
                    text = "Citizen Report · v1.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } ?: LoadingState(message = "Cargando perfil…")
    }
}

private fun initials(firstName: String, lastName: String?): String {
    val f = firstName.trim().firstOrNull()?.uppercase().orEmpty()
    val l = lastName?.trim()?.firstOrNull()?.uppercase().orEmpty()
    return (f + l).ifEmpty { "?" }
}

@Composable
private fun RoleBadge(role: UserRole) {
    val (label, icon) = when (role) {
        UserRole.ADMIN       -> "Administrador" to Icons.Outlined.Shield
        UserRole.FUNCIONARIO -> "Funcionario"   to Icons.Outlined.Shield
        UserRole.USER        -> "Ciudadano"     to Icons.Outlined.Person
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    val spacing = AppTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline,
        thickness = 1.dp,
        modifier = Modifier.padding(start = 64.dp)
    )
}
