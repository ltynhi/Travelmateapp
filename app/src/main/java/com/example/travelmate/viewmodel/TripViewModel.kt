package com.example.travelmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.data.model.Trip
import com.example.travelmate.data.model.TripPlace
import com.example.travelmate.data.model.TripPlaceWithDetail
import com.example.travelmate.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TripViewModel : ViewModel() {
    private val repository = TripRepository()

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips

    private val _selectedTrip = MutableStateFlow<Trip?>(null)
    val selectedTrip: StateFlow<Trip?> = _selectedTrip

    /** Danh sách địa điểm kèm chi tiết, đã sắp xếp theo ngày/giờ */
    private val _tripPlacesWithDetail = MutableStateFlow<List<TripPlaceWithDetail>>(emptyList())
    val tripPlacesWithDetail: StateFlow<List<TripPlaceWithDetail>> = _tripPlacesWithDetail

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isPlacesLoading = MutableStateFlow(false)
    val isPlacesLoading: StateFlow<Boolean> = _isPlacesLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    // ─── Trips ───────────────────────────────────────────────────────────────

    fun loadTrips(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getTripsByUser(userId).fold(
                onSuccess = { _trips.value = it },
                onFailure = { e -> _error.value = e.message }
            )
            _isLoading.value = false
        }
    }

    fun createTrip(userId: String, tripName: String, startDate: String, endDate: String, destination: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            val trip = Trip(userId = userId, tripName = tripName, startDate = startDate, endDate = endDate, destination = destination)
            repository.createTrip(trip).fold(
                onSuccess = { newTrip ->
                    // Thêm local thay vì reload
                    _trips.value = _trips.value + newTrip
                    _successMessage.value = "Tạo chuyến đi thành công!"
                },
                onFailure = { e -> _error.value = e.message }
            )
            _isLoading.value = false
        }
    }

    fun updateTrip(trip: Trip, userId: String) {
        viewModelScope.launch {
            repository.updateTrip(trip).fold(
                onSuccess = {
                    _selectedTrip.value = trip
                    // Cập nhật local
                    _trips.value = _trips.value.map { if (it.tripId == trip.tripId) trip else it }
                    _successMessage.value = "Cập nhật thành công!"
                },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun deleteTrip(tripId: String, userId: String) {
        viewModelScope.launch {
            repository.deleteTrip(tripId).fold(
                onSuccess = {
                    // Xóa local
                    _trips.value = _trips.value.filter { it.tripId != tripId }
                    _successMessage.value = "Đã xóa chuyến đi"
                },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun selectTrip(trip: Trip) {
        _selectedTrip.value = trip
        loadTripPlacesWithDetail(trip.tripId)
    }

    /** Load trip theo ID — dùng khi xem trip được mời (không có trong local list) */
    fun loadTripById(tripId: String) {
        viewModelScope.launch {
            // Reset trước để tránh hiện trip cũ
            _selectedTrip.value = null
            _tripPlacesWithDetail.value = emptyList()
            repository.getTripById(tripId).fold(
                onSuccess = { trip ->
                    _selectedTrip.value = trip
                    loadTripPlacesWithDetail(tripId)
                },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    // ─── TripPlaces ──────────────────────────────────────────────────────────

    fun loadTripPlacesWithDetail(tripId: String) {
        viewModelScope.launch {
            _isPlacesLoading.value = true
            repository.getTripPlacesWithDetail(tripId).fold(
                onSuccess = { _tripPlacesWithDetail.value = it },
                onFailure = { e -> _error.value = e.message }
            )
            _isPlacesLoading.value = false
        }
    }

    fun addPlaceToTrip(
        tripId: String,
        placeId: String,
        userId: String,
        visitDate: String = "",
        visitTime: String = "",
        note: String = ""
    ) {
        viewModelScope.launch {
            repository.addPlaceToTrip(tripId, placeId, visitDate, visitTime, note).fold(
                onSuccess = {
                    _successMessage.value = "Đã thêm địa điểm!"
                    loadTripPlacesWithDetail(tripId)
                    // Cập nhật placeCount local — không cần reload toàn bộ trips
                    _trips.value = _trips.value.map { t ->
                        if (t.tripId == tripId) t.copy(placeCount = t.placeCount + 1) else t
                    }
                    _selectedTrip.value = _selectedTrip.value?.let { t ->
                        if (t.tripId == tripId) t.copy(placeCount = t.placeCount + 1) else t
                    }
                },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun updateTripPlace(tripPlace: TripPlace, tripId: String) {
        viewModelScope.launch {
            repository.updateTripPlace(tripPlace).fold(
                onSuccess = { loadTripPlacesWithDetail(tripId) },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun removePlaceFromTrip(tripPlaceId: String, tripId: String) {
        viewModelScope.launch {
            repository.removePlaceFromTrip(tripPlaceId, tripId).fold(
                onSuccess = {
                    loadTripPlacesWithDetail(tripId)
                    // Cập nhật placeCount local
                    _trips.value = _trips.value.map { t ->
                        if (t.tripId == tripId) t.copy(placeCount = (t.placeCount - 1).coerceAtLeast(0)) else t
                    }
                    _selectedTrip.value = _selectedTrip.value?.let { t ->
                        if (t.tripId == tripId) t.copy(placeCount = (t.placeCount - 1).coerceAtLeast(0)) else t
                    }
                },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }

    fun clearSelectedTrip() {
        _selectedTrip.value = null
        _tripPlacesWithDetail.value = emptyList()
    }

    fun getTripCount(): Int = _trips.value.size

    // ── Auto-schedule state ───────────────────────────────────────────────────
    private val _autoScheduleResult = MutableStateFlow<AutoScheduleResult?>(null)
    val autoScheduleResult: StateFlow<AutoScheduleResult?> = _autoScheduleResult

    fun clearAutoScheduleResult() { _autoScheduleResult.value = null }

    /**
     * Tự động sắp xếp lịch trình thông minh.
     *
     * Ý tưởng 1 — Xếp theo loại địa điểm + khung giờ hợp lý:
     *   Sáng (08:00): Biển, Núi, Di tích
     *   Trưa (13:00): Quán ăn, Cafe
     *   Chiều (15:00): Công viên, Check-in
     *   Tối (19:00):  Cafe, Check-in
     *
     * Ý tưởng 2 — Gợi ý thêm địa điểm cùng city/category còn slot trống.
     *
     * Ý tưởng 4 — Tính tổng chi phí ước tính và tổng thời gian.
     */
    fun autoSchedule(
        tripId: String,
        startDate: String,
        endDate: String,
        rescheduleAll: Boolean = false,
        allAvailablePlaces: List<com.example.travelmate.data.model.Place> = emptyList()
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            val toSchedule = if (rescheduleAll)
                _tripPlacesWithDetail.value
            else
                _tripPlacesWithDetail.value.filter { it.tripPlace.visitDate.isBlank() }

            if (toSchedule.isEmpty()) {
                _successMessage.value = "Tất cả địa điểm đã có lịch rồi!"
                _isLoading.value = false
                return@launch
            }

            val dates = generateDateRange(startDate, endDate)
            if (dates.isEmpty()) {
                _error.value = "Ngày bắt đầu / kết thúc không hợp lệ"
                _isLoading.value = false
                return@launch
            }

            // ── Ý tưởng 1: Sắp xếp theo category → khung giờ hợp lý ─────────
            // Ưu tiên xếp: buổi sáng → trưa → chiều → tối
            val sorted = toSchedule.sortedBy { categoryPriority(it.place.category) }

            val maxPerDay = maxOf(1, kotlin.math.ceil(
                sorted.size.toDouble() / dates.size
            ).toInt())

            var placeIndex = 0
            val updatedPlaces = mutableListOf<com.example.travelmate.data.model.TripPlace>()
            // track slot đã dùng trong mỗi ngày để gán giờ đúng theo category
            val daySlotMap = mutableMapOf<String, Int>() // date → số slot đã dùng

            for (date in dates) {
                if (placeIndex >= sorted.size) break
                var slotsThisDay = 0
                while (slotsThisDay < maxPerDay && placeIndex < sorted.size) {
                    val item = sorted[placeIndex]
                    val time = categoryToTime(item.place.category, slotsThisDay)
                    updatedPlaces.add(
                        item.tripPlace.copy(visitDate = date, visitTime = time)
                    )
                    placeIndex++
                    slotsThisDay++
                }
                daySlotMap[date] = slotsThisDay
            }

            // Lưu lên Firestore
            var hasError = false
            for (tp in updatedPlaces) {
                repository.updateTripPlace(tp).onFailure { hasError = true }
            }

            if (hasError) {
                _error.value = "Có lỗi khi lưu lịch trình"
                _isLoading.value = false
                return@launch
            }

            loadTripPlacesWithDetail(tripId)

            // ── Ý tưởng 2: Gợi ý thêm địa điểm còn slot trống ──────────────
            val suggestions = if (allAvailablePlaces.isNotEmpty()) {
                val currentPlaceIds = _tripPlacesWithDetail.value
                    .map { it.place.placeId }.toSet()
                val currentCity = toSchedule.firstOrNull()?.place?.city ?: ""

                // Tìm ngày còn ít hơn maxPerDay địa điểm
                val daysWithSlot = dates.filter { date ->
                    val count = updatedPlaces.count { it.visitDate == date }
                    count < maxPerDay
                }

                // Gợi ý địa điểm cùng city, chưa trong trip, theo category phù hợp
                val suggested = mutableListOf<SuggestedPlace>()
                for (date in daysWithSlot.take(3)) {
                    val existingCategories = updatedPlaces
                        .filter { it.visitDate == date }
                        .mapNotNull { tp ->
                            toSchedule.find { it.tripPlace.tripPlaceId == tp.tripPlaceId }
                                ?.place?.category
                        }
                    val candidate = allAvailablePlaces
                        .filter { p ->
                            p.placeId !in currentPlaceIds &&
                            p.city.equals(currentCity, ignoreCase = true) &&
                            p.category !in existingCategories
                        }
                        .maxByOrNull { it.rating }
                    if (candidate != null) {
                        suggested.add(SuggestedPlace(date = date, place = candidate))
                    }
                }
                suggested
            } else emptyList()

            // ── Ý tưởng 4: Tính tổng chi phí ước tính + thời gian ───────────
            val (estimatedMinCost, estimatedMaxCost) = toSchedule.fold(Pair(0L, 0L)) { acc, item ->
                val (mn, mx) = estimateCost(item.place.category)
                Pair(acc.first + mn, acc.second + mx)
            }
            val estimatedHours = toSchedule.fold(0) { acc, item ->
                acc + estimateHours(item.place.category)
            }

            _autoScheduleResult.value = AutoScheduleResult(
                scheduledCount = updatedPlaces.size,
                totalDays = dates.size,
                suggestions = suggestions,
                estimatedMinCost = estimatedMinCost,
                estimatedMaxCost = estimatedMaxCost,
                estimatedHours = estimatedHours
            )

            _isLoading.value = false
        }
    }

    // ── Helpers cho smart schedule ────────────────────────────────────────────

    /** Thứ tự ưu tiên xếp trong ngày theo category */
    private fun categoryPriority(category: String): Int = when (category) {
        "Biển"      -> 0   // sáng sớm
        "Núi"       -> 1   // sáng sớm, cần nhiều thời gian
        "Di tích"   -> 2   // sáng mát
        "Công viên" -> 3   // sáng/chiều
        "Check-in"  -> 4   // chiều/tối, ánh sáng đẹp
        "Quán ăn"   -> 5   // trưa/tối
        "Cafe"      -> 6   // trưa/chiều/tối
        else        -> 7
    }

    /** Gán giờ hợp lý theo category và slot trong ngày */
    private fun categoryToTime(category: String, slotIndex: Int): String {
        // Slot đầu tiên trong ngày → theo category
        if (slotIndex == 0) {
            return when (category) {
                "Biển", "Núi"       -> "07:00"
                "Di tích"           -> "08:00"
                "Công viên"         -> "09:00"
                "Quán ăn"           -> "11:30"
                "Cafe"              -> "09:00"
                "Check-in"          -> "16:00"
                else                -> "08:00"
            }
        }
        // Slot tiếp theo → theo thứ tự cố định
        val fallback = listOf("08:00","10:00","13:00","15:00","17:00","19:00")
        return fallback.getOrElse(slotIndex) { "08:00" }
    }

    /** Ước tính chi phí (min, max) theo category — đơn vị nghìn đồng */
    private fun estimateCost(category: String): Pair<Long, Long> = when (category) {
        "Biển"      -> Pair(0L,      50_000L)
        "Núi"       -> Pair(200_000L, 900_000L)
        "Di tích"   -> Pair(20_000L,  150_000L)
        "Công viên" -> Pair(0L,       80_000L)
        "Check-in"  -> Pair(0L,       50_000L)
        "Quán ăn"   -> Pair(50_000L,  200_000L)
        "Cafe"      -> Pair(30_000L,  100_000L)
        else        -> Pair(50_000L,  200_000L)
    }

    /** Ước tính số giờ tham quan theo category */
    private fun estimateHours(category: String): Int = when (category) {
        "Núi"       -> 4
        "Di tích"   -> 2
        "Biển"      -> 3
        "Công viên" -> 2
        "Quán ăn"   -> 1
        "Cafe"      -> 1
        "Check-in"  -> 1
        else        -> 2
    }

    /**
     * Sinh danh sách ngày từ startDate đến endDate (format dd/MM/yyyy).
     */
    private fun generateDateRange(startDate: String, endDate: String): List<String> {
        val formats = listOf("d/M/yyyy", "dd/MM/yyyy", "d/MM/yyyy", "dd/M/yyyy")
        fun parseDate(s: String): java.util.Date? {
            for (fmt in formats) {
                try {
                    val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.getDefault())
                    sdf.isLenient = false
                    return sdf.parse(s.trim())
                } catch (_: Exception) {}
            }
            return null
        }
        return try {
            val outputFmt = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val start = parseDate(startDate) ?: return emptyList()
            val end   = parseDate(endDate)   ?: return emptyList()
            val dates = mutableListOf<String>()
            val cal    = java.util.Calendar.getInstance().apply { time = start }
            val endCal = java.util.Calendar.getInstance().apply { time = end }
            while (!cal.after(endCal)) {
                dates.add(outputFmt.format(cal.time))
                cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
            dates
        } catch (e: Exception) { emptyList() }
    }
}

// ── Data classes kết quả auto-schedule ───────────────────────────────────────

data class SuggestedPlace(
    val date: String,
    val place: com.example.travelmate.data.model.Place
)

data class AutoScheduleResult(
    val scheduledCount: Int = 0,
    val totalDays: Int = 0,
    val suggestions: List<SuggestedPlace> = emptyList(),
    val estimatedMinCost: Long = 0L,
    val estimatedMaxCost: Long = 0L,
    val estimatedHours: Int = 0
)
