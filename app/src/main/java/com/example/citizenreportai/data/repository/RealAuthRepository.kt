package com.example.citizenreportai.data.repository

import com.example.citizenreportai.data.model.User
import com.example.citizenreportai.data.remote.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RealAuthRepository : AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser

    override suspend fun login(email: String, identifier: String): Boolean {
        return try {
            val users = RetrofitInstance.api.getUsers()
            val user = users.find { it.email.equals(email, ignoreCase = true) && it.identifier == identifier }
            if (user != null) {
                _currentUser.value = user
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun logout() {
        _currentUser.value = null
    }
}
