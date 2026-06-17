package com.example.travelmate.ui.screens.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.travelmate.ui.components.PlaceCard
import com.example.travelmate.viewmodel.AuthViewModel
import com.example.travelmate.viewmodel.FavoriteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    authViewModel: AuthViewModel,
    favoriteViewModel: FavoriteViewModel,
    onPlaceClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val favoritePlaces by favoriteViewModel.favoritePlaces.collectAsState()
    val favoriteIds by favoriteViewModel.favoriteIds.collectAsState()
    val isLoading by favoriteViewModel.isLoading.collectAsState()

    LaunchedEffect(currentUser) {
        currentUser?.let { favoriteViewModel.loadFavorites(it.userId) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Địa điểm yêu thích") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (favoritePlaces.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Chưa có địa điểm yêu thích",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Nhấn ❤️ để lưu địa điểm bạn thích",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "${favoritePlaces.size} địa điểm yêu thích",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(favoritePlaces) { place ->
                    PlaceCard(
                        place = place,
                        isFavorite = favoriteIds.contains(place.placeId),
                        onFavoriteClick = {
                            currentUser?.let { user ->
                                favoriteViewModel.toggleFavorite(user.userId, place.placeId)
                            }
                        },
                        onClick = { onPlaceClick(place.placeId) }
                    )
                }
            }
        }
    }
}
