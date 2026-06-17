package com.example.travelmate.data.model

data class User(
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val role: String = "user", // "user" or "admin"
    val isBlocked: Boolean = false
)
