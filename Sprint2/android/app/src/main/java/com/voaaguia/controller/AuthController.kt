package com.voaaguia.controller

import com.voaaguia.model.User
import com.voaaguia.model.UserRole
import com.voaaguia.model.repository.ApiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthController {
    private val repository = ApiRepository()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init {
        checkCurrentSession()
    }

    private fun checkCurrentSession() {
        val uid = repository.getCurrentUserId()
        if (uid != null) {
            _isLoggedIn.value = true
        }
    }

    suspend fun login(email: String, password: String) {
        _isLoading.value = true
        _errorMessage.value = null
        val result = repository.login(email, password)
        result.onSuccess { user ->
            _currentUser.value = user
            _isLoggedIn.value = true
        }.onFailure { error ->
            _errorMessage.value = error.message ?: "Erro ao fazer login"
        }
        _isLoading.value = false
    }

    suspend fun register(email: String, password: String, name: String, role: UserRole, department: String) {
        _isLoading.value = true
        _errorMessage.value = null
        val result = repository.register(email, password, name, role, department)
        result.onSuccess { user ->
            _currentUser.value = user
            _isLoggedIn.value = true
        }.onFailure { error ->
            _errorMessage.value = error.message ?: "Erro ao cadastrar"
        }
        _isLoading.value = false
    }

    suspend fun loadCurrentUser() {
        val uid = repository.getCurrentUserId() ?: return
        try {
            val user = repository.getUser(uid)
            _currentUser.value = user
        } catch (e: Exception) {
            _errorMessage.value = "Erro ao carregar perfil"
        }
    }

    suspend fun logout() {
        try { repository.logout() } catch (_: Exception) { }
        _currentUser.value = null
        _isLoggedIn.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setError(message: String) {
        _errorMessage.value = message
        _isLoading.value = false
    }
}
