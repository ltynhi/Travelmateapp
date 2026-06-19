package com.example.travelmate.data.model

/**
 * Thông tin thời tiết cho một ngày cụ thể.
 * Lấy từ OpenWeatherMap API (forecast endpoint).
 */
data class WeatherInfo(
    val date: String = "",           // "dd/MM/yyyy" — khớp với visitDate trong TripPlace
    val tempMin: Int = 0,            // Nhiệt độ thấp nhất (°C)
    val tempMax: Int = 0,            // Nhiệt độ cao nhất (°C)
    val description: String = "",    // "Nhiều mây", "Có mưa", "Nắng"...
    val icon: String = "",           // mã icon OpenWeather, vd: "01d", "10d"
    val emoji: String = "",          // emoji tương ứng để hiển thị
    val humidity: Int = 0,           // Độ ẩm (%)
    val windSpeed: Double = 0.0      // Tốc độ gió (m/s)
)
