package com.example.travelmate.ui.screens.timeline

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelmate.ui.theme.*
import com.example.travelmate.utils.CloudinaryUploader
import com.example.travelmate.viewmodel.AuthViewModel
import com.example.travelmate.viewmodel.TravelPostViewModel
import com.example.travelmate.viewmodel.TripViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    authViewModel: AuthViewModel,
    postViewModel: TravelPostViewModel,
    tripViewModel: TripViewModel,
    preselectedTripId: String = "",
    preselectedTripName: String = "",
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by authViewModel.currentUser.collectAsState()
    val trips by tripViewModel.trips.collectAsState()
    val isLoading by postViewModel.isLoading.collectAsState()
    val successMessage by postViewModel.successMessage.collectAsState()
    val error by postViewModel.error.collectAsState()

    var imageUrl by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf("") }

    var caption by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf("") }
    // Dùng trực tiếp giá trị được truyền vào — không cần load trips để resolve
    var selectedTripId by remember { mutableStateOf(preselectedTripId) }
    var selectedTripName by remember { mutableStateOf(preselectedTripName) }
    var expandedMood by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        currentUser?.let { tripViewModel.loadTrips(it.userId) }
    }
    // Fallback: nếu tripName rỗng nhưng tripId có, resolve từ trips khi load xong
    LaunchedEffect(trips, preselectedTripId) {
        if (preselectedTripId.isNotBlank() && selectedTripName.isBlank()) {
            trips.find { it.tripId == preselectedTripId }?.let {
                selectedTripName = it.tripName
            }
        }
    }

    // Picker ảnh từ điện thoại
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imageUrl = "" // reset URL khi chọn ảnh mới
            uploadError = ""
            // Upload lên Cloudinary ngay
            scope.launch {
                isUploading = true
                val result = CloudinaryUploader.uploadImage(context, it, "timeline")
                result.fold(
                    onSuccess = { url ->
                        imageUrl = url
                        uploadError = ""
                    },
                    onFailure = { e ->
                        uploadError = "Upload thất bại: ${e.message}"
                        selectedImageUri = null
                    }
                )
                isUploading = false
            }
        }
    }

    val showPreview = selectedImageUri != null || imageUrl.trim().startsWith("http")
    val previewModel: Any = selectedImageUri ?: imageUrl.trim()

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            postViewModel.clearMessages()
            onSuccess()
        }
    }

    val canPost = !isLoading && !isUploading && (caption.isNotBlank() || imageUrl.isNotBlank())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thêm kỷ niệm mới", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            currentUser?.let { user ->
                                postViewModel.createPost(
                                    userId = user.userId,
                                    authorName = user.fullName,
                                    authorAvatar = user.avatarUrl,
                                    imageUrl = imageUrl,
                                    caption = caption,
                                    location = location,
                                    tripId = selectedTripId,
                                    tripName = selectedTripName,
                                    mood = selectedMood
                                )
                            }
                        },
                        enabled = canPost
                    ) {
                        if (isLoading || isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SkyBlue40)
                        } else {
                            Text("Lưu", fontWeight = FontWeight.Bold, color = SkyBlue40, fontSize = 16.sp)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Chọn ảnh ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (showPreview && !isUploading)
                            Modifier.background(Color.Black)
                        else Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(SkyBlue90, MaterialTheme.colorScheme.surfaceVariant)
                            )
                        )
                    )
                    .border(
                        width = if (showPreview) 0.dp else 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { imagePickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SkyBlue40, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Đang tải ảnh lên...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (showPreview) {
                    AsyncImage(
                        model = previewModel,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                    // Overlay nút đổi ảnh
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp),
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Edit, null,
                                modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("Đổi ảnh", style = MaterialTheme.typography.labelSmall,
                                color = Color.White)
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.AddPhotoAlternate, null,
                            modifier = Modifier.size(52.dp),
                            tint = SkyBlue40)
                        Spacer(Modifier.height(8.dp))
                        Text("Chọn ảnh từ điện thoại",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Nhấn để chọn ảnh",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }

            if (uploadError.isNotBlank()) {
                Text(uploadError, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            // ── Caption ──────────────────────────────────────────────────────
            OutlinedTextField(
                value = caption, onValueChange = { caption = it },
                label = { Text("Ghi lại kỷ niệm...") },
                placeholder = { Text("Hôm nay thật tuyệt vời khi...") },
                modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 8,
                shape = RoundedCornerShape(14.dp)
            )

            // ── Địa điểm ─────────────────────────────────────────────────────
            OutlinedTextField(
                value = location, onValueChange = { location = it },
                label = { Text("Địa điểm") },
                placeholder = { Text("Vd: Vịnh Hạ Long, Quảng Ninh") },
                leadingIcon = { Icon(Icons.Filled.LocationOn, null, tint = Peach40) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // ── Cảm xúc ──────────────────────────────────────────────────────
            ExposedDropdownMenuBox(expanded = expandedMood, onExpandedChange = { expandedMood = it }) {
                OutlinedTextField(
                    value = selectedMood.ifBlank { "Cảm xúc lúc này..." },
                    onValueChange = {}, readOnly = true,
                    label = { Text("Cảm xúc") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMood) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(14.dp)
                )
                ExposedDropdownMenu(expanded = expandedMood, onDismissRequest = { expandedMood = false }) {
                    MOODS.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m, fontSize = 15.sp) },
                            onClick = { selectedMood = m; expandedMood = false }
                        )
                    }
                }
            }

            if (error != null) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            // ── Nút lưu ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (canPost)
                            Brush.horizontalGradient(colors = listOf(GradSkyStart, GradSkyEnd))
                        else Brush.horizontalGradient(
                            colors = listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading || isUploading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(
                        onClick = {
                            currentUser?.let { user ->
                                postViewModel.createPost(
                                    userId = user.userId,
                                    authorName = user.fullName,
                                    authorAvatar = user.avatarUrl,
                                    imageUrl = imageUrl,
                                    caption = caption,
                                    location = location,
                                    tripId = selectedTripId,
                                    tripName = selectedTripName,
                                    mood = selectedMood
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
