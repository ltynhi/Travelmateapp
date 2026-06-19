package com.example.travelmate.data.model

data class TripPlace(
    val tripPlaceId: String = "",
    val tripId: String = "",
    val placeId: String = "",
    val visitDate: String = "",      // "dd/MM/yyyy"
    val visitTime: String = "",      // "HH:mm"
    val note: String = "",           // Ghi chú cho địa điểm này
    val estimatedCost: Long = 0L,   // Chi phí ước tính (VNĐ) — user có thể chỉnh
    val orderIndex: Int = 0          // Thứ tự trong ngày
)
