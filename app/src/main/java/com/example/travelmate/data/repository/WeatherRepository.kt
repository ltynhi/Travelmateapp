package com.example.travelmate.data.repository

import com.example.travelmate.data.model.WeatherInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class WeatherRepository {

    companion object {
        // ⚠️ Thay bằng API key của bạn từ https://openweathermap.org/api
        // Đăng ký miễn phí → My API Keys → copy key vào đây
        const val API_KEY = "YOUR_OPENWEATHER_API_KEY"

        // OpenWeather 5-day forecast, 3-hour intervals, trả về 40 data points
        private const val BASE_URL =
            "https://api.openweathermap.org/data/2.5/forecast"
    }

    /**
     * Lấy dự báo thời tiết cho một thành phố trong khoảng ngày của trip.
     *
     * @param city      Tên thành phố, vd: "Da Nang", "Hue", "Ho Chi Minh City"
     * @param tripDates Danh sách ngày trip theo format "dd/MM/yyyy"
     * @return Map<"dd/MM/yyyy", WeatherInfo> — mỗi ngày trong trip có 1 WeatherInfo
     */
    suspend fun getWeatherForTrip(
        city: String,
        tripDates: List<String>
    ): Result<Map<String, WeatherInfo>> = withContext(Dispatchers.IO) {
        try {
            val encodedCity = java.net.URLEncoder.encode(city, "UTF-8")
            val urlStr = "$BASE_URL?q=$encodedCity&appid=$API_KEY&units=metric&lang=vi&cnt=40"
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(
                    Exception("Không thể tải thời tiết (lỗi $responseCode)")
                )
            }

            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val list = json.getJSONArray("list")

            // Parse tất cả data points từ API → nhóm theo ngày
            // Format ngày từ API: "yyyy-MM-dd HH:mm:ss"
            val apiFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val displayFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            // Map: "dd/MM/yyyy" → list các reading trong ngày đó
            data class DayReading(
                val tempMin: Double, val tempMax: Double,
                val icon: String, val description: String,
                val humidity: Int, val windSpeed: Double
            )
            val dayMap = mutableMapOf<String, MutableList<DayReading>>()

            for (i in 0 until list.length()) {
                val item = list.getJSONObject(i)
                val dtTxt = item.getString("dt_txt")
                val date = apiFmt.parse(dtTxt) ?: continue
                val dayKey = displayFmt.format(date)

                val main = item.getJSONObject("main")
                val weather = item.getJSONArray("weather").getJSONObject(0)
                val wind = item.getJSONObject("wind")

                dayMap.getOrPut(dayKey) { mutableListOf() }.add(
                    DayReading(
                        tempMin = main.getDouble("temp_min"),
                        tempMax = main.getDouble("temp_max"),
                        icon = weather.getString("icon"),
                        description = weather.getString("description"),
                        humidity = main.getInt("humidity"),
                        windSpeed = wind.getDouble("speed")
                    )
                )
            }

            // Tổng hợp mỗi ngày: min/max qua tất cả readings, icon + desc của buổi trưa
            val result = mutableMapOf<String, WeatherInfo>()
            for (dayKey in tripDates) {
                val readings = dayMap[dayKey]
                if (readings.isNullOrEmpty()) continue

                val tempMin = readings.minOf { it.tempMin }.toInt()
                val tempMax = readings.maxOf { it.tempMax }.toInt()
                // Ưu tiên reading giữa ngày (12:00 hoặc 15:00) để lấy icon đại diện
                val midday = readings.find { r ->
                    dayMap[dayKey]?.indexOf(r)?.let { it >= readings.size / 2 } == true
                } ?: readings.first()
                val avgHumidity = readings.map { it.humidity }.average().toInt()
                val avgWind = readings.map { it.windSpeed }.average()

                result[dayKey] = WeatherInfo(
                    date = dayKey,
                    tempMin = tempMin,
                    tempMax = tempMax,
                    description = midday.description.replaceFirstChar { it.uppercase() },
                    icon = midday.icon,
                    emoji = iconToEmoji(midday.icon),
                    humidity = avgHumidity,
                    windSpeed = avgWind
                )
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Chuyển mã icon OpenWeather → emoji phù hợp để hiển thị không cần load ảnh.
     * Danh sách icon: https://openweathermap.org/weather-conditions
     */
    private fun iconToEmoji(icon: String): String = when {
        icon.startsWith("01") -> "☀️"   // clear sky
        icon.startsWith("02") -> "🌤️"   // few clouds
        icon.startsWith("03") -> "⛅"   // scattered clouds
        icon.startsWith("04") -> "☁️"   // broken/overcast clouds
        icon.startsWith("09") -> "🌧️"   // shower rain
        icon.startsWith("10") -> "🌦️"   // rain
        icon.startsWith("11") -> "⛈️"   // thunderstorm
        icon.startsWith("13") -> "❄️"   // snow
        icon.startsWith("50") -> "🌫️"   // mist/fog
        else -> "🌡️"
    }
}
