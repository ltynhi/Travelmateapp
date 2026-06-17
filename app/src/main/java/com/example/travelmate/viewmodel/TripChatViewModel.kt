package com.example.travelmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.data.model.TripMessage
import com.example.travelmate.data.model.User
import com.example.travelmate.data.repository.TripChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TripChatViewModel : ViewModel() {
    private val repository = TripChatRepository()

    private val _messages = MutableStateFlow<List<TripMessage>>(emptyList())
    val messages: StateFlow<List<TripMessage>> = _messages

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private var listenJob: Job? = null
    private var currentTripId: String = ""

    fun startListening(tripId: String) {
        // Nếu đang nghe trip này rồi thì không làm gì
        if (currentTripId == tripId && listenJob?.isActive == true) return

        // Cancel job cũ nếu có
        listenJob?.cancel()
        currentTripId = tripId
        _messages.value = emptyList()

        listenJob = viewModelScope.launch {
            repository.getMessagesFlow(tripId).collect { messages ->
                _messages.value = messages
            }
        }
    }

    fun sendMessage(tripId: String, content: String, sender: User) {
        if (content.isBlank()) return
        viewModelScope.launch {
            _isSending.value = true
            val message = TripMessage(
                tripId = tripId,
                senderId = sender.userId,
                senderName = sender.fullName,
                senderAvatar = sender.avatarUrl,
                content = content.trim(),
                createdAt = System.currentTimeMillis()
            )
            repository.sendMessage(message)
            _isSending.value = false
            // Không cần reload — Firestore listener tự cập nhật realtime
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
            // Listener tự cập nhật
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenJob?.cancel()
    }
}
