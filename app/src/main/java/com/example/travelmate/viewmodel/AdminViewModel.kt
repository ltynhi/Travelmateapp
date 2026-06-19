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

/** Dữ liệu một cột trong biểu đồ bar */
data class ChartEntry(val label: String, val value: Int)

class AdminViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val placeRepository = PlaceRepository()
    private val tripRepository = TripRepository()
    private val postRepository = TravelPostRepository()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _dashboardStats = MutableStateFlow(DashboardStats())
    val dashboardStats: StateFlow<DashboardStats> = _dashboardStats

    // ── Chart data ────────────────────────────────────────────────────────────
    /** Địa điểm theo thành phố */
    private val _placesByCity = MutableStateFlow<List<ChartEntry>>(emptyList())
    val placesByCity: StateFlow<List<ChartEntry>> = _placesByCity

    /** Chuyến đi theo tháng (6 tháng gần nhất) */
    private val _tripsByMonth = MutableStateFlow<List<ChartEntry>>(emptyList())
    val tripsByMonth: StateFlow<List<ChartEntry>> = _tripsByMonth

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    fun loadDashboardStats() {
        viewModelScope.launch {
            _isLoading.value = true

            // Stats cơ bản
            val places = placeRepository.getAllPlaces().getOrNull() ?: emptyList()
            val totalUsers = userRepository.getTotalUsersCount()
            val totalTrips = tripRepository.getTotalTripsCount()
            val totalPosts = postRepository.getTotalPostsCount()

            _dashboardStats.value = DashboardStats(
                totalUsers = totalUsers,
                totalPlaces = places.size,
                totalTrips = totalTrips,
                totalPosts = totalPosts
            )

            // ── Biểu đồ 1: Địa điểm theo thành phố (top 5) ──────────────────
            val cityChart = places
                .groupBy { it.city }
                .filter { it.key.isNotBlank() }
                .map { (city, list) -> ChartEntry(city, list.size) }
                .sortedByDescending { it.value }
                .take(5)
            _placesByCity.value = cityChart

            // ── Biểu đồ 2: Chuyến đi theo tháng (6 tháng gần nhất) ──────────
            val allTrips = tripRepository.getAllTrips().getOrNull() ?: emptyList()
            val fmt = java.text.SimpleDateFormat("MM/yyyy", java.util.Locale.getDefault())
            val fmtParse = listOf("d/M/yyyy", "dd/MM/yyyy", "d/MM/yyyy").map {
                java.text.SimpleDateFormat(it, java.util.Locale.getDefault()).also { s -> s.isLenient = false }
            }

            // Sinh 6 tháng gần nhất
            val months = (5 downTo 0).map { offset ->
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.MONTH, -offset)
                fmt.format(cal.time)
            }

            val tripsByMonth = allTrips.groupBy { trip ->
                if (trip.startDate.isBlank()) return@groupBy ""
                var parsed: java.util.Date? = null
                for (f in fmtParse) {
                    try { parsed = f.parse(trip.startDate.trim()); break } catch (_: Exception) {}
                }
                if (parsed != null) fmt.format(parsed) else ""
            }

            val monthChart = months.map { month ->
                ChartEntry(
                    label = month.substring(0, 2), // chỉ lấy "MM"
                    value = tripsByMonth[month]?.size ?: 0
                )
            }
            _tripsByMonth.value = monthChart

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
