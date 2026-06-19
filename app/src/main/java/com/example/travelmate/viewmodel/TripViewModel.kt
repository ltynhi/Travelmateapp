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

    fun createTrip(userId: String, tripName: String, startDate: String, endDate: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val trip = Trip(userId = userId, tripName = tripName, startDate = startDate, endDate = endDate)
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

    /**
     * Tự động sắp xếp lịch trình cho trip.
     *
     * Thuật toán:
     * 1. Lấy tất cả địa điểm chưa có ngày (visitDate trống)
     * 2. Tính số ngày của trip từ startDate → endDate
     * 3. Phân bổ đều địa điểm vào từng ngày (tối đa [maxPerDay] địa điểm/ngày)
     * 4. Gán giờ tham quan gợi ý theo slot trong ngày
     * 5. Lưu tất cả lên Firestore bằng batch update
     */
    fun autoSchedule(tripId: String, startDate: String, endDate: String) {
        viewModelScope.launch {
            _isLoading.value = true

            // Chỉ lấy địa điểm chưa được xếp ngày
            val unscheduled = _tripPlacesWithDetail.value
                .filter { it.tripPlace.visitDate.isBlank() }

            if (unscheduled.isEmpty()) {
                _successMessage.value = "Tất cả địa điểm đã có lịch rồi!"
                _isLoading.value = false
                return@launch
            }

            // Sinh danh sách ngày từ startDate đến endDate
            val dates = generateDateRange(startDate, endDate)
            if (dates.isEmpty()) {
                _error.value = "Ngày bắt đầu / kết thúc không hợp lệ"
                _isLoading.value = false
                return@launch
            }

            // Số địa điểm tối đa mỗi ngày (chia đều, tối thiểu 1)
            val maxPerDay = maxOf(1, kotlin.math.ceil(
                unscheduled.size.toDouble() / dates.size
            ).toInt())

            // Giờ tham quan gợi ý theo thứ tự trong ngày
            val suggestedTimes = listOf(
                "08:00", "10:00", "13:00", "15:00", "17:00", "19:00"
            )

            // Phân bổ địa điểm vào ngày
            var placeIndex = 0
            val updatedPlaces = mutableListOf<com.example.travelmate.data.model.TripPlace>()

            for (date in dates) {
                if (placeIndex >= unscheduled.size) break
                var slotIndex = 0
                while (slotIndex < maxPerDay && placeIndex < unscheduled.size) {
                    val tp = unscheduled[placeIndex].tripPlace
                    val suggestedTime = suggestedTimes.getOrElse(slotIndex) { "" }
                    updatedPlaces.add(
                        tp.copy(
                            visitDate = date,
                            visitTime = suggestedTime
                        )
                    )
                    placeIndex++
                    slotIndex++
                }
            }

            // Lưu tất cả lên Firestore
            var hasError = false
            for (tp in updatedPlaces) {
                repository.updateTripPlace(tp).onFailure { hasError = true }
            }

            if (hasError) {
                _error.value = "Có lỗi khi lưu lịch trình, thử lại nhé"
            } else {
                _successMessage.value = "✅ Đã tự động xếp lịch ${updatedPlaces.size} địa điểm!"
                loadTripPlacesWithDetail(tripId)
            }
            _isLoading.value = false
        }
    }

    /**
     * Sinh danh sách ngày từ startDate đến endDate (format dd/MM/yyyy).
     */
    private fun generateDateRange(startDate: String, endDate: String): List<String> {
        return try {
            val fmt = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val start = fmt.parse(startDate) ?: return emptyList()
            val end = fmt.parse(endDate) ?: return emptyList()
            val dates = mutableListOf<String>()
            val cal = java.util.Calendar.getInstance().apply { time = start }
            val endCal = java.util.Calendar.getInstance().apply { time = end }
            while (!cal.after(endCal)) {
                dates.add(fmt.format(cal.time))
                cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
            dates
        } catch (e: Exception) {
            emptyList()
        }
    }
}
