package com.example.travelmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.data.model.Review
import com.example.travelmate.data.repository.PlaceRepository
import com.example.travelmate.data.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReviewViewModel : ViewModel() {
    private val repository = ReviewRepository()
    private val placeRepository = PlaceRepository()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews

    private val _allReviews = MutableStateFlow<List<Review>>(emptyList())
    val allReviews: StateFlow<List<Review>> = _allReviews

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private val _userReview = MutableStateFlow<Review?>(null)
    val userReview: StateFlow<Review?> = _userReview

    fun loadReviewsForPlace(placeId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getReviewsByPlace(placeId)
            result.fold(
                onSuccess = { _reviews.value = it },
                onFailure = { e -> _error.value = e.message }
            )
            _isLoading.value = false
        }
    }

    fun loadAllReviews() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getAllReviews()
            result.fold(
                onSuccess = { _allReviews.value = it },
                onFailure = { e -> _error.value = e.message }
            )
            _isLoading.value = false
        }
    }

    fun loadUserReview(userId: String, placeId: String) {
        viewModelScope.launch {
            _userReview.value = repository.getUserReviewForPlace(userId, placeId)
        }
    }

    fun addReview(
        userId: String,
        placeId: String,
        rating: Float,
        comment: String,
        authorName: String,
        authorAvatar: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val review = Review(
                userId = userId,
                placeId = placeId,
                rating = rating,
                comment = comment,
                timestamp = System.currentTimeMillis(),
                authorName = authorName,
                authorAvatar = authorAvatar
            )
            val result = repository.addReview(review)
            result.fold(
                onSuccess = {
                    _successMessage.value = "Đánh giá thành công!"
                    loadReviewsForPlace(placeId)
                    // Update place average rating
                    val avgRating = repository.getAverageRating(placeId)
                    placeRepository.updatePlaceRating(placeId, avgRating)
                },
                onFailure = { e -> _error.value = e.message }
            )
            _isLoading.value = false
        }
    }

    fun deleteReview(reviewId: String, placeId: String) {
        viewModelScope.launch {
            repository.deleteReview(reviewId).fold(
                onSuccess = {
                    _successMessage.value = "Đã xóa đánh giá"
                    // Cập nhật local
                    _reviews.value = _reviews.value.filter { it.reviewId != reviewId }
                    val avgRating = repository.getAverageRating(placeId)
                    placeRepository.updatePlaceRating(placeId, avgRating)
                },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun deleteReviewAdmin(reviewId: String) {
        viewModelScope.launch {
            repository.deleteReview(reviewId).fold(
                onSuccess = {
                    _successMessage.value = "Đã xóa đánh giá"
                    _allReviews.value = _allReviews.value.filter { it.reviewId != reviewId }
                },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }
}
