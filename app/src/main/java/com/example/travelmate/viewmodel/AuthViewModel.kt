package com.example.travelmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.data.model.User
import com.example.travelmate.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
    object LoggedOut : AuthState()
}

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    init {
        if (repository.currentUser != null) {
            loadCurrentUser()
        }
    }

    fun register(fullName: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.register(fullName, email, password)
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Success(user)
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(e.message ?: "Đăng ký thất bại")
                }
            )
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.login(email, password)
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Success(user)
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(e.message ?: "Đăng nhập thất bại")
                }
            )
        }
    }

    fun logout() {
        repository.logout()
        _currentUser.value = null
        _authState.value = AuthState.LoggedOut
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            val user = repository.getCurrentUserData()
            _currentUser.value = user
        }
    }

    fun updateAvatar(avatarUrl: String) {
        viewModelScope.launch {
            repository.updateAvatar(avatarUrl)
            _currentUser.value = _currentUser.value?.copy(avatarUrl = avatarUrl)
        }
    }

    fun updateFullName(fullName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.updateFullName(fullName)
            result.fold(
                onSuccess = {
                    _currentUser.value = _currentUser.value?.copy(fullName = fullName)
                    onResult(true)
                },
                onFailure = { onResult(false) }
            )
        }
    }

    fun changePassword(newPassword: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.changePassword(newPassword)
            result.fold(
                onSuccess = { onResult(true, "Đổi mật khẩu thành công!") },
                onFailure = { e -> onResult(false, e.message ?: "Đổi mật khẩu thất bại") }
            )
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun isLoggedIn(): Boolean = repository.currentUser != null
}
