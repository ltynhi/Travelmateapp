package com.example.travelmate.data.model

data class TripMessage(
    val messageId: String = "",
    val tripId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String = "",
    val content: String = "",
    val createdAt: Long = 0L
)
