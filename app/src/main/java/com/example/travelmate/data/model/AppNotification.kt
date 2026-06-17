package com.example.travelmate.data.model

data class AppNotification(
    val notificationId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "general",   // "general" | "new_place" | "promotion" | "system"
    val imageUrl: String = "",
    val targetUserId: String = "",  // "" = gửi tất cả, userId cụ thể = gửi riêng
    val createdAt: Long = 0L,
    val isRead: Boolean = false,
    val readBy: List<String> = emptyList() // danh sách userId đã đọc
)
