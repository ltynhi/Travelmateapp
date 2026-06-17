package com.example.travelmate.data.model

data class TripInvite(
    val inviteId: String = "",
    val tripId: String = "",
    val tripName: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromUserAvatar: String = "",
    val toUserId: String = "",
    val status: String = "pending",   // "pending" | "accepted" | "declined"
    val createdAt: Long = 0L
)
