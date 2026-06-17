package com.example.travelmate.ui.screens.places

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelmate.data.model.Review
import com.example.travelmate.ui.components.StarRatingBar
import com.example.travelmate.ui.theme.Peach40
import com.example.travelmate.ui.theme.SkyBlue40
import com.example.travelmate.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlaceDetailScreen(
    placeId: String,
    placeViewModel: PlaceViewModel,
    favoriteViewModel: FavoriteViewModel,
    reviewViewModel: ReviewViewModel,
    tripViewModel: TripViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val selectedPlace by placeViewModel.selectedPlace.collectAsState()
    val reviews by reviewViewModel.reviews.collectAsState()
    val trips by tripViewModel.trips.collectAsState()
    val favoriteIds by favoriteViewModel.favoriteIds.collectAsState()
    val successMessage by reviewViewModel.successMessage.collectAsState()
    val tripSuccessMessage by tripViewModel.successMessage.collectAsState()

    var showReviewDialog by remember { mutableStateOf(false) }
    var showAddToTripDialog by remember { mutableStateOf(false) }
    var reviewRating by remember { mutableStateOf(5f) }
    var reviewComment by remember { mutableStateOf("") }

    LaunchedEffect(placeId) {
        placeViewModel.loadPlaceById(placeId)
        reviewViewModel.loadReviewsForPlace(placeId)
        currentUser?.let { user ->
            tripViewModel.loadTrips(user.userId)
            reviewViewModel.loadUserReview(user.userId, placeId)
        }
    }

    LaunchedEffect(successMessage) {
        if (successMessage != null) reviewViewModel.clearMessages()
    }
    LaunchedEffect(tripSuccessMessage) {
        if (tripSuccessMessage != null) tripViewModel.clearMessages()
    }

    val place = selectedPlace
    if (place == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isFav = favoriteIds.contains(placeId)
    val allImages = place.getAllImages()
        .ifEmpty { listOf("https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800") }
    val pagerState = rememberPagerState(pageCount = { allImages.size })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(place.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        currentUser?.let { user ->
                            favoriteViewModel.toggleFavorite(user.userId, placeId)
                        }
                    }) {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFav) Peach40 else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showAddToTripDialog = true }) {
                        Icon(Icons.Filled.AddLocation, null)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Image Pager ───────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        AsyncImage(
                            model = allImages[page],
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Dot indicators
                    if (allImages.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(allImages.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (pagerState.currentPage == index) Color.White
                                            else Color.White.copy(alpha = 0.5f)
                                        )
                                )
                            }
                        }

                        // Counter top-right
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.5f)
                        ) {
                            Text(
                                "${pagerState.currentPage + 1}/${allImages.size}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ── Thông tin ─────────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                place.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, null,
                                    tint = SkyBlue40, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(place.address,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                place.category,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StarRatingBar(rating = place.rating.toFloat())
                        Spacer(Modifier.width(8.dp))
                        Text(String.format("%.1f", place.rating),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Text(" (${reviews.size} đánh giá)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Mô tả", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(place.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Đánh giá (${reviews.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showReviewDialog = true }) {
                            Icon(Icons.Filled.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Viết đánh giá")
                        }
                    }
                }
            }

            if (reviews.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Chưa có đánh giá nào. Hãy là người đầu tiên!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(reviews) { review ->
                    ReviewItem(review = review)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Review Dialog
    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text("Viết đánh giá") },
            text = {
                Column {
                    Text("Chọn số sao:")
                    StarRatingBar(rating = reviewRating, onRatingChanged = { reviewRating = it })
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reviewComment, onValueChange = { reviewComment = it },
                        label = { Text("Bình luận") },
                        modifier = Modifier.fillMaxWidth(), minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    currentUser?.let { user ->
                        reviewViewModel.addReview(
                            userId = user.userId, placeId = placeId,
                            rating = reviewRating, comment = reviewComment,
                            authorName = user.fullName, authorAvatar = user.avatarUrl
                        )
                    }
                    showReviewDialog = false; reviewComment = ""; reviewRating = 5f
                }) { Text("Gửi") }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) { Text("Hủy") }
            }
        )
    }

    // Add to Trip Dialog
    if (showAddToTripDialog) {
        AlertDialog(
            onDismissRequest = { showAddToTripDialog = false },
            title = { Text("Thêm vào chuyến đi") },
            text = {
                if (trips.isEmpty()) {
                    Text("Bạn chưa có chuyến đi nào. Hãy tạo chuyến đi trước!")
                } else {
                    Column {
                        trips.forEach { trip ->
                            TextButton(
                                onClick = {
                                    currentUser?.let { user ->
                                        tripViewModel.addPlaceToTrip(trip.tripId, placeId, user.userId)
                                    }
                                    showAddToTripDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${trip.tripName} (${trip.startDate} - ${trip.endDate})")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddToTripDialog = false }) { Text("Đóng") }
            }
        )
    }
}

@Composable
private fun ReviewItem(review: Review) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        AsyncImage(
            model = review.authorAvatar.ifBlank { "https://ui-avatars.com/api/?name=${review.authorName}&background=4A90D9&color=fff&size=64" },
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(review.authorName.ifBlank { "Người dùng" },
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(review.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StarRatingBar(rating = review.rating)
            if (review.comment.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(review.comment, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
