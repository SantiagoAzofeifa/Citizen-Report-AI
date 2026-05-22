package com.example.citizenreportai.data.repository

import com.example.citizenreportai.data.model.User
import com.example.citizenreportai.data.remote.RetrofitInstance
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.HttpException
import java.io.IOException

class RealAuthRepository : AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser

    override suspend fun login(email: String, identifier: String): LoginResult {
        val normalizedEmail = email.trim()
        val normalizedIdentifier = identifier.trim()
        val totalAttempts = 2
        var backoffMillis = 2000L
        var lastError: Exception? = null

        for (attempt in 1..totalAttempts) {
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
                val shouldRetry = e is IOException || (e is HttpException && e.code() >= 500)
                lastError = e
                if (!shouldRetry || attempt == totalAttempts) {
                    break
                }
                delay(backoffMillis)
                backoffMillis = (backoffMillis * 2).coerceAtMost(10000L)
            }
        }
        lastError?.printStackTrace()
        return LoginResult.NetworkError
    }

    override fun logout() {
        _currentUser.value = null
    }
}
