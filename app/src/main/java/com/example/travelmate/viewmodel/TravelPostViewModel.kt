package com.example.travelmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.data.model.TravelPost
import com.example.travelmate.data.repository.TravelPostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TravelPostViewModel : ViewModel() {
    private val repository = TravelPostRepository()

    private val _posts = MutableStateFlow<List<TravelPost>>(emptyList())
    val posts: StateFlow<List<TravelPost>> = _posts

    private val _myPosts = MutableStateFlow<List<TravelPost>>(emptyList())
    val myPosts: StateFlow<List<TravelPost>> = _myPosts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    fun loadAllPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getAllPosts()
            result.fold(
                onSuccess = { _posts.value = it },
                onFailure = { e -> _error.value = e.message }
            )
            _isLoading.value = false
        }
    }

    fun loadMyPosts(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getPostsByUser(userId)
            result.fold(
                onSuccess = { _myPosts.value = it },
                onFailure = { e -> _error.value = e.message }
            )
            _isLoading.value = false
        }
    }

    /**
     * Tạo bài đăng mới với URL ảnh trực tiếp (không upload file).
     * Người dùng nhập link ảnh từ internet.
     */
    fun createPost(
        userId: String,
        authorName: String,
        authorAvatar: String,
        imageUrl: String,
        caption: String,
        location: String,
        tripId: String = "",
        tripName: String = "",
        mood: String = ""
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val post = TravelPost(
                    userId = userId,
                    imageUrl = imageUrl.trim(),
                    caption = caption.trim(),
                    location = location.trim(),
                    tripId = tripId,
                    tripName = tripName,
                    mood = mood,
                    createdAt = System.currentTimeMillis(),
                    authorName = authorName,
                    authorAvatar = authorAvatar
                )
                val result = repository.createPost(post)
                result.fold(
                    onSuccess = {
                        _successMessage.value = "Đã lưu kỷ niệm! 📖"
                        loadAllPosts()
                        loadMyPosts(userId)
                    },
                    onFailure = { e -> _error.value = e.message }
                )
            } catch (e: Exception) {
                _error.value = e.message
            }
            _isLoading.value = false
        }
    }

    fun deletePost(postId: String, userId: String) {
        viewModelScope.launch {
            repository.deletePost(postId).fold(
                onSuccess = {
                    _successMessage.value = "Đã xóa bài đăng"
                    // Cập nhật local thay vì reload
                    _posts.value = _posts.value.filter { it.postId != postId }
                    _myPosts.value = _myPosts.value.filter { it.postId != postId }
                },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun deletePostAdmin(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId).fold(
                onSuccess = {
                    _successMessage.value = "Đã xóa bài đăng"
                    _posts.value = _posts.value.filter { it.postId != postId }
                },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun updatePost(
        postId: String,
        userId: String,
        caption: String,
        location: String,
        imageUrl: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updatePost(postId, caption.trim(), location.trim(), imageUrl.trim()).fold(
                onSuccess = {
                    _successMessage.value = "Cập nhật bài viết thành công!"
                    // Cập nhật local
                    val update = { post: com.example.travelmate.data.model.TravelPost ->
                        if (post.postId == postId) post.copy(caption = caption, location = location, imageUrl = imageUrl)
                        else post
                    }
                    _posts.value = _posts.value.map(update)
                    _myPosts.value = _myPosts.value.map(update)
                },
                onFailure = { e -> _error.value = e.message }
            )
            _isLoading.value = false
        }
    }

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }

    fun getPostCount(): Int = _myPosts.value.size
}
