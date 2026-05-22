package com.example.citizenreportai.ui.screens.login

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import com.example.citizenreportai.data.repository.AuthRepository
import com.example.citizenreportai.data.repository.CreateUserResult
import com.example.citizenreportai.data.repository.LoginResult
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

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "loginButtonScale"
    )

    LaunchedEffect(Unit) {
        authRepository.warmUp()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo / Title area
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "CR",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Citizen Report AI",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Reporta y mejora tu ciudad hoy",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Iniciar sesión") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Crear usuario") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (selectedTab) {
            0 -> LoginForm(
                email = loginEmail,
                onEmailChange = { loginEmail = it },
                identifier = loginIdentifier,
                onIdentifierChange = { loginIdentifier = it },
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
                            LoginResult.InvalidCredentials -> loginErrorMessage = "Credenciales incorrectas"
                            LoginResult.NetworkError -> loginErrorMessage =
                                "No se pudo conectar con el servidor. Intenta de nuevo en unos segundos."
                        }
                    }
                },
                buttonScale = buttonScale,
                interactionSource = interactionSource
            )
            else -> CreateUserForm(
                firstName = firstName,
                onFirstNameChange = { firstName = it },
                lastName = lastName,
                onLastNameChange = { lastName = it },
                phone = phone,
                onPhoneChange = { phone = it },
                email = registerEmail,
                onEmailChange = { registerEmail = it },
                identifier = registerIdentifier,
                onIdentifierChange = { registerIdentifier = it },
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
                                loginInfoMessage = "Usuario creado. Inicia sesión."
                                selectedTab = 0
                            }
                            CreateUserResult.AlreadyExists ->
                                registerErrorMessage = "El usuario ya existe."
                            CreateUserResult.InvalidData ->
                                registerErrorMessage = "Datos inválidos. Revisa los campos."
                            CreateUserResult.NetworkError ->
                                registerErrorMessage = "No se pudo crear el usuario. Intenta nuevamente."
                        }
                    }
                }
            )
        }
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
    onSubmit: () -> Unit,
    buttonScale: Float,
    interactionSource: MutableInteractionSource
) {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Correo electrónico") },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = identifier,
        onValueChange = onIdentifierChange,
        label = { Text("Identificador (identifier)") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    )

    if (infoMessage != null) {
        Text(
            text = infoMessage,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
    }

    if (errorMessage != null) {
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
    }

    Spacer(modifier = Modifier.height(40.dp))

    Button(
        onClick = onSubmit,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
            },
        shape = RoundedCornerShape(16.dp),
        enabled = !isLoading && email.isNotBlank() && identifier.isNotBlank(),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp,
            disabledElevation = 0.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
        } else {
            Text("Iniciar Sesión", fontSize = 18.sp)
        }
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
    OutlinedTextField(
        value = firstName,
        onValueChange = onFirstNameChange,
        label = { Text("Nombre") },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = lastName,
        onValueChange = onLastNameChange,
        label = { Text("Apellido (opcional)") },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = phone,
        onValueChange = onPhoneChange,
        label = { Text("Teléfono") },
        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Correo electrónico") },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = identifier,
        onValueChange = onIdentifierChange,
        label = { Text("Identificador (identifier)") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Rol: Usuario",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (errorMessage != null) {
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
    }

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onSubmit,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        enabled = !isLoading &&
            firstName.isNotBlank() &&
            phone.isNotBlank() &&
            email.isNotBlank() &&
            identifier.isNotBlank(),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp,
            disabledElevation = 0.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
        } else {
            Text("Crear usuario", fontSize = 18.sp)
        }
    }
}
