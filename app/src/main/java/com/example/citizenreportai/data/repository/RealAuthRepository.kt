package com.example.citizenreportai.data.repository

import com.example.citizenreportai.data.model.CreateUserRequest
import com.example.citizenreportai.data.model.User
import com.example.citizenreportai.data.remote.RetrofitInstance
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class RealAuthRepository : AuthRepository {
    private companion object {
        const val LOGIN_ATTEMPTS = 4
        const val INITIAL_BACKOFF_MILLIS = 500L
        const val MAX_BACKOFF_MILLIS = 4000L
        const val LOGIN_REQUEST_TIMEOUT_MILLIS = 8000L
        const val WARMUP_TIMEOUT_MILLIS = 4000L
        const val CREATE_USER_TIMEOUT_MILLIS = 8000L
        const val USER_ROLE_ID = 2
    }

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser

    override suspend fun login(email: String, identifier: String): LoginResult {
        val normalizedEmail = email.trim()
        val normalizedIdentifier = identifier.trim()
        var backoffMillis = INITIAL_BACKOFF_MILLIS
        var lastError: Exception? = null

        for (attempt in 1..LOGIN_ATTEMPTS) {
            try {
                // Buscamos en el backend real de Supabase/Spring Boot
                val users = withTimeout(LOGIN_REQUEST_TIMEOUT_MILLIS) {
                    RetrofitInstance.api.getUsers()
                }
                val user = users.find {
                    it.email.equals(normalizedEmail, ignoreCase = true) &&
                        it.identifier == normalizedIdentifier
                }

                return if (user != null) {
                    _currentUser.value = user
                    LoginResult.Success
                } else {
                    LoginResult.InvalidCredentials
                }
            } catch (e: Exception) {
                if (e is CancellationException && e !is TimeoutCancellationException) {
                    throw e
                }
                val httpException = e as? HttpException
                val statusCode = httpException?.code()
                val retryableIOException = e is UnknownHostException ||
                    e is SocketTimeoutException ||
                    e is ConnectException
                val retryableTimeout = e is TimeoutCancellationException
                val shouldRetry = retryableTimeout || retryableIOException || statusCode == 429 ||
                    (statusCode != null && statusCode >= 500)
                lastError = e
                if (!shouldRetry || attempt == LOGIN_ATTEMPTS) {
                    break
                }
                val retryAfterHeader = httpException?.response()?.headers()?.get("Retry-After")
                val retryAfterSeconds = retryAfterHeader?.toLongOrNull()
                val retryAfterMillis = retryAfterSeconds?.let { seconds ->
                    when {
                        seconds <= 0 -> null
                        seconds > MAX_BACKOFF_MILLIS / 1000L -> MAX_BACKOFF_MILLIS
                        else -> seconds * 1000L
                    }
                } ?: retryAfterHeader?.let {
                        try {
                            val retryAfterDate = ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME)
                            val duration = Duration.between(Instant.now(), retryAfterDate.toInstant())
                            if (duration.isNegative) {
                                null
                            } else {
                                duration.toMillis()
                            }
                        } catch (_: Exception) {
                            null
                        }
                    }
                val delayMillis = retryAfterMillis?.coerceAtMost(MAX_BACKOFF_MILLIS)
                    ?: backoffMillis.coerceAtMost(MAX_BACKOFF_MILLIS)
                delay(delayMillis)
                if (retryAfterMillis == null) {
                    backoffMillis = (backoffMillis * 2).coerceAtMost(MAX_BACKOFF_MILLIS)
                }
            }
        }
        lastError?.printStackTrace()
        return LoginResult.NetworkError
    }

    override suspend fun createUser(
        firstName: String,
        lastName: String?,
        phone: String,
        email: String,
        identifier: String
    ): CreateUserResult {
        val request = CreateUserRequest(
            primerNombre = firstName.trim(),
            apellidos = lastName?.trim()?.takeIf { it.isNotEmpty() },
            telefono = phone.trim(),
            email = email.trim(),
            identificador = identifier.trim(),
            rolId = USER_ROLE_ID
        )

        return try {
            withTimeout(CREATE_USER_TIMEOUT_MILLIS) {
                RetrofitInstance.api.createUser(request)
            }
            CreateUserResult.Success
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) {
                throw e
            }
            val httpException = e as? HttpException
            when (httpException?.code()) {
                409 -> CreateUserResult.AlreadyExists
                400 -> CreateUserResult.InvalidData
                else -> CreateUserResult.NetworkError
            }
        }
    }

    override suspend fun warmUp() {
        try {
            withTimeout(WARMUP_TIMEOUT_MILLIS) {
                RetrofitInstance.api.getUsers()
            }
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) {
                throw e
            }
        }
    }

    override fun logout() {
        _currentUser.value = null
    }
}
