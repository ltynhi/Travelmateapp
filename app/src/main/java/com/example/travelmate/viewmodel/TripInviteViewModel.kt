package com.example.travelmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.data.model.Trip
import com.example.travelmate.data.model.TripInvite
import com.example.travelmate.data.model.User
import com.example.travelmate.data.repository.TripInviteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TripInviteViewModel : ViewModel() {
    private val repository = TripInviteRepository()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults

    private val _pendingInvites = MutableStateFlow<List<TripInvite>>(emptyList())
    val pendingInvites: StateFlow<List<TripInvite>> = _pendingInvites

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount

    private val _tripMembers = MutableStateFlow<List<User>>(emptyList())
    val tripMembers: StateFlow<List<User>> = _tripMembers

    private val _joinedTrips = MutableStateFlow<List<Trip>>(emptyList())
    val joinedTrips: StateFlow<List<Trip>> = _joinedTrips

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun searchUsers(query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            _isSearching.value = true
            repository.searchUsers(query).fold(
                onSuccess = { _searchResults.value = it },
                onFailure = { e -> _error.value = e.message }
            )
            _isSearching.value = false
        }
    }

    fun clearSearch() { _searchResults.value = emptyList() }

    fun sendInvite(
        tripId: String,
        tripName: String,
        fromUser: User,
        toUser: User
    ) {
        viewModelScope.launch {
            val invite = TripInvite(
                tripId = tripId,
                tripName = tripName,
                fromUserId = fromUser.userId,
                fromUserName = fromUser.fullName,
                fromUserAvatar = fromUser.avatarUrl,
                toUserId = toUser.userId,
                status = "pending",
                createdAt = System.currentTimeMillis()
            )
            repository.sendInvite(invite).fold(
                onSuccess = {
                    _successMessage.value = "Đã gửi lời mời đến ${toUser.fullName}"
                    _searchResults.value = emptyList()
                },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun loadPendingInvites(userId: String) {
        viewModelScope.launch {
            repository.getPendingInvites(userId).fold(
                onSuccess = {
                    _pendingInvites.value = it
                    _pendingCount.value = it.size
                },
                onFailure = {}
            )
        }
    }

    fun loadPendingCount(userId: String) {
        viewModelScope.launch {
            _pendingCount.value = repository.getPendingInviteCount(userId)
        }
    }

    fun acceptInvite(invite: TripInvite, userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.acceptInvite(invite).fold(
                onSuccess = {
                    _successMessage.value = "Đã tham gia chuyến đi ${invite.tripName}"
                    loadPendingInvites(userId)
                    loadJoinedTrips(userId)
                },
                onFailure = { e -> _error.value = e.message }
            )
            _isLoading.value = false
        }
    }

    fun declineInvite(inviteId: String, userId: String) {
        viewModelScope.launch {
            repository.declineInvite(inviteId).fold(
                onSuccess = {
                    _successMessage.value = "Đã từ chối lời mời"
                    loadPendingInvites(userId)
                },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun loadTripMembers(memberIds: List<String>) {
        viewModelScope.launch {
            repository.getTripMembers(memberIds).fold(
                onSuccess = { _tripMembers.value = it },
                onFailure = {}
            )
        }
    }

    fun loadJoinedTrips(userId: String) {
        viewModelScope.launch {
            repository.getJoinedTrips(userId).fold(
                onSuccess = { _joinedTrips.value = it },
                onFailure = {}
            )
        }
    }

    fun leaveTrip(tripId: String, userId: String) {
        viewModelScope.launch {
            repository.leaveTrip(tripId, userId).fold(
                onSuccess = {
                    _successMessage.value = "Đã rời khỏi chuyến đi"
                    loadJoinedTrips(userId)
                },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun clearMessages() {
        _successMessage.value = null
        _error.value = null
    }
}
