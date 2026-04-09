package com.example.citizenreportai.data.repository

import com.example.citizenreportai.data.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<User?>
    suspend fun login(email: String, identifier: String): Boolean
    fun logout()
}
