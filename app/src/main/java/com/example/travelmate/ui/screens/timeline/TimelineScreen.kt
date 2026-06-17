package com.example.travelmate.ui.screens.timeline

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelmate.data.model.TravelPost
import com.example.travelmate.data.model.Trip
import com.example.travelmate.ui.theme.*
import com.example.travelmate.viewmodel.AuthViewModel
import com.example.travelmate.viewmodel.TravelPostViewModel
import com.example.travelmate.viewmodel.TripInviteViewModel
import com.example.travelmate.viewmodel.TripViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

val MOODS = listOf("😊 Vui vẻ", "😍 Tuyệt vời", "😌 Bình yên", "🤩 Phấn khích", "😴 Mệt mỏi", "🥰 Hạnh phúc")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    authViewModel: AuthViewModel,
    postViewModel: TravelPostViewModel,
    tripViewModel: TripViewModel,
    tripInviteViewModel: TripInviteViewModel,
    onCreatePost: () -> Unit,   // giữ để AppNavGraph không lỗi, nhưng không dùng nữa
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by authViewModel.currentUser.collectAsState()
    val myPosts by postViewModel.myPosts.collectAsState()
    val trips by tripViewModel.trips.collectAsState()
    val joinedTrips by tripInviteViewModel.joinedTrips.collectAsState()
    val isLoading by postViewModel.isLoading.collectAsState()
    val successMessage by postViewModel.successMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Gộp trips của mình + trips được mời, loại trùng
    val allTrips = remember(trips, joinedTrips) {
        (trips + joinedTrips).distinctBy { it.tripId }
    }

    // Filter state
    var selectedTripId by remember { mutableStateOf("") }

    val selectedTripName = remember(selectedTripId, allTrips) {
        allTrips.find { it.tripId == selectedTripId }?.tripName ?: ""
    }

    // Edit state
    var editingPost by remember { mutableStateOf<TravelPost?>(null) }

    // ── Bottom sheet state ────────────────────────────────────────────────────
    var showCreateSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Form state cho bottom sheet (reset khi mở)
    var sheetImageUrl by remember { mutableStateOf("") }
    var sheetSelectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var sheetIsUploading by remember { mutableStateOf(false) }
    var sheetUploadError by remember { mutableStateOf("") }
    var sheetCaption by remember { mutableStateOf("") }
    var sheetLocation by remember { mutableStateOf("") }
    var sheetMood by remember { mutableStateOf("") }
    var sheetExpandedMood by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            sheetSelectedImageUri = it
            sheetImageUrl = ""
            sheetUploadError = ""
            scope.launch {
                sheetIsUploading = true
                val result = com.example.travelmate.utils.CloudinaryUploader.uploadImage(context, it, "timeline")
                result.fold(
                    onSuccess = { url -> sheetImageUrl = url; sheetUploadError = "" },
                    onFailure = { e -> sheetUploadError = "Upload thất bại: ${e.message}"; sheetSelectedImageUri = null }
                )
                sheetIsUploading = false
            }
        }
    }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            postViewModel.loadMyPosts(user.userId)
            tripViewModel.loadTrips(user.userId)
            tripInviteViewModel.loadJoinedTrips(user.userId)
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            postViewModel.clearMessages()
            // Đóng sheet sau khi lưu thành công, giữ nguyên selectedTripId
            showCreateSheet = false
        }
    }

    // Reset form khi mở sheet
    LaunchedEffect(showCreateSheet) {
        if (showCreateSheet) {
            sheetImageUrl = ""
            sheetSelectedImageUri = null
            sheetIsUploading = false
            sheetUploadError = ""
            sheetCaption = ""
            sheetLocation = ""
            sheetMood = ""
        }
    }

    val filteredPosts = if (selectedTripId.isEmpty()) myPosts
    else myPosts.filter { it.tripId == selectedTripId }

    val groupedPosts = filteredPosts.groupBy { post ->
        SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Date(post.createdAt))
    }

    val canPost = !isLoading && !sheetIsUploading && (sheetCaption.isNotBlank() || sheetImageUrl.isNotBlank())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nhật ký hành trình", fontWeight = FontWeight.Bold)
                        Text(
                            "${myPosts.size} kỷ niệm đã lưu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = SkyBlue40,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Thêm kỷ niệm")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Filter theo chuyến đi ─────────────────────────────────────────
            if (allTrips.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        TripFilterChip(
                            label = "Tất cả (${myPosts.size})",
                            selected = selectedTripId.isEmpty(),
                            onClick = { selectedTripId = "" }
                        )
                    }
                    items(allTrips) { trip ->
                        val count = myPosts.count { it.tripId == trip.tripId }
                        TripFilterChip(
                            label = "${trip.tripName} ($count)",
                            selected = selectedTripId == trip.tripId,
                            onClick = { selectedTripId = trip.tripId }
                        )
                    }
                }
                HorizontalDivider()
            }

            // ── Nội dung ─────────────────────────────────────────────────────
            if (isLoading && myPosts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SkyBlue40)
                }
            } else if (filteredPosts.isEmpty()) {
                EmptyTimelineHint(
                    hasTrips = allTrips.isNotEmpty(),
                    onAdd = { showCreateSheet = true }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    groupedPosts.forEach { (monthYear, posts) ->
                        item {
                            MonthHeader(monthYear = monthYear, count = posts.size)
                        }
                        items(posts, key = { it.postId }) { post ->
                            MemoryCard(
                                post = post,
                                onEdit = { editingPost = post },
                                onDelete = {
                                    currentUser?.let { user ->
                                        postViewModel.deletePost(post.postId, user.userId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog chỉnh sửa
    editingPost?.let { post ->
        EditPostDialog(
            post = post,
            trips = allTrips,
            onSave = { newCaption, newLocation, newImageUrl, newMood ->
                currentUser?.let { user ->
                    postViewModel.updatePost(
                        postId = post.postId,
                        userId = user.userId,
                        caption = newCaption,
                        location = newLocation,
                        imageUrl = newImageUrl
                    )
                }
                editingPost = null
            },
            onDismiss = { editingPost = null }
        )
    }

    // ── Bottom sheet tạo kỷ niệm ─────────────────────────────────────────────
    if (showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            val sheetPreviewModel: Any = sheetSelectedImageUri ?: sheetImageUrl.trim()
            val showPreview = sheetSelectedImageUri != null || sheetImageUrl.trim().startsWith("http")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Thêm kỷ niệm mới", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (selectedTripName.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(50), color = SkyBlue40.copy(alpha = 0.12f)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.FlightTakeoff, null, Modifier.size(12.dp), tint = SkyBlue40)
                                Spacer(Modifier.width(4.dp))
                                Text(selectedTripName, style = MaterialTheme.typography.labelSmall, color = SkyBlue40, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Chọn ảnh
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (showPreview && !sheetIsUploading) Color.Black
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { imagePickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (sheetIsUploading) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = SkyBlue40, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Đang tải ảnh...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (showPreview) {
                        AsyncImage(model = sheetPreviewModel, contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop)
                        Surface(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                            shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = 0.6f)) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Edit, null, Modifier.size(12.dp), tint = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Text("Đổi ảnh", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.AddPhotoAlternate, null, Modifier.size(44.dp), tint = SkyBlue40)
                            Spacer(Modifier.height(6.dp))
                            Text("Chọn ảnh", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (sheetUploadError.isNotBlank()) {
                    Text(sheetUploadError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(value = sheetCaption, onValueChange = { sheetCaption = it },
                    label = { Text("Ghi lại kỷ niệm...") },
                    placeholder = { Text("Hôm nay thật tuyệt vời khi...") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6,
                    shape = RoundedCornerShape(14.dp))

                OutlinedTextField(value = sheetLocation, onValueChange = { sheetLocation = it },
                    label = { Text("Địa điểm") },
                    leadingIcon = { Icon(Icons.Filled.LocationOn, null, tint = Peach40) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(14.dp))

                ExposedDropdownMenuBox(expanded = sheetExpandedMood, onExpandedChange = { sheetExpandedMood = it }) {
                    OutlinedTextField(
                        value = sheetMood.ifBlank { "Cảm xúc lúc này..." },
                        onValueChange = {}, readOnly = true,
                        label = { Text("Cảm xúc") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sheetExpandedMood) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(14.dp))
                    ExposedDropdownMenu(expanded = sheetExpandedMood, onDismissRequest = { sheetExpandedMood = false }) {
                        MOODS.forEach { m ->
                            DropdownMenuItem(text = { Text(m) }, onClick = { sheetMood = m; sheetExpandedMood = false })
                        }
                    }
                }

                // Nút lưu
                Box(
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (canPost) Brush.horizontalGradient(listOf(GradSkyStart, GradSkyEnd))
                            else Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading || sheetIsUploading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(
                            onClick = {
                                currentUser?.let { user ->
                                    postViewModel.createPost(
                                        userId = user.userId,
                                        authorName = user.fullName,
                                        authorAvatar = user.avatarUrl,
                                        imageUrl = sheetImageUrl,
                                        caption = sheetCaption,
                                        location = sheetLocation,
                                        tripId = selectedTripId,
                                        tripName = selectedTripName,
                                        mood = sheetMood
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            enabled = canPost
                        ) {
                            Icon(Icons.Filled.BookmarkAdd, null, Modifier.size(18.dp), tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Lưu kỷ niệm", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── Filter chip chuyến đi ─────────────────────────────────────────────────────
@Composable
private fun TripFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) SkyBlue40 else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Header tháng ─────────────────────────────────────────────────────────────
@Composable
private fun MonthHeader(monthYear: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(SkyBlue40)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "📅 Tháng $monthYear",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = SkyBlue40
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = SkyBlue40.copy(alpha = 0.12f)
        ) {
            Text(
                "$count kỷ niệm",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = SkyBlue40
            )
        }
        Spacer(Modifier.weight(1f))
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// ── Card kỷ niệm ─────────────────────────────────────────────────────────────
@Composable
private fun MemoryCard(
    post: TravelPost,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // ── Ảnh ──────────────────────────────────────────────────────────
            if (post.imageUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                                    startY = 300f
                                )
                            )
                    )
                    // Mood badge
                    if (post.mood.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp),
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.4f)
                        ) {
                            Text(
                                post.mood,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                    // Date overlay bottom-left
                    Text(
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(post.createdAt)),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                    // Menu button
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, null,
                                tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Edit, null, Modifier.size(16.dp), tint = SkyBlue40)
                                        Spacer(Modifier.width(8.dp)); Text("Chỉnh sửa")
                                    }
                                },
                                onClick = { showMenu = false; onEdit() }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Delete, null, Modifier.size(16.dp), tint = Peach40)
                                        Spacer(Modifier.width(8.dp)); Text("Xóa", color = Peach40)
                                    }
                                },
                                onClick = { showMenu = false; showDeleteDialog = true }
                            )
                        }
                    }
                }
            }

            // ── Nội dung ─────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(14.dp)) {
                // Trip tag
                if (post.tripName.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FlightTakeoff, null,
                            Modifier.size(13.dp), tint = SkyBlue40)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            post.tripName,
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyBlue40,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // Caption
                if (post.caption.isNotBlank()) {
                    Text(
                        post.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Location + date (nếu không có ảnh)
                if (post.imageUrl.isBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (post.location.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, null,
                                    Modifier.size(13.dp), tint = Peach40)
                                Text(post.location,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Peach40)
                            }
                        }
                        Text(
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(post.createdAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (post.location.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null,
                            Modifier.size(13.dp), tint = Peach40)
                        Text(post.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = Peach40)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa kỷ niệm") },
            text = { Text("Bạn có chắc muốn xóa kỷ niệm này không?") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Peach40)
                ) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy") }
            }
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyTimelineHint(hasTrips: Boolean, onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("📖", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Nhật ký của bạn đang trống",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (hasTrips) "Hãy lưu lại những khoảnh khắc đáng nhớ trong chuyến đi của bạn!"
                else "Tạo chuyến đi trước, rồi lưu lại những kỷ niệm đẹp nhé!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = SkyBlue40)
            ) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Thêm kỷ niệm đầu tiên")
            }
        }
    }
}

// ── Dialog chỉnh sửa ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPostDialog(
    post: TravelPost,
    trips: List<Trip>,
    onSave: (caption: String, location: String, imageUrl: String, mood: String) -> Unit,
    onDismiss: () -> Unit
) {
    var caption by remember { mutableStateOf(post.caption) }
    var location by remember { mutableStateOf(post.location) }
    var imageUrl by remember { mutableStateOf(post.imageUrl) }
    var mood by remember { mutableStateOf(post.mood) }
    var expandedMood by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa kỷ niệm ✏️") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (imageUrl.trim().startsWith("http")) {
                    AsyncImage(
                        model = imageUrl.trim(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                OutlinedTextField(
                    value = imageUrl, onValueChange = { imageUrl = it },
                    label = { Text("Link ảnh (URL)") },
                    leadingIcon = { Icon(Icons.Filled.Image, null, tint = SkyBlue40) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                OutlinedTextField(
                    value = caption, onValueChange = { caption = it },
                    label = { Text("Ghi chú / Cảm xúc") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = location, onValueChange = { location = it },
                    label = { Text("Địa điểm") },
                    leadingIcon = { Icon(Icons.Filled.LocationOn, null, tint = Peach40) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                // Mood picker
                ExposedDropdownMenuBox(expanded = expandedMood, onExpandedChange = { expandedMood = it }) {
                    OutlinedTextField(
                        value = mood.ifBlank { "Chọn cảm xúc..." },
                        onValueChange = {}, readOnly = true,
                        label = { Text("Cảm xúc") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMood) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = expandedMood, onDismissRequest = { expandedMood = false }) {
                        MOODS.forEach { m ->
                            DropdownMenuItem(text = { Text(m) }, onClick = { mood = m; expandedMood = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(caption, location, imageUrl, mood) },
                colors = ButtonDefaults.buttonColors(containerColor = SkyBlue40)
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}
