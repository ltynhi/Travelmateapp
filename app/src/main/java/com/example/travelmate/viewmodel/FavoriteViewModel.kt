package com.example.travelmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.data.model.Favorite
import com.example.travelmate.data.model.Place
import com.example.travelmate.data.repository.FavoriteRepository
import com.example.travelmate.data.repository.PlaceRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FavoriteViewModel : ViewModel() {
    private val favoriteRepository = FavoriteRepository()
    private val placeRepository = PlaceRepository()

    private val _favorites = MutableStateFlow<List<Favorite>>(emptyList())
    val favorites: StateFlow<List<Favorite>> = _favorites

    private val _favoritePlaces = MutableStateFlow<List<Place>>(emptyList())
    val favoritePlaces: StateFlow<List<Place>> = _favoritePlaces

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadFavorites(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = favoriteRepository.getFavoritesByUser(userId)
            result.fold(
                onSuccess = { list ->
                    _favorites.value = list
                    _favoriteIds.value = list.map { it.placeId }.toSet()
                    loadFavoritePlaces(list)
                },
                onFailure = {}
            )
            _isLoading.value = false
        }
    }

    private suspend fun loadFavoritePlaces(favorites: List<Favorite>) {
        // Fetch tất cả places song song
        val places = coroutineScope {
            favorites.map { fav ->
                async { placeRepository.getPlaceById(fav.placeId).getOrNull() }
            }.awaitAll().filterNotNull()
        }
        _favoritePlaces.value = places
    }

    fun toggleFavorite(userId: String, placeId: String) {
        viewModelScope.launch {
            if (_favoriteIds.value.contains(placeId)) {
                favoriteRepository.removeFavorite(userId, placeId)
                _favoriteIds.value = _favoriteIds.value - placeId
                _favoritePlaces.value = _favoritePlaces.value.filter { it.placeId != placeId }
            } else {
                favoriteRepository.addFavorite(userId, placeId)
                _favoriteIds.value = _favoriteIds.value + placeId
                placeRepository.getPlaceById(placeId).getOrNull()?.let { place ->
                    _favoritePlaces.value = _favoritePlaces.value + place
                }
            }
        }
    }

    fun isFavorite(placeId: String): Boolean = _favoriteIds.value.contains(placeId)

    fun getFavoriteCount(): Int = _favorites.value.size
}
