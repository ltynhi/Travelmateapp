package com.example.travelmate.data.model

data class Trip(
    val tripId: String = "",
    val userId: String = "",            // Chủ chuyến đi
    val tripName: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val placeCount: Int = 0,
    val memberIds: List<String> = emptyList()  // Danh sách userId thành viên (không gồm chủ)
)
