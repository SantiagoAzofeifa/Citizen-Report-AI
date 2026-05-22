package com.example.citizenreportai.data.repository

import com.example.citizenreportai.data.model.User
import com.example.citizenreportai.data.remote.RetrofitInstance
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.io.IOException

class RealAuthRepository : AuthRepository {
    private companion object {
        const val LOGIN_ATTEMPTS = 2
        const val INITIAL_BACKOFF_MILLIS = 2000L
        const val MAX_BACKOFF_MILLIS = 10000L
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
                val users = RetrofitInstance.api.getUsers()
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
                val httpException = e as? HttpException
                val statusCode = httpException?.code()
                val retryableIOException = e is UnknownHostException ||
                    e is SocketTimeoutException ||
                    e is ConnectException
                val shouldRetry = retryableIOException || statusCode == 429 || (statusCode != null && statusCode >= 500)
                lastError = e
                if (!shouldRetry || attempt == LOGIN_ATTEMPTS) {
                    break
                }
                val retryAfterHeader = httpException?.response()?.headers()?.get("Retry-After")
                val retryAfterMillis = retryAfterHeader?.toLongOrNull()?.let { it * 1000L }
                    ?: retryAfterHeader?.let {
                        try {
                            val retryAfterDate = ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME)
                            val delayMillis = Duration.between(Instant.now(), retryAfterDate.toInstant()).toMillis()
                            delayMillis.takeIf { millis -> millis > 0 }
                        } catch (parseError: Exception) {
                            null
                        }
                    }
                val boundedRetryAfterMillis = retryAfterMillis?.coerceAtMost(MAX_BACKOFF_MILLIS)
                val delayMillis = boundedRetryAfterMillis ?: backoffMillis
                delay(delayMillis)
                if (retryAfterMillis == null) {
                    backoffMillis = (backoffMillis * 2).coerceAtMost(MAX_BACKOFF_MILLIS)
                }
            }
        }
        lastError?.printStackTrace()
        return LoginResult.NetworkError
    }

    override fun logout() {
        _currentUser.value = null
    }
}
