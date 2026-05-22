package com.example.citizenreportai.data.repository

import com.example.citizenreportai.data.model.User
import kotlinx.coroutines.flow.StateFlow

sealed class LoginResult {
    data object Success : LoginResult()
    data object InvalidCredentials : LoginResult()
    data object NetworkError : LoginResult()
}

interface AuthRepository {
    val currentUser: StateFlow<User?>
    suspend fun login(email: String, identifier: String): LoginResult
    suspend fun warmUp()
    fun logout()
}
