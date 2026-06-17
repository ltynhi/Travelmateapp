package com.example.travelmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.data.model.User
import com.example.travelmate.data.repository.PlaceRepository
import com.example.travelmate.data.repository.TravelPostRepository
import com.example.travelmate.data.repository.TripRepository
import com.example.travelmate.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DashboardStats(
    val totalUsers: Int = 0,
    val totalPlaces: Int = 0,
    val totalTrips: Int = 0,
    val totalPosts: Int = 0
)

class AdminViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val placeRepository = PlaceRepository()
    private val tripRepository = TripRepository()
    private val postRepository = TravelPostRepository()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _dashboardStats = MutableStateFlow(DashboardStats())
    val dashboardStats: StateFlow<DashboardStats> = _dashboardStats

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    fun loadDashboardStats() {
        viewModelScope.launch {
            _isLoading.value = true
            val totalUsers = userRepository.getTotalUsersCount()
            val totalPlaces = placeRepository.getAllPlaces().getOrNull()?.size ?: 0
            val totalTrips = tripRepository.getTotalTripsCount()
            val totalPosts = postRepository.getTotalPostsCount()
            _dashboardStats.value = DashboardStats(
                totalUsers = totalUsers,
                totalPlaces = totalPlaces,
                totalTrips = totalTrips,
                totalPosts = totalPosts
            )
            _isLoading.value = false
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = userRepository.getAllUsers()
            result.fold(
                onSuccess = { _users.value = it },
                onFailure = { e -> _error.value = e.message }
            )
            _isLoading.value = false
        }
    }

    fun blockUser(userId: String, blocked: Boolean) {
        viewModelScope.launch {
            val result = userRepository.blockUser(userId, blocked)
            result.fold(
                onSuccess = {
                    _successMessage.value = if (blocked) "Đã khóa tài khoản" else "Đã mở khóa tài khoản"
                    loadUsers()
                },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }
}
