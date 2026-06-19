package com.example.travelmate.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelmate.navigation.NavRoutes
import com.example.travelmate.ui.components.BottomNavBar
import com.example.travelmate.ui.components.CategoryChip
import com.example.travelmate.ui.components.PlaceCard
import com.example.travelmate.ui.theme.*
import com.example.travelmate.viewmodel.AuthViewModel
import com.example.travelmate.viewmodel.FavoriteViewModel
import com.example.travelmate.viewmodel.NotificationViewModel
import com.example.travelmate.viewmodel.PlaceViewModel

// Danh mục đầy đủ — dùng khi chưa chọn thành phố
val ALL_CATEGORIES = listOf("Biển", "Núi", "Cafe", "Check-in", "Di tích", "Công viên", "Quán ăn")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    placeViewModel: PlaceViewModel,
    favoriteViewModel: FavoriteViewModel,
    notificationViewModel: NotificationViewModel,
    onPlaceClick: (String) -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToTrips: () -> Unit,
    onNavigateToTimeline: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit = {}
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val places by placeViewModel.filteredPlaces.collectAsState()
    val isLoading by placeViewModel.isLoading.collectAsState()
    val favoriteIds by favoriteViewModel.favoriteIds.collectAsState()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()

    val cityQuery by placeViewModel.cityQuery.collectAsState()
    val selectedCity by placeViewModel.selectedCity.collectAsState()
    val citySuggestions by placeViewModel.citySuggestions.collectAsState()
    val selectedCategory by placeViewModel.selectedCategory.collectAsState()
    val availableCategories by placeViewModel.availableCategories.collectAsState()
    val searchMode by placeViewModel.searchMode.collectAsState()
    val placeNameQuery by placeViewModel.placeNameQuery.collectAsState()
    val placeNameSuggestions by placeViewModel.placeNameSuggestions.collectAsState()

    // Danh mục hiển thị: nếu đã chọn thành phố → chỉ hiện danh mục có trong thành phố đó
    val displayCategories = if (selectedCity.isNotBlank() && availableCategories.isNotEmpty())
        availableCategories else ALL_CATEGORIES

    val firstName = currentUser?.fullName?.split(" ")?.lastOrNull() ?: "bạn"

    LaunchedEffect(currentUser) {
        currentUser?.let {
            favoriteViewModel.loadFavorites(it.userId)
            notificationViewModel.loadNotificationsForUser(it.userId)
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentRoute = NavRoutes.HOME,
                onItemClick = { route ->
                    when (route) {
                        NavRoutes.FAVORITES -> onNavigateToFavorites()
                        NavRoutes.TRIPS     -> onNavigateToTrips()
                        NavRoutes.TIMELINE  -> onNavigateToTimeline()
                        NavRoutes.PROFILE   -> onNavigateToProfile()
                        else -> {}
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── Hero Header ──────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(GradSkyStart, GradSkyEnd,
                                    MaterialTheme.colorScheme.background)
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier.size(140.dp).align(Alignment.TopEnd)
                            .offset(x = 30.dp, y = (-20).dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Sunshine.copy(alpha = 0.5f), Color.Transparent)
                                ), shape = CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier.size(80.dp).align(Alignment.TopStart)
                            .offset(x = (-20).dp, y = 20.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
                                ), shape = CircleShape
                            )
                    )
                    // ── Notification bell ────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 16.dp)
                    ) {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = "Thông báo",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        if (unreadCount > 0) {
                            Badge(
                                modifier = Modifier.align(Alignment.TopEnd),
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.align(Alignment.BottomStart)
                            .padding(start = 20.dp, bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val avatarUrl = currentUser?.avatarUrl?.ifBlank {
                            "https://ui-avatars.com/api/?name=${currentUser?.fullName ?: "U"}&background=4A90D9&color=fff&size=64"
                        } ?: "https://ui-avatars.com/api/?name=U&background=4A90D9&color=fff&size=64"
                        AsyncImage(
                            model = avatarUrl, contentDescription = null,
                            modifier = Modifier.size(44.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Xin chào, $firstName! ✈️",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black, color = Color.White)
                            Text("Hôm nay muốn đi đâu?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f))
                        }
                    }
                }
            }

            // ── Search bar + toggle mode ─────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = (-16).dp)
                ) {
                    // Toggle tabs: Thành phố | Tên địa điểm
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (searchMode == "city") SkyBlue40
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { placeViewModel.switchToCitySearch() }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🏙️", fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Thành phố",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (searchMode == "city") Color.White
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (searchMode == "name") SkyBlue40
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { placeViewModel.switchToNameSearch() }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔍", fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Tên địa điểm",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (searchMode == "name") Color.White
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Search input
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        if (searchMode == "city") {
                            OutlinedTextField(
                                value = cityQuery,
                                onValueChange = { placeViewModel.onCityQueryChanged(it) },
                                placeholder = { Text("Tìm thành phố, tỉnh...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                leadingIcon = {
                                    Icon(
                                        if (selectedCity.isNotBlank()) Icons.Filled.LocationCity else Icons.Filled.Search,
                                        null, tint = if (selectedCity.isNotBlank()) Mint40 else SkyBlue40
                                    )
                                },
                                trailingIcon = {
                                    if (cityQuery.isNotBlank()) {
                                        IconButton(onClick = { placeViewModel.clearSearch() }) {
                                            Icon(Icons.Filled.Close, null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true, shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (selectedCity.isNotBlank()) Mint40 else SkyBlue40,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        } else {
                            OutlinedTextField(
                                value = placeNameQuery,
                                onValueChange = { placeViewModel.onPlaceNameQueryChanged(it) },
                                placeholder = { Text("Nhập tên địa điểm...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                leadingIcon = { Icon(Icons.Filled.Search, null, tint = SkyBlue40) },
                                trailingIcon = {
                                    if (placeNameQuery.isNotBlank()) {
                                        IconButton(onClick = { placeViewModel.clearSearch() }) {
                                            Icon(Icons.Filled.Close, null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true, shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SkyBlue40,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }

                    // ── Gợi ý thành phố dropdown ─────────────────────────────
                    AnimatedVisibility(visible = citySuggestions.isNotEmpty() && searchMode == "city",
                        enter = fadeIn(), exit = fadeOut()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column {
                                citySuggestions.forEach { city ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .clickable { placeViewModel.selectCity(city) }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                            .background(SkyBlue40.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center) {
                                            Text("🏙️", fontSize = 14.sp)
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(city, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                            Text("${placeViewModel.places.value.count { it.city.equals(city, ignoreCase = true) }} địa điểm",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Filled.Search, null, tint = SkyBlue40, modifier = Modifier.size(16.dp))
                                    }
                                    if (city != citySuggestions.last()) {
                                        HorizontalDivider(modifier = Modifier.padding(start = 60.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }

                    // ── Gợi ý tên địa điểm dropdown ──────────────────────────
                    AnimatedVisibility(visible = placeNameSuggestions.isNotEmpty() && searchMode == "name",
                        enter = fadeIn(), exit = fadeOut()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column {
                                placeNameSuggestions.forEach { place ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .clickable { placeViewModel.selectPlaceByName(place) }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.foundation.layout.Box(
                                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                        ) {
                                            coil.compose.AsyncImage(
                                                model = place.imageUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(place.name, style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold, maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                            Text("📍 ${place.city}  •  ${place.category}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("⭐ ${place.rating}", style = MaterialTheme.typography.labelSmall,
                                            color = SkyBlue40, fontWeight = FontWeight.Bold)
                                    }
                                    if (place != placeNameSuggestions.last()) {
                                        HorizontalDivider(modifier = Modifier.padding(start = 62.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Thành phố đang xem (badge) ───────────────────────────────────
            if (selectedCity.isNotBlank()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .offset(y = (-8).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Mint40.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📍", fontSize = 14.sp)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    selectedCity,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Mint40
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "· ${places.size} địa điểm",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Mint40.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // ── Category chips ───────────────────────────────────────────────
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(displayCategories) { category ->
                        CategoryChip(
                            label = category,
                            selected = selectedCategory == category,
                            onClick = { placeViewModel.filterByCategory(category) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Section title ────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            when {
                                selectedCity.isNotBlank() && selectedCategory.isNotBlank() ->
                                    "$selectedCategory tại $selectedCity"
                                selectedCity.isNotBlank() ->
                                    "Khám phá $selectedCity 🌟"
                                selectedCategory.isNotBlank() ->
                                    "$selectedCategory nổi bật 🌟"
                                else -> "Địa điểm nổi bật 🌟"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "${places.size} địa điểm đang chờ bạn",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Places list ──────────────────────────────────────────────────
            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SkyBlue40)
                    }
                }
            } else if (places.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (selectedCity.isNotBlank()) "🏙️" else "🗺️",
                                fontSize = 48.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (selectedCity.isNotBlank() && selectedCategory.isNotBlank())
                                    "Chưa có $selectedCategory nào tại $selectedCity"
                                else if (selectedCity.isNotBlank())
                                    "Chưa có địa điểm nào tại $selectedCity"
                                else "Không tìm thấy địa điểm nào",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(places) { place ->
                    PlaceCard(
                        place = place,
                        isFavorite = favoriteIds.contains(place.placeId),
                        onFavoriteClick = {
                            currentUser?.let { user ->
                                favoriteViewModel.toggleFavorite(user.userId, place.placeId)
                            }
                        },
                        onClick = { onPlaceClick(place.placeId) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
