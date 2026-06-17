package com.example.travelmate.data.model

data class TravelPost(
    val postId: String = "",
    val userId: String = "",
    val tripId: String = "",        // Gắn với chuyến đi cụ thể (có thể rỗng)
    val tripName: String = "",      // Tên chuyến đi để hiển thị
    val imageUrl: String = "",
    val caption: String = "",
    val location: String = "",
    val mood: String = "",          // Cảm xúc: "😊 Vui vẻ", "😍 Tuyệt vời", v.v.
    val createdAt: Long = 0L,
    val authorName: String = "",
    val authorAvatar: String = ""
)
