package com.example.travelmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.data.model.AppNotification
import com.example.travelmate.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    private val repository = NotificationRepository()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications

    private val _allNotifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val allNotifications: StateFlow<List<AppNotification>> = _allNotifications

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    fun loadNotificationsForUser(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getNotificationsForUser(userId).fold(
                onSuccess = { list ->
                    _notifications.value = list
                    _unreadCount.value = list.count { !it.readBy.contains(userId) }
                },
                onFailure = {}
            )
            _isLoading.value = false
        }
    }

    fun loadAllNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllNotifications().fold(
                onSuccess = { _allNotifications.value = it },
                onFailure = {}
            )
            _isLoading.value = false
        }
    }

    fun markAsRead(notificationId: String, userId: String) {
        viewModelScope.launch {
            repository.markAsRead(notificationId, userId)
            // Cập nhật local state
            _notifications.value = _notifications.value.map { n ->
                if (n.notificationId == notificationId && !n.readBy.contains(userId))
                    n.copy(readBy = n.readBy + userId)
                else n
            }
            _unreadCount.value = _notifications.value.count { !it.readBy.contains(userId) }
        }
    }

    fun markAllAsRead(userId: String) {
        viewModelScope.launch {
            _notifications.value.forEach { n ->
                if (!n.readBy.contains(userId)) {
                    repository.markAsRead(n.notificationId, userId)
                }
            }
            _notifications.value = _notifications.value.map { n ->
                if (!n.readBy.contains(userId)) n.copy(readBy = n.readBy + userId) else n
            }
            _unreadCount.value = 0
        }
    }

    // ── Admin functions ───────────────────────────────────────────────────────

    fun sendNotification(
        title: String,
        message: String,
        type: String = "general",
        imageUrl: String = "",
        targetUserId: String = ""   // "" = gửi tất cả
    ) {
        viewModelScope.launch {
            val notification = AppNotification(
                title = title.trim(),
                message = message.trim(),
                type = type,
                imageUrl = imageUrl.trim(),
                targetUserId = targetUserId,
                createdAt = System.currentTimeMillis()
            )
            repository.createNotification(notification).fold(
                onSuccess = {
                    _successMessage.value = "Đã gửi thông báo thành công!"
                    loadAllNotifications()
                },
                onFailure = {}
            )
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            repository.deleteNotification(notificationId).fold(
                onSuccess = {
                    _successMessage.value = "Đã xóa thông báo"
                    loadAllNotifications()
                },
                onFailure = {}
            )
        }
    }

    fun clearMessages() { _successMessage.value = null }
}
