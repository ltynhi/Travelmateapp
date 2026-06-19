package com.example.travelmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.data.model.WeatherInfo
import com.example.travelmate.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class WeatherState {
    object Idle : WeatherState()
    object Loading : WeatherState()
    data class Success(val weatherMap: Map<String, WeatherInfo>) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

class WeatherViewModel : ViewModel() {
    private val repository = WeatherRepository()

    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Idle)
    val weatherState: StateFlow<WeatherState> = _weatherState

    // Cache: tránh gọi API lại khi user quay lại màn hình
    private var lastCity = ""
    private var lastDates = listOf<String>()

    /**
     * Tải thời tiết cho trip.
     *
     * @param city       Tên thành phố lấy từ địa điểm trong trip (vd: "Đà Nẵng")
     * @param startDate  Ngày bắt đầu trip "dd/MM/yyyy"
     * @param endDate    Ngày kết thúc trip "dd/MM/yyyy"
     */
    fun loadWeather(city: String, startDate: String, endDate: String) {
        if (city.isBlank() || startDate.isBlank() || endDate.isBlank()) return

        val dates = generateDateRange(startDate, endDate)

        // Nếu parse ngày thất bại hoặc trip đã qua → dùng 3 ngày từ hôm nay
        val effectiveDates = if (dates.isEmpty()) {
            val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val today = Calendar.getInstance()
            (0..2).map { offset ->
                val c = Calendar.getInstance().apply { time = today.time }
                c.add(Calendar.DAY_OF_MONTH, offset)
                fmt.format(c.time)
            }
        } else {
            // Kiểm tra xem ngày trip có nằm trong 5 ngày tới không
            // Nếu trip đã qua → vẫn gọi API với ngày hôm nay để hiển thị thời tiết hiện tại
            val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val future5 = Calendar.getInstance().apply {
                time = today.time; add(Calendar.DAY_OF_MONTH, 5)
            }
            val firstDate = try { fmt.parse(dates.first()) } catch (_: Exception) { null }
            if (firstDate != null && firstDate.before(today.time)) {
                // Trip đã qua → dùng hôm nay
                (0 until minOf(dates.size, 3)).map { offset ->
                    val c = Calendar.getInstance().apply { time = today.time }
                    c.add(Calendar.DAY_OF_MONTH, offset)
                    fmt.format(c.time)
                }
            } else {
                dates
            }
        }

        if (effectiveDates.isEmpty()) return

        // Bỏ qua nếu đã load cùng city + dates rồi
        if (city == lastCity && effectiveDates == lastDates &&
            _weatherState.value is WeatherState.Success) return

        lastCity = city
        lastDates = effectiveDates

        viewModelScope.launch {
            _weatherState.value = WeatherState.Loading
            val cityEn = toEnglishCityName(city)
            repository.getWeatherForTrip(cityEn, effectiveDates).fold(
                onSuccess = { map ->
                    _weatherState.value = if (map.isEmpty()) {
                        WeatherState.Error("Không có dữ liệu thời tiết")
                    } else {
                        WeatherState.Success(map)
                    }
                },
                onFailure = { e ->
                    _weatherState.value = WeatherState.Error(
                        e.message ?: "Không thể tải thời tiết"
                    )
                }
            )
        }
    }

    fun reset() {
        _weatherState.value = WeatherState.Idle
        lastCity = ""
        lastDates = listOf()
    }

    /**
     * Sinh danh sách ngày từ startDate đến endDate (tối đa 5 ngày vì free API).
     * Hỗ trợ cả "d/M/yyyy" lẫn "dd/MM/yyyy".
     */
    private fun generateDateRange(startDate: String, endDate: String): List<String> {
        // Thử nhiều format để xử lý cả "1/5/2026" lẫn "01/05/2026"
        val formats = listOf(
            SimpleDateFormat("d/M/yyyy", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("d/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("dd/M/yyyy", Locale.getDefault())
        ).onEach { it.isLenient = false }

        val outputFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        fun parseDate(s: String): Date? {
            for (fmt in formats) {
                try { return fmt.parse(s.trim()) } catch (_: Exception) {}
            }
            return null
        }

        return try {
            val start = parseDate(startDate) ?: return emptyList()
            val end   = parseDate(endDate)   ?: return emptyList()

            val dates = mutableListOf<String>()
            val cal    = Calendar.getInstance().apply { time = start }
            val endCal = Calendar.getInstance().apply { time = end }

            // Giới hạn 5 ngày (OpenWeather free forecast 5 ngày)
            var count = 0
            while (!cal.after(endCal) && count < 5) {
                dates.add(outputFmt.format(cal.time))
                cal.add(Calendar.DAY_OF_MONTH, 1)
                count++
            }
            dates
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Chuyển tên thành phố Việt Nam → tên tiếng Anh để OpenWeather hiểu.
     * OpenWeather hỗ trợ một số tên có dấu nhưng không ổn định.
     */
    private fun toEnglishCityName(city: String): String = when {
        city.contains("Đà Nẵng", ignoreCase = true) ||
        city.contains("Da Nang", ignoreCase = true)  -> "Da Nang"

        city.contains("Huế", ignoreCase = true) ||
        city.contains("Hue", ignoreCase = true)      -> "Hue"

        city.contains("Hà Nội", ignoreCase = true) ||
        city.contains("Ha Noi", ignoreCase = true)   -> "Hanoi"

        city.contains("Hồ Chí Minh", ignoreCase = true) ||
        city.contains("Sài Gòn", ignoreCase = true) ||
        city.contains("TPHCM", ignoreCase = true)    -> "Ho Chi Minh City"

        city.contains("Hội An", ignoreCase = true)   -> "Hoi An"
        city.contains("Nha Trang", ignoreCase = true) -> "Nha Trang"
        city.contains("Đà Lạt", ignoreCase = true)   -> "Da Lat"
        city.contains("Phú Quốc", ignoreCase = true) -> "Phu Quoc"
        city.contains("Cần Thơ", ignoreCase = true)  -> "Can Tho"
        city.contains("Hải Phòng", ignoreCase = true) -> "Hai Phong"
        city.contains("Quảng Ninh", ignoreCase = true) ||
        city.contains("Hạ Long", ignoreCase = true)  -> "Ha Long"

        else -> city // dùng nguyên tên nếu không match
    }
}
