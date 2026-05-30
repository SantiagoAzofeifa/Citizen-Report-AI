package com.example.citizenreportai.data.repository

import android.content.Context
import com.example.citizenreportai.data.model.CreateUserRequest
import com.example.citizenreportai.data.model.User
import com.example.citizenreportai.data.remote.NetworkRetry
import com.example.citizenreportai.data.remote.RetrofitInstance
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException

class RealAuthRepository(context: Context) : AuthRepository {
    private companion object {
        const val LOGIN_REQUEST_TIMEOUT_MILLIS = 20_000L
        const val WARMUP_TIMEOUT_MILLIS = 90_000L
        const val CREATE_USER_TIMEOUT_MILLIS = 20_000L
        const val USER_ROLE_ID = 1
        const val PREFS_NAME = "auth_prefs"
        const val KEY_USER = "current_user"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _currentUser = MutableStateFlow<User?>(loadSavedUser())
    override val currentUser: StateFlow<User?> = _currentUser

    private fun loadSavedUser(): User? {
        val json = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(json, User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveUser(user: User) {
        prefs.edit().putString(KEY_USER, gson.toJson(user)).apply()
    }

    private fun clearUser() {
        prefs.edit().remove(KEY_USER).apply()
    }

    override suspend fun login(email: String, identifier: String): LoginResult {
        val normalizedEmail = email.trim()
        val normalizedIdentifier = identifier.trim()

        return try {
            val users = NetworkRetry.withRetry(
                requestTimeoutMillis = LOGIN_REQUEST_TIMEOUT_MILLIS
            ) {
                RetrofitInstance.api.getUsers()
            }
            val user = users.find {
                it.email.equals(normalizedEmail, ignoreCase = true) &&
                    it.identifier == normalizedIdentifier
            }
            if (user != null) {
                _currentUser.value = user
                saveUser(user)
                LoginResult.Success
            } else {
                LoginResult.InvalidCredentials
            }
        } catch (e: CancellationException) {
            if (e !is TimeoutCancellationException) throw e
            LoginResult.NetworkError
        } catch (e: Exception) {
            e.printStackTrace()
            LoginResult.NetworkError
        }
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
            NetworkRetry.withRetry(
                requestTimeoutMillis = CREATE_USER_TIMEOUT_MILLIS
            ) {
                RetrofitInstance.api.createUser(request)
            }
            CreateUserResult.Success
        } catch (e: CancellationException) {
            if (e !is TimeoutCancellationException) throw e
            CreateUserResult.NetworkError
        } catch (e: Exception) {
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

    override suspend fun getUsers(): List<User> {
        return try {
            NetworkRetry.withRetry { RetrofitInstance.api.getUsers() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun logout() {
        _currentUser.value = null
        clearUser()
    }
}
