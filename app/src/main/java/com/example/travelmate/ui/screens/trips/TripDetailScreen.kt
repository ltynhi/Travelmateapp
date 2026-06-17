package com.example.travelmate.ui.screens.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelmate.data.model.Place
import com.example.travelmate.data.model.TripPlaceWithDetail
import com.example.travelmate.ui.theme.SkyBlue40
import com.example.travelmate.viewmodel.AuthViewModel
import com.example.travelmate.viewmodel.PlaceViewModel
import com.example.travelmate.viewmodel.TripViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: String,
    tripViewModel: TripViewModel,
    placeViewModel: PlaceViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onPlaceClick: (String) -> Unit,
    onInviteMembers: () -> Unit = {},
    onOpenChat: () -> Unit = {}
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val selectedTrip by tripViewModel.selectedTrip.collectAsState()
    val tripPlacesWithDetail by tripViewModel.tripPlacesWithDetail.collectAsState()
    val trips by tripViewModel.trips.collectAsState()
    val allPlaces by placeViewModel.places.collectAsState()
    val isPlacesLoading by tripViewModel.isPlacesLoading.collectAsState()
    val successMessage by tripViewModel.successMessage.collectAsState()

    // Dialog states
    var showEditTripDialog by remember { mutableStateOf(false) }
    var showAddPlaceSheet by remember { mutableStateOf(false) }
    var showEditPlaceDialog by remember { mutableStateOf<TripPlaceWithDetail?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<TripPlaceWithDetail?>(null) }

    // Edit trip fields
    var editTripName by remember { mutableStateOf("") }
    var editStartDate by remember { mutableStateOf("") }
    var editEndDate by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    // Load trip khi vào màn hình
    LaunchedEffect(tripId) {
        // Reset để tránh hiện trip cũ trong khi đang load
        if (selectedTrip?.tripId != tripId) {
            tripViewModel.clearSelectedTrip()
        }
        val localTrip = trips.find { it.tripId == tripId }
        when {
            localTrip != null -> tripViewModel.selectTrip(localTrip)
            else -> tripViewModel.loadTripById(tripId)
        }
        placeViewModel.loadPlaces()
    }

    // Khi trips list thay đổi, kiểm tra lại
    LaunchedEffect(trips) {
        val localTrip = trips.find { it.tripId == tripId }
        if (localTrip != null && selectedTrip?.tripId != tripId) {
            tripViewModel.selectTrip(localTrip)
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            tripViewModel.clearMessages()
        }
    }

    val trip = selectedTrip

    // Nhóm địa điểm theo ngày
    val groupedByDate = tripPlacesWithDetail.groupBy { it.tripPlace.visitDate.ifBlank { "Chưa xếp ngày" } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip?.tripName ?: "Chi tiết chuyến đi", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Nút Chat
                    IconButton(onClick = onOpenChat) {
                        Icon(Icons.Filled.Chat, contentDescription = "Chat",
                            tint = SkyBlue40)
                    }
                    IconButton(onClick = onInviteMembers) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Mời thành viên",
                            tint = SkyBlue40)
                    }
                    IconButton(onClick = {
                        trip?.let {
                            editTripName = it.tripName
                            editStartDate = it.startDate
                            editEndDate = it.endDate
                            showEditTripDialog = true
                        }
                    }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Chỉnh sửa")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPlaceSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Thêm địa điểm", tint = Color.White)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (trip == null) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // ── Header card ──────────────────────────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                trip.tripName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CalendarToday, null,
                                    Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.width(6.dp))
                                Text("${trip.startDate} → ${trip.endDate}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, null,
                                    Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.width(6.dp))
                                Text("${tripPlacesWithDetail.size} địa điểm",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // ── Nút mời bạn bè + Chat ────────────────────────
                            val isOwner = trip.userId == currentUser?.userId
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Nút Chat
                                OutlinedButton(
                                    onClick = onOpenChat,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = SkyBlue40
                                    )
                                ) {
                                    Icon(Icons.Filled.Chat, null,
                                        modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Chat nhóm", fontWeight = FontWeight.SemiBold)
                                }

                                if (isOwner) {
                                    Button(
                                        onClick = onInviteMembers,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SkyBlue40,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(Icons.Filled.PersonAdd, null,
                                            modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            if (trip.memberIds.isEmpty()) "Mời bạn bè"
                                            else "Thành viên (${trip.memberIds.size + 1})",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = SkyBlue40.copy(alpha = 0.15f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Filled.Group, null,
                                                Modifier.size(14.dp), tint = SkyBlue40)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Thành viên",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SkyBlue40,
                                                fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Nội dung ─────────────────────────────────────────────────
                if (isPlacesLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (tripPlacesWithDetail.isEmpty()) {
                    item { EmptyPlacesHint(onAdd = { showAddPlaceSheet = true }) }
                } else {
                    // Hiển thị theo nhóm ngày
                    groupedByDate.forEach { (date, items) ->
                        item {
                            DateGroupHeader(date = date)
                        }
                        items(items) { item ->
                            TripPlaceItem(
                                item = item,
                                onEdit = { showEditPlaceDialog = item },
                                onDelete = { showDeleteConfirm = item },
                                onPlaceClick = { onPlaceClick(item.place.placeId) }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Bottom sheet chọn địa điểm ───────────────────────────────────────────
    if (showAddPlaceSheet) {
        AddPlaceBottomSheet(
            allPlaces = allPlaces,
            existingPlaceIds = tripPlacesWithDetail.map { it.place.placeId }.toSet(),
            tripStartDate = trip?.startDate ?: "",
            tripEndDate = trip?.endDate ?: "",
            onAdd = { placeId, visitDate, visitTime, note ->
                currentUser?.let { user ->
                    tripViewModel.addPlaceToTrip(
                        tripId = tripId,
                        placeId = placeId,
                        userId = user.userId,
                        visitDate = visitDate,
                        visitTime = visitTime,
                        note = note
                    )
                }
                showAddPlaceSheet = false
            },
            onDismiss = { showAddPlaceSheet = false }
        )
    }

    // ── Dialog chỉnh sửa ngày/giờ/ghi chú địa điểm ──────────────────────────
    showEditPlaceDialog?.let { item ->
        EditTripPlaceDialog(
            item = item,
            onSave = { updatedTripPlace ->
                tripViewModel.updateTripPlace(updatedTripPlace, tripId)
                showEditPlaceDialog = null
            },
            onDismiss = { showEditPlaceDialog = null }
        )
    }

    // ── Dialog xác nhận xóa ──────────────────────────────────────────────────
    showDeleteConfirm?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Xóa địa điểm") },
            text = { Text("Xóa \"${item.place.name}\" khỏi chuyến đi?") },
            confirmButton = {
                Button(
                    onClick = {
                        tripViewModel.removePlaceFromTrip(item.tripPlace.tripPlaceId, tripId)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Hủy") }
            }
        )
    }

    // ── Dialog chỉnh sửa chuyến đi ───────────────────────────────────────────
    if (showEditTripDialog) {
        AlertDialog(
            onDismissRequest = { showEditTripDialog = false },
            title = { Text("Chỉnh sửa chuyến đi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editTripName, onValueChange = { editTripName = it },
                        label = { Text("Tên chuyến đi") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = editStartDate, onValueChange = { editStartDate = it },
                        label = { Text("Ngày bắt đầu (dd/MM/yyyy)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = editEndDate, onValueChange = { editEndDate = it },
                        label = { Text("Ngày kết thúc (dd/MM/yyyy)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    trip?.let { t ->
                        currentUser?.let { user ->
                            tripViewModel.updateTrip(
                                t.copy(tripName = editTripName, startDate = editStartDate, endDate = editEndDate),
                                user.userId
                            )
                        }
                    }
                    showEditTripDialog = false
                }) { Text("Lưu") }
            },
            dismissButton = {
                TextButton(onClick = { showEditTripDialog = false }) { Text("Hủy") }
            }
        )
    }
}

// ── Header nhóm ngày ─────────────────────────────────────────────────────────
@Composable
private fun DateGroupHeader(date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Text(
                text = if (date == "Chưa xếp ngày") "📋 Chưa xếp ngày" else "📅 $date",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(8.dp))
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

// ── Item địa điểm trong timeline ─────────────────────────────────────────────
@Composable
private fun TripPlaceItem(
    item: TripPlaceWithDetail,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPlaceClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Timeline line + dot
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(80.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )
        }

        Spacer(Modifier.width(12.dp))

        // Card nội dung
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp)
                .clickable { onPlaceClick() },
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                // Ảnh địa điểm
                AsyncImage(
                    model = item.place.imageUrl.ifBlank { "https://via.placeholder.com/60" },
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.place.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Giờ
                    if (item.tripPlace.visitTime.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Schedule, null,
                                Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text(item.tripPlace.visitTime,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                    // Ghi chú
                    if (item.tripPlace.note.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Notes, null,
                                Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text(item.tripPlace.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    // Địa chỉ
                    Text(
                        item.place.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                // Actions
                Column {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, null,
                            Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, null,
                            Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// ── Gợi ý khi chưa có địa điểm ───────────────────────────────────────────────
@Composable
private fun EmptyPlacesHint(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🗺️", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("Chưa có địa điểm nào",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium)
        Text("Nhấn + để thêm địa điểm vào chuyến đi",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAdd) {
            Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Thêm địa điểm")
        }
    }
}

// ── Bottom sheet chọn địa điểm ───────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPlaceBottomSheet(
    allPlaces: List<Place>,
    existingPlaceIds: Set<String>,
    tripStartDate: String,
    tripEndDate: String,
    onAdd: (placeId: String, visitDate: String, visitTime: String, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPlace by remember { mutableStateOf<Place?>(null) }
    var visitDate by remember { mutableStateOf(tripStartDate) }
    var visitTime by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val filteredPlaces = allPlaces.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.address.contains(searchQuery, ignoreCase = true)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Thêm địa điểm vào chuyến đi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            if (selectedPlace == null) {
                // ── Bước 1: Chọn địa điểm ────────────────────────────────────
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm kiếm địa điểm...") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    filteredPlaces.forEach { place ->
                        val alreadyAdded = existingPlaceIds.contains(place.placeId)
                        PlacePickerItem(
                            place = place,
                            alreadyAdded = alreadyAdded,
                            onClick = {
                                if (!alreadyAdded) selectedPlace = place
                            }
                        )
                    }
                }
            } else {
                // ── Bước 2: Đặt ngày/giờ/ghi chú ────────────────────────────
                // Preview địa điểm đã chọn
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = selectedPlace!!.imageUrl.ifBlank { "https://via.placeholder.com/48" },
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(selectedPlace!!.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall)
                            Text(selectedPlace!!.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        TextButton(onClick = { selectedPlace = null }) { Text("Đổi") }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = visitDate,
                    onValueChange = { visitDate = it },
                    label = { Text("Ngày tham quan") },
                    placeholder = { Text("dd/MM/yyyy") },
                    leadingIcon = { Icon(Icons.Filled.CalendarToday, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("Để trống nếu chưa xác định") }
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = visitTime,
                    onValueChange = { visitTime = it },
                    label = { Text("Giờ tham quan") },
                    placeholder = { Text("HH:mm  (vd: 09:00)") },
                    leadingIcon = { Icon(Icons.Filled.Schedule, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("Để trống nếu chưa xác định") }
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    placeholder = { Text("Vd: Đặt vé trước, mang kem chống nắng...") },
                    leadingIcon = { Icon(Icons.Filled.Notes, null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        onAdd(selectedPlace!!.placeId, visitDate.trim(), visitTime.trim(), note.trim())
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Filled.Add, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Thêm vào chuyến đi", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Item chọn địa điểm ───────────────────────────────────────────────────────
@Composable
private fun PlacePickerItem(
    place: Place,
    alreadyAdded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !alreadyAdded) { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = place.imageUrl.ifBlank { "https://via.placeholder.com/48" },
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                place.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = if (alreadyAdded) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                place.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (alreadyAdded) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    "Đã thêm",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
    HorizontalDivider()
}

// ── Dialog chỉnh sửa ngày/giờ/ghi chú ───────────────────────────────────────
@Composable
private fun EditTripPlaceDialog(
    item: TripPlaceWithDetail,
    onSave: (com.example.travelmate.data.model.TripPlace) -> Unit,
    onDismiss: () -> Unit
) {
    var visitDate by remember { mutableStateOf(item.tripPlace.visitDate) }
    var visitTime by remember { mutableStateOf(item.tripPlace.visitTime) }
    var note by remember { mutableStateOf(item.tripPlace.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa: ${item.place.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = visitDate,
                    onValueChange = { visitDate = it },
                    label = { Text("Ngày tham quan") },
                    placeholder = { Text("dd/MM/yyyy") },
                    leadingIcon = { Icon(Icons.Filled.CalendarToday, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = visitTime,
                    onValueChange = { visitTime = it },
                    label = { Text("Giờ tham quan") },
                    placeholder = { Text("HH:mm") },
                    leadingIcon = { Icon(Icons.Filled.Schedule, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    leadingIcon = { Icon(Icons.Filled.Notes, null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(item.tripPlace.copy(
                    visitDate = visitDate.trim(),
                    visitTime = visitTime.trim(),
                    note = note.trim()
                ))
            }) { Text("Lưu") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
