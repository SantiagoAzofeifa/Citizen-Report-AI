package com.example.citizenreportai.ui.screens.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.citizenreportai.data.repository.AuthRepository
import com.example.citizenreportai.data.repository.CreateUserResult
import com.example.citizenreportai.data.repository.LoginResult
import com.example.citizenreportai.ui.components.AppTextField
import com.example.citizenreportai.ui.components.PrimaryButton
import com.example.citizenreportai.ui.theme.AppTheme
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onLoginSuccess: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    var loginEmail by remember { mutableStateOf("") }
    var loginIdentifier by remember { mutableStateOf("") }
    var loginIsLoading by remember { mutableStateOf(false) }
    var loginErrorMessage by remember { mutableStateOf<String?>(null) }
    var loginInfoMessage by remember { mutableStateOf<String?>(null) }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var registerEmail by remember { mutableStateOf("") }
    var registerIdentifier by remember { mutableStateOf("") }
    var registerIsLoading by remember { mutableStateOf(false) }
    var registerErrorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val spacing = AppTheme.spacing

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.xl)
                .padding(top = spacing.xxxl, bottom = spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandHeader()

            Spacer(Modifier.height(spacing.xxl))

            SegmentedTabs(
                selected = selectedTab,
                onSelect = {
                    selectedTab = it
                    loginErrorMessage = null
                    registerErrorMessage = null
                }
            )

            Spacer(Modifier.height(spacing.xl))

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(tween(220)) togetherWith fadeOut(tween(180)))
                },
                label = "authTabContent"
            ) { tab ->
                if (tab == 0) {
                    LoginForm(
                        email = loginEmail,
                        onEmailChange = { loginEmail = it; loginErrorMessage = null },
                        identifier = loginIdentifier,
                        onIdentifierChange = { loginIdentifier = it; loginErrorMessage = null },
                        isLoading = loginIsLoading,
                        errorMessage = loginErrorMessage,
                        infoMessage = loginInfoMessage,
                        onSubmit = {
                            scope.launch {
                                loginIsLoading = true
                                loginErrorMessage = null
                                loginInfoMessage = null
                                val result = authRepository.login(loginEmail, loginIdentifier)
                                loginIsLoading = false
                                when (result) {
                                    LoginResult.Success -> onLoginSuccess()
                                    LoginResult.InvalidCredentials ->
                                        loginErrorMessage = "Credenciales incorrectas. Verifica e intenta de nuevo."
                                    LoginResult.NetworkError ->
                                        loginErrorMessage = "No se pudo conectar con el servidor. Intenta en unos segundos."
                                }
                            }
                        }
                    )
                } else {
                    CreateUserForm(
                        firstName = firstName,
                        onFirstNameChange = { firstName = it; registerErrorMessage = null },
                        lastName = lastName,
                        onLastNameChange = { lastName = it },
                        phone = phone,
                        onPhoneChange = { phone = it; registerErrorMessage = null },
                        email = registerEmail,
                        onEmailChange = { registerEmail = it; registerErrorMessage = null },
                        identifier = registerIdentifier,
                        onIdentifierChange = { registerIdentifier = it; registerErrorMessage = null },
                        isLoading = registerIsLoading,
                        errorMessage = registerErrorMessage,
                        onSubmit = {
                            scope.launch {
                                registerIsLoading = true
                                registerErrorMessage = null
                                val result = authRepository.createUser(
                                    firstName = firstName,
                                    lastName = lastName,
                                    phone = phone,
                                    email = registerEmail,
                                    identifier = registerIdentifier
                                )
                                registerIsLoading = false
                                when (result) {
                                    CreateUserResult.Success -> {
                                        loginEmail = registerEmail
                                        loginIdentifier = registerIdentifier
                                        loginInfoMessage = "Cuenta creada. Inicia sesión."
                                        selectedTab = 0
                                    }
                                    CreateUserResult.AlreadyExists ->
                                        registerErrorMessage = "Ya existe una cuenta con esos datos."
                                    CreateUserResult.InvalidData ->
                                        registerErrorMessage = "Datos inválidos. Revisa los campos."
                                    CreateUserResult.NetworkError ->
                                        registerErrorMessage = "No se pudo crear la cuenta. Intenta nuevamente."
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    val spacing = AppTheme.spacing
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "CR",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Spacer(Modifier.height(spacing.lg))
        Text(
            text = "Citizen Report",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(spacing.xs))
        Text(
            text = "Reporta y mejora tu ciudad",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SegmentedTabs(selected: Int, onSelect: (Int) -> Unit) {
    val spacing = AppTheme.spacing
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            SegmentItem("Iniciar sesión", selected == 0, Modifier.weight(1f)) { onSelect(0) }
            SegmentItem("Crear cuenta", selected == 1, Modifier.weight(1f)) { onSelect(1) }
        }
    }
    Spacer(Modifier.height(spacing.none))
}

@Composable
private fun SegmentItem(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
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
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = fg
        )
    }
}

@Composable
private fun LoginForm(
    email: String,
    onEmailChange: (String) -> Unit,
    identifier: String,
    onIdentifierChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    infoMessage: String?,
    onSubmit: () -> Unit
) {
    val spacing = AppTheme.spacing
    Column(modifier = Modifier.fillMaxWidth()) {
        AppTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Correo electrónico",
            placeholder = "tu@email.com",
            leadingIcon = Icons.Outlined.AlternateEmail,
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(spacing.lg))
        AppTextField(
            value = identifier,
            onValueChange = onIdentifierChange,
            label = "Identificador",
            placeholder = "Tu documento o usuario",
            leadingIcon = Icons.Outlined.Badge,
            visualTransformation = PasswordVisualTransformation()
        )

        FeedbackBanner(error = errorMessage, info = infoMessage)

        Spacer(Modifier.height(spacing.xl))
        PrimaryButton(
            text = "Iniciar sesión",
            onClick = onSubmit,
            loading = isLoading,
            enabled = email.isNotBlank() && identifier.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CreateUserForm(
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    identifier: String,
    onIdentifierChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onSubmit: () -> Unit
) {
    val spacing = AppTheme.spacing
    Column(modifier = Modifier.fillMaxWidth()) {
        AppTextField(
            value = firstName,
            onValueChange = onFirstNameChange,
            label = "Nombre",
            placeholder = "Juan",
            leadingIcon = Icons.Outlined.Person
        )
        Spacer(Modifier.height(spacing.lg))
        AppTextField(
            value = lastName,
            onValueChange = onLastNameChange,
            label = "Apellido (opcional)",
            placeholder = "Pérez",
            leadingIcon = Icons.Outlined.Person
        )
        Spacer(Modifier.height(spacing.lg))
        AppTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = "Teléfono",
            placeholder = "3001234567",
            leadingIcon = Icons.Outlined.Phone,
            keyboardType = KeyboardType.Phone
        )
        Spacer(Modifier.height(spacing.lg))
        AppTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Correo electrónico",
            placeholder = "tu@email.com",
            leadingIcon = Icons.Outlined.AlternateEmail,
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(spacing.lg))
        AppTextField(
            value = identifier,
            onValueChange = onIdentifierChange,
            label = "Identificador",
            placeholder = "Documento o usuario",
            leadingIcon = Icons.Outlined.Badge,
            visualTransformation = PasswordVisualTransformation()
        )

        FeedbackBanner(error = errorMessage, info = null)

        Spacer(Modifier.height(spacing.xl))
        PrimaryButton(
            text = "Crear cuenta",
            onClick = onSubmit,
            loading = isLoading,
            enabled = firstName.isNotBlank() && phone.isNotBlank() &&
                email.isNotBlank() && identifier.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(spacing.md))
        Text(
            text = "Al continuar aceptas los términos del servicio.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FeedbackBanner(error: String?, info: String?) {
    val spacing = AppTheme.spacing
    val (text, bg, fg) = when {
        error != null -> Triple(error, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
        info != null  -> Triple(info,  MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        else -> return
    }
    Spacer(Modifier.height(spacing.lg))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = bg
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = fg,
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md)
        )
    }
}
