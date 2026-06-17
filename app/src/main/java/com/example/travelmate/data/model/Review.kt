package com.example.travelmate.data.model

data class Review(
    val reviewId: String = "",
    val userId: String = "",
    val placeId: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val timestamp: Long = 0L,
    val authorName: String = "",
    val authorAvatar: String = ""
)
