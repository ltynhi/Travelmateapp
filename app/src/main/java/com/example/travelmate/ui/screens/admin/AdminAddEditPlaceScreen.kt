package com.example.travelmate.ui.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelmate.data.model.Place
import com.example.travelmate.ui.screens.home.ALL_CATEGORIES
import com.example.travelmate.ui.theme.Mint40
import com.example.travelmate.ui.theme.Peach40
import com.example.travelmate.ui.theme.SkyBlue40
import com.example.travelmate.utils.CloudinaryUploader
import com.example.travelmate.viewmodel.PlaceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddEditPlaceScreen(
    placeId: String?,
    placeViewModel: PlaceViewModel,
    onBack: () -> Unit
) {
    val isEdit = placeId != null
    val isLoading by placeViewModel.isLoading.collectAsState()
    val places by placeViewModel.places.collectAsState()
    val existingPlace = remember(placeId, places) {
        if (isEdit) places.find { it.placeId == placeId } else null
    }

    var name by remember(placeId) { mutableStateOf("") }
    var description by remember(placeId) { mutableStateOf("") }
    var address by remember(placeId) { mutableStateOf("") }
    var city by remember(placeId) { mutableStateOf("") }
    var category by remember(placeId) { mutableStateOf(ALL_CATEGORIES.first()) }
    // Gallery ảnh — list URL
    var imageList by remember(placeId) { mutableStateOf<List<String>>(emptyList()) }
    var expandedCategory by remember { mutableStateOf(false) }
    var initialized by remember(placeId) { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadingIndex by remember { mutableStateOf(-1) } // -1 = thêm mới

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Picker ảnh — dùng Photo Picker mở thẳng thư viện ảnh (Android 13+)
    // Fallback GetMultipleContents cho Android cũ hơn
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            isUploading = true
            uris.forEach { uri ->
                val result = CloudinaryUploader.uploadImage(context, uri, "places")
                result.fold(
                    onSuccess = { url -> imageList = imageList + url },
                    onFailure = {}
                )
            }
            isUploading = false
        }
    }

    LaunchedEffect(existingPlace) {
        if (!initialized && existingPlace != null) {
            name = existingPlace.name
            description = existingPlace.description
            address = existingPlace.address
            city = existingPlace.city
            category = existingPlace.category.ifBlank { ALL_CATEGORIES.first() }
            imageList = existingPlace.getAllImages()
            initialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Chỉnh sửa địa điểm" else "Thêm địa điểm mới") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
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
            // ── Gallery ảnh ───────────────────────────────────────────────────
            Text("Ảnh địa điểm", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Các ảnh đã thêm
                itemsIndexed(imageList) { index, url ->
                    Box(modifier = Modifier.size(110.dp)) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        // Badge ảnh đại diện (ảnh đầu tiên)
                        if (index == 0) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp),
                                shape = RoundedCornerShape(6.dp),
                                color = SkyBlue40
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Star, null,
                                        modifier = Modifier.size(10.dp), tint = Color.White)
                                    Spacer(Modifier.width(2.dp))
                                    Text("Chính", fontSize = 9.sp, color = Color.White,
                                        fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        // Nút xóa ảnh
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable {
                                    imageList = imageList.toMutableList().also { it.removeAt(index) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Close, null,
                                modifier = Modifier.size(14.dp), tint = Color.White)
                        }
                    }
                }

                // Nút thêm ảnh
                item {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                1.dp,
                                if (isUploading) SkyBlue40 else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = !isUploading) {
                                imagePickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = SkyBlue40,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.AddPhotoAlternate, null,
                                    modifier = Modifier.size(28.dp), tint = SkyBlue40)
                                Spacer(Modifier.height(4.dp))
                                Text("Thêm ảnh",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyBlue40)
                            }
                        }
                    }
                }
            }

            // Gợi ý
            Text(
                "Ảnh đầu tiên sẽ là ảnh đại diện. Có thể chọn nhiều ảnh cùng lúc.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Tên địa điểm ─────────────────────────────────────────────────
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Tên địa điểm *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // ── Thành phố ────────────────────────────────────────────────────
            OutlinedTextField(
                value = city, onValueChange = { city = it },
                label = { Text("Thành phố / Tỉnh *") },
                placeholder = { Text("Vd: Đà Nẵng, Hà Nội...") },
                leadingIcon = { Icon(Icons.Filled.LocationOn, null, tint = Mint40) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // ── Địa chỉ ──────────────────────────────────────────────────────
            OutlinedTextField(
                value = address, onValueChange = { address = it },
                label = { Text("Địa chỉ cụ thể") },
                leadingIcon = { Icon(Icons.Filled.LocationOn, null) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // ── Danh mục ─────────────────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = it }
            ) {
                OutlinedTextField(
                    value = category, onValueChange = {}, readOnly = true,
                    label = { Text("Danh mục *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    modifier = Modifier.fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    ALL_CATEGORIES.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = { category = cat; expandedCategory = false }
                        )
                    }
                }
            }

            // ── Mô tả ────────────────────────────────────────────────────────
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Mô tả") },
                modifier = Modifier.fillMaxWidth(), minLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            // ── Nút lưu ──────────────────────────────────────────────────────
            Button(
                onClick = {
                    if (name.isNotBlank() && city.isNotBlank()) {
                        val mainImage = imageList.firstOrNull() ?: ""
                        val extraImages = if (imageList.size > 1) imageList.drop(1) else emptyList()
                        if (isEdit && existingPlace != null) {
                            placeViewModel.updatePlace(
                                existingPlace.copy(
                                    name = name.trim(),
                                    description = description.trim(),
                                    address = address.trim(),
                                    city = city.trim(),
                                    category = category,
                                    imageUrl = mainImage,
                                    images = imageList
                                ),
                                onSuccess = onBack
                            )
                        } else {
                            placeViewModel.addPlace(
                                Place(
                                    name = name.trim(),
                                    description = description.trim(),
                                    address = address.trim(),
                                    city = city.trim(),
                                    category = category,
                                    imageUrl = mainImage,
                                    images = imageList
                                ),
                                onSuccess = onBack
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isLoading && !isUploading && name.isNotBlank() && city.isNotBlank(),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isEdit) "Cập nhật" else "Thêm địa điểm", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
