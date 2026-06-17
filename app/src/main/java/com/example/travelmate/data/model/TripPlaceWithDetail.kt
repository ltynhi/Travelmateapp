package com.example.travelmate.data.model

/** Kết hợp TripPlace + Place để hiển thị trên UI */
data class TripPlaceWithDetail(
    val tripPlace: TripPlace,
    val place: Place
)
