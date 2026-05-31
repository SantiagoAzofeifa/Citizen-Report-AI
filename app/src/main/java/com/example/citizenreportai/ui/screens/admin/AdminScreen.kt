package com.example.citizenreportai.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.citizenreportai.data.model.User
import com.example.citizenreportai.data.model.UserRole
import com.example.citizenreportai.data.repository.AuthRepository
import com.example.citizenreportai.data.repository.CreateUserResult
import com.example.citizenreportai.data.repository.DeleteUserResult
import com.example.citizenreportai.ui.components.AppTextField
import com.example.citizenreportai.ui.components.EmptyState
import com.example.citizenreportai.ui.components.LoadingState
import com.example.citizenreportai.ui.components.PrimaryButton
import com.example.citizenreportai.ui.theme.AppTheme
import kotlinx.coroutines.launch

private const val FUNCIONARIO_ROLE_ID = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    authRepository: AuthRepository,
    onNavigateToProfile: () -> Unit
) {
    val spacing = AppTheme.spacing
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser by authRepository.currentUser.collectAsState()

    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<User?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    suspend fun reload() {
        isLoading = true
        users = authRepository.getUsers()
        isLoading = false
    }

    LaunchedEffect(Unit) {
        authRepository.warmUp() // despierta el backend (Render) para que crear funcionarios no expire
        reload()
    }

    // Categoría 0 = Funcionarios (staff), 1 = Usuarios normales (ciudadanos)
    val funcionarios = users.filter { it.role != UserRole.USER }
    val ciudadanos = users.filter { it.role == UserRole.USER }
    val visibleUsers = if (selectedTab == 0) funcionarios else ciudadanos

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Gestión de usuarios", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Outlined.Person, contentDescription = "Perfil")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
                text = { Text("Crear funcionario", style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = spacing.lg)
        ) {
            CategoryTabs(
                selected = selectedTab,
                funcionariosCount = funcionarios.size,
                ciudadanosCount = ciudadanos.size,
                onSelect = { selectedTab = it }
            )

            Spacer(Modifier.height(spacing.md))

            when {
                isLoading -> LoadingState(message = "Cargando usuarios…")
                visibleUsers.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Group,
                    title = if (selectedTab == 0) "Sin funcionarios" else "Sin usuarios",
                    description = if (selectedTab == 0)
                        "Aún no hay funcionarios. Crea uno con el botón inferior."
                    else
                        "Todavía no hay ciudadanos registrados."
                )
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(visibleUsers, key = { it.id }) { user ->
                        UserRow(
                            user = user,
                            canDelete = user.id != currentUser?.id,
                            onDelete = { userToDelete = user }
                        )
                    }
                }
            }
        }
    }

    userToDelete?.let { target ->
        val targetName = listOfNotNull(
            target.firstName.trim().takeIf { it.isNotEmpty() },
            target.lastName?.trim()?.takeIf { it.isNotEmpty() }
        ).joinToString(" ").ifBlank { target.email }

        AlertDialog(
            onDismissRequest = { if (!isDeleting) userToDelete = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isDeleting = true
                            val result = authRepository.deleteUser(target.id)
                            isDeleting = false
                            userToDelete = null
                            when (result) {
                                DeleteUserResult.Success -> {
                                    reload()
                                    snackbarHostState.showSnackbar("Usuario eliminado")
                                }
                                DeleteUserResult.NetworkError ->
                                    snackbarHostState.showSnackbar("No se pudo eliminar. Intenta de nuevo.")
                            }
                        }
                    },
                    enabled = !isDeleting
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }, enabled = !isDeleting) {
                    Text("Cancelar")
                }
            },
            title = { Text("Eliminar usuario") },
            text = {
                Text("¿Seguro que quieres eliminar a $targetName? Esta acción no se puede deshacer.")
            }
        )
    }

    if (showCreateDialog) {
        CreateFuncionarioDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { firstName, lastName, phone, email, identifier, onResult ->
                scope.launch {
                    val result = authRepository.createUser(
                        firstName = firstName,
                        lastName = lastName,
                        phone = phone,
                        email = email,
                        identifier = identifier,
                        rolId = FUNCIONARIO_ROLE_ID
                    )
                    onResult(result)
                    if (result is CreateUserResult.Success) {
                        showCreateDialog = false
                        selectedTab = 0
                        reload()
                        snackbarHostState.showSnackbar("Funcionario creado correctamente")
                    }
                }
            }
        )
    }
}

