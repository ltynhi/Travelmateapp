package com.example.travelmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.data.model.Place
import com.example.travelmate.data.repository.PlaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlaceViewModel : ViewModel() {
    private val repository = PlaceRepository()

    private val _places = MutableStateFlow<List<Place>>(emptyList())
    val places: StateFlow<List<Place>> = _places

    private val _filteredPlaces = MutableStateFlow<List<Place>>(emptyList())
    val filteredPlaces: StateFlow<List<Place>> = _filteredPlaces

    private val _selectedPlace = MutableStateFlow<Place?>(null)
    val selectedPlace: StateFlow<Place?> = _selectedPlace

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // ── Search state ──────────────────────────────────────────────────────────
    /** Từ khoá tìm kiếm thành phố/tỉnh */
    private val _cityQuery = MutableStateFlow("")
    val cityQuery: StateFlow<String> = _cityQuery

    /** Danh mục đang chọn */
    private val _selectedCategory = MutableStateFlow("")
    val selectedCategory: StateFlow<String> = _selectedCategory

    /** Danh sách thành phố gợi ý khi gõ */
    private val _citySuggestions = MutableStateFlow<List<String>>(emptyList())
    val citySuggestions: StateFlow<List<String>> = _citySuggestions

    /** Thành phố đang được chọn (đã xác nhận tìm kiếm) */
    private val _selectedCity = MutableStateFlow("")
    val selectedCity: StateFlow<String> = _selectedCity

    /** Các danh mục có sẵn trong thành phố đang chọn */
    private val _availableCategories = MutableStateFlow<List<String>>(emptyList())
    val availableCategories: StateFlow<List<String>> = _availableCategories

    init {
        loadPlaces()
    }

    fun loadPlaces() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllPlaces().fold(
                onSuccess = { list ->
                    _places.value = list
                    applyFilter()
                },
                onFailure = { e -> _error.value = e.message }
            )
            _isLoading.value = false
        }
    }

    /**
     * Gọi khi user gõ vào ô tìm kiếm.
     * Cập nhật gợi ý thành phố realtime.
     */
    fun onCityQueryChanged(query: String) {
        _cityQuery.value = query
        if (query.isBlank()) {
            _citySuggestions.value = emptyList()
            _selectedCity.value = ""
            _selectedCategory.value = ""
            applyFilter()
            return
        }
        // Gợi ý các thành phố khớp với query
        val allCities = _places.value
            .map { it.city }
            .filter { it.isNotBlank() }
            .distinct()
            .filter { it.contains(query, ignoreCase = true) }
            .sorted()
        _citySuggestions.value = allCities
        // Nếu chưa chọn thành phố cụ thể, vẫn filter theo query
        if (_selectedCity.value.isEmpty()) {
            applyFilter()
        }
    }

    /**
     * Gọi khi user chọn một thành phố từ gợi ý.
     */
    fun selectCity(city: String) {
        _selectedCity.value = city
        _cityQuery.value = city
        _citySuggestions.value = emptyList()
        _selectedCategory.value = ""
        updateAvailableCategories(city)
        applyFilter()
    }

    /**
     * Cập nhật danh sách danh mục có trong thành phố đã chọn.
     */
    private fun updateAvailableCategories(city: String) {
        val cats = _places.value
            .filter { it.city.equals(city, ignoreCase = true) }
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        _availableCategories.value = cats
    }

    /**
     * Gọi khi user nhấn một danh mục.
     * Toggle: nhấn lại để bỏ chọn.
     */
    fun filterByCategory(category: String) {
        _selectedCategory.value = if (_selectedCategory.value == category) "" else category
        applyFilter()
    }

    /**
     * Xoá tìm kiếm, về trạng thái ban đầu.
     */
    fun clearSearch() {
        _cityQuery.value = ""
        _selectedCity.value = ""
        _selectedCategory.value = ""
        _citySuggestions.value = emptyList()
        _availableCategories.value = emptyList()
        applyFilter()
    }

    private fun applyFilter() {
        var result = _places.value

        val city = _selectedCity.value
        val query = _cityQuery.value
        val category = _selectedCategory.value

        when {
            // Đã chọn thành phố cụ thể → filter theo city + category
            city.isNotBlank() -> {
                result = result.filter { it.city.equals(city, ignoreCase = true) }
                if (category.isNotBlank()) {
                    result = result.filter { it.category == category }
                }
            }
            // Đang gõ nhưng chưa chọn → tìm theo tên địa điểm, địa chỉ, thành phố
            query.isNotBlank() -> {
                result = result.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.address.contains(query, ignoreCase = true) ||
                    it.city.contains(query, ignoreCase = true)
                }
                if (category.isNotBlank()) {
                    result = result.filter { it.category == category }
                }
            }
            // Không tìm kiếm → hiện tất cả, filter category nếu có
            else -> {
                if (category.isNotBlank()) {
                    result = result.filter { it.category == category }
                }
            }
        }

        _filteredPlaces.value = result
    }

    // ── Place CRUD ────────────────────────────────────────────────────────────

    fun selectPlace(place: Place) { _selectedPlace.value = place }

    fun loadPlaceById(placeId: String) {
        viewModelScope.launch {
            repository.getPlaceById(placeId).fold(
                onSuccess = { _selectedPlace.value = it },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun addPlace(place: Place, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.addPlace(place).fold(
                onSuccess = { newPlace ->
                    // Cập nhật local thay vì reload toàn bộ
                    _places.value = _places.value + newPlace
                    applyFilter()
                    onSuccess()
                },
                onFailure = { e -> _error.value = e.message }
            )
            _isLoading.value = false
        }
    }

    fun updatePlace(place: Place, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updatePlace(place).fold(
                onSuccess = {
                    // Cập nhật local
                    _places.value = _places.value.map { if (it.placeId == place.placeId) place else it }
                    applyFilter()
                    onSuccess()
                },
                onFailure = { e -> _error.value = e.message }
            )
            _isLoading.value = false
        }
    }

    fun deletePlace(placeId: String) {
        viewModelScope.launch {
            repository.deletePlace(placeId).fold(
                onSuccess = {
                    // Xóa local
                    _places.value = _places.value.filter { it.placeId != placeId }
                    applyFilter()
                },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun clearError() { _error.value = null }

    // Legacy compat
    fun searchPlaces(query: String) = onCityQueryChanged(query)
    fun getSelectedCategory() = _selectedCategory.value
}
