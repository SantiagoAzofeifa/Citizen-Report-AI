package com.example.citizenreportai.data.repository

import com.example.citizenreportai.data.model.User
import kotlinx.coroutines.flow.StateFlow

sealed class LoginResult {
    data object Success : LoginResult()
    data object InvalidCredentials : LoginResult()
    data object NetworkError : LoginResult()
}

sealed class CreateUserResult {
    data object Success : CreateUserResult()
    data object AlreadyExists : CreateUserResult()
    data object InvalidData : CreateUserResult()
    data object NetworkError : CreateUserResult()
}

interface AuthRepository {
    val currentUser: StateFlow<User?>
    suspend fun login(email: String, identifier: String): LoginResult
    suspend fun createUser(
        firstName: String,
        lastName: String?,
        phone: String,
        email: String,
        identifier: String
    ): CreateUserResult
    suspend fun warmUp()
    suspend fun getUsers(): List<User>
    fun logout()
}