@Composable
private fun CategoryTabs(
    selected: Int,
    funcionariosCount: Int,
    ciudadanosCount: Int,
    onSelect: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            TabItem("Funcionarios ($funcionariosCount)", selected == 0, Modifier.weight(1f)) { onSelect(0) }
            TabItem("Usuarios ($ciudadanosCount)", selected == 1, Modifier.weight(1f)) { onSelect(1) }
        }
    }
}

@Composable
private fun TabItem(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
private fun UserRow(
    user: User,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    val spacing = AppTheme.spacing
    val fullName = listOfNotNull(
        user.firstName.trim().takeIf { it.isNotEmpty() },
        user.lastName?.trim()?.takeIf { it.isNotEmpty() }
    ).joinToString(" ").ifBlank { "Usuario #${user.id}" }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fullName.firstOrNull()?.uppercase() ?: "U",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RolePill(role = user.role)
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Eliminar usuario",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun RolePill(role: UserRole) {
    val label = when (role) {
        UserRole.ADMIN -> "Admin"
        UserRole.FUNCIONARIO -> "Funcionario"
        UserRole.USER -> "Ciudadano"
    }
    val icon = if (role == UserRole.USER) Icons.Outlined.Person else Icons.Outlined.Shield
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CreateFuncionarioDialog(
    onDismiss: () -> Unit,
    onCreate: (
        firstName: String,
        lastName: String?,
        phone: String,
        email: String,
        identifier: String,
        onResult: (CreateUserResult) -> Unit
    ) -> Unit
) {
    val spacing = AppTheme.spacing

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var identifier by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val canSubmit = firstName.isNotBlank() && phone.isNotBlank() &&
        email.isNotBlank() && identifier.isNotBlank() && !isLoading

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        confirmButton = {
            PrimaryButton(
                text = "Crear funcionario",
                onClick = {
                    isLoading = true
                    errorMessage = null
                    onCreate(firstName, lastName.takeIf { it.isNotBlank() }, phone, email, identifier) { result ->
                        isLoading = false
                        errorMessage = when (result) {
                            CreateUserResult.Success -> null
                            CreateUserResult.AlreadyExists -> "Ya existe un usuario con ese email o identificador. Ambos deben ser únicos."
                            CreateUserResult.InvalidData -> "Datos inválidos. Revisa los campos."
                            CreateUserResult.NetworkError -> "No se pudo crear. Intenta nuevamente."
                        }
                    }
                },
                enabled = canSubmit,
                loading = isLoading
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancelar") }
        },
        title = { Text("Nuevo funcionario", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                AppTextField(
                    value = firstName,
                    onValueChange = { firstName = it; errorMessage = null },
                    label = "Nombre",
                    placeholder = "Juan",
                    leadingIcon = Icons.Outlined.Person
                )
                AppTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = "Apellido (opcional)",
                    placeholder = "Pérez",
                    leadingIcon = Icons.Outlined.Person
                )
                AppTextField(
                    value = phone,
                    onValueChange = { phone = it; errorMessage = null },
                    label = "Teléfono",
                    placeholder = "3001234567",
                    leadingIcon = Icons.Outlined.Phone,
                    keyboardType = KeyboardType.Phone
                )
                AppTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = "Correo electrónico",
                    placeholder = "funcionario@email.com",
                    leadingIcon = Icons.Outlined.Email,
                    keyboardType = KeyboardType.Email
                )
                AppTextField(
                    value = identifier,
                    onValueChange = { identifier = it; errorMessage = null },
                    label = "Identificador",
                    placeholder = "Documento o usuario",
                    leadingIcon = Icons.Outlined.Badge
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}
