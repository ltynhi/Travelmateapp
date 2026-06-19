package com.example.travelmate.ui.screens.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Air
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelmate.data.model.Place
import com.example.travelmate.data.model.WeatherInfo
import com.example.travelmate.data.model.TripPlaceWithDetail
import com.example.travelmate.ui.theme.SkyBlue40
import com.example.travelmate.viewmodel.AuthViewModel
import com.example.travelmate.viewmodel.PlaceViewModel
import com.example.travelmate.viewmodel.TripViewModel
import com.example.travelmate.viewmodel.WeatherViewModel
import com.example.travelmate.viewmodel.WeatherState
import com.example.travelmate.viewmodel.SuggestItineraryState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: String,
    tripViewModel: TripViewModel,
    placeViewModel: PlaceViewModel,
    authViewModel: AuthViewModel,
    weatherViewModel: WeatherViewModel,
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
    val weatherState by weatherViewModel.weatherState.collectAsState()

    // Dialog states
    var showEditTripDialog by remember { mutableStateOf(false) }
    var showAddPlaceSheet by remember { mutableStateOf(false) }
    var showEditPlaceDialog by remember { mutableStateOf<TripPlaceWithDetail?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<TripPlaceWithDetail?>(null) }
    var showAutoScheduleConfirm by remember { mutableStateOf(false) }
    var showAutoScheduleResult by remember { mutableStateOf(false) }
    var showSuggestDialog by remember { mutableStateOf(false) }

    val autoScheduleResult by tripViewModel.autoScheduleResult.collectAsState()
    val suggestItineraryState by tripViewModel.suggestItineraryState.collectAsState()

    // Edit trip fields
    var editTripName by remember { mutableStateOf("") }
    var editStartDate by remember { mutableStateOf("") }
    var editEndDate by remember { mutableStateOf("") }
    var editDestination by remember { mutableStateOf("") }

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

    // Khi có kết quả auto-schedule → hiện dialog tóm tắt
    LaunchedEffect(autoScheduleResult) {
        if (autoScheduleResult != null) showAutoScheduleResult = true
    }

    // Khi suggest itinerary sẵn → hiện dialog preview
    LaunchedEffect(suggestItineraryState) {
        if (suggestItineraryState is SuggestItineraryState.Ready) showSuggestDialog = true
    }

    // Reset weather khi đổi trip
    LaunchedEffect(tripId) {
        weatherViewModel.reset()
    }

    // Khi trip load xong → lấy thành phố → gọi API thời tiết
    LaunchedEffect(selectedTrip, tripPlacesWithDetail) {
        val t = selectedTrip ?: return@LaunchedEffect
        if (t.startDate.isBlank() || t.endDate.isBlank()) return@LaunchedEffect
        // Ưu tiên destination của trip, fallback sang city của địa điểm đầu tiên
        val city = when {
            t.destination.isNotBlank() -> t.destination
            tripPlacesWithDetail.isNotEmpty() ->
                tripPlacesWithDetail.firstOrNull()?.place?.city ?: return@LaunchedEffect
            else -> return@LaunchedEffect
        }
        weatherViewModel.loadWeather(city, t.startDate, t.endDate)
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
                            editDestination = it.destination
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
                            if (trip.destination.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📍", fontSize = 14.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(trip.destination,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, null,
                                    Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.width(6.dp))
                                Text("${tripPlacesWithDetail.size} địa điểm",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }

                            // ── Tổng chi phí trip ────────────────────────────
                            val totalCost = tripPlacesWithDetail
                                .sumOf { it.tripPlace.estimatedCost }
                            if (totalCost > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF2E7D32).copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("💰", fontSize = 14.sp)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Tổng chi phí ước tính: ${formatCostFull(totalCost)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
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

                            // ── Nút Tạo lịch trình gợi ý (khi chưa có địa điểm) ──
                            if (tripPlacesWithDetail.isEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        tripViewModel.generateItinerarySuggestion(
                                            trip = trip,
                                            allPlaces = allPlaces
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1565C0),
                                        contentColor = Color.White
                                    )
                                ) {
                                    when (suggestItineraryState) {
                                        is SuggestItineraryState.Loading -> {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text("Đang tạo gợi ý...", fontWeight = FontWeight.SemiBold)
                                        }
                                        else -> {
                                            Text("✨", fontSize = 16.sp)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Tạo lịch trình gợi ý cho tôi",
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                                // Hiện lỗi nếu có
                                if (suggestItineraryState is SuggestItineraryState.Error) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        (suggestItineraryState as SuggestItineraryState.Error).message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            // ── Nút Auto-schedule ─────────────────────────────
                            if (tripPlacesWithDetail.isNotEmpty()) {
                                val unscheduledCount = tripPlacesWithDetail
                                    .count { it.tripPlace.visitDate.isBlank() }
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { showAutoScheduleConfirm = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF5C6BC0),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, null,
                                        modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (unscheduledCount > 0)
                                            "Tự động xếp lịch ($unscheduledCount chưa có ngày)"
                                        else
                                            "Sắp xếp lại lịch trình",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Weather Card ─────────────────────────────────────────────
                item {
                    WeatherCard(
                        weatherState = weatherState,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
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
            estimateCostFn = { place -> tripViewModel.estimateCostForPlace(place) },
            onAdd = { placeId, visitDate, visitTime, note, estimatedCost ->
                currentUser?.let { user ->
                    tripViewModel.addPlaceToTrip(
                        tripId = tripId, placeId = placeId, userId = user.userId,
                        visitDate = visitDate, visitTime = visitTime,
                        note = note, estimatedCost = estimatedCost
                    )
                }
                showAddPlaceSheet = false
            },
            onAddCustom = { customName, customAddress, customCategory, customImageUrl,
                            visitDate, visitTime, note, estimatedCost ->
                tripViewModel.addCustomPlaceToTrip(
                    tripId = tripId,
                    customName = customName, customAddress = customAddress,
                    customCategory = customCategory, customImageUrl = customImageUrl,
                    visitDate = visitDate, visitTime = visitTime,
                    note = note, estimatedCost = estimatedCost
                )
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

    // ── Dialog preview lịch trình gợi ý ─────────────────────────────────────
    if (showSuggestDialog && suggestItineraryState is SuggestItineraryState.Ready) {
        val state = suggestItineraryState as SuggestItineraryState.Ready
        AlertDialog(
            onDismissRequest = {
                showSuggestDialog = false
                tripViewModel.clearSuggestState()
            },
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✨", fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Gợi ý lịch trình", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "📍 ${state.destination}  •  ${state.totalDays} ngày  •  ~${state.totalHours}h",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tổng chi phí
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF2E7D32).copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💰", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Tổng chi phí ước tính",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    formatCostFull(state.totalCost),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Danh sách địa điểm theo ngày
                    val grouped = state.items.groupBy { it.visitDate }
                    grouped.forEach { (date, items) ->
                        // Header ngày
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1565C0).copy(alpha = 0.1f)
                        ) {
                            Text(
                                "📅 $date",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                        }
                        items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = item.place.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        item.place.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "⏰ ${item.visitTime}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "💰 ${formatCost(item.estimatedCost)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF2E7D32)
                                        )
                                        Text(
                                            "⭐ ${item.place.rating}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        tripViewModel.confirmSuggestedItinerary(tripId, state.items)
                        showSuggestDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Text("✅ Thêm vào trip")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSuggestDialog = false
                    tripViewModel.clearSuggestState()
                }) { Text("Hủy") }
            }
        )
    }

    // ── Dialog kết quả auto-schedule (ý tưởng 2 + 4) ────────────────────────
    if (showAutoScheduleResult && autoScheduleResult != null) {
        val result = autoScheduleResult!!
        AlertDialog(
            onDismissRequest = {
                showAutoScheduleResult = false
                tripViewModel.clearAutoScheduleResult()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎉", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Xếp lịch hoàn tất!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // ── Tóm tắt lịch trình (ý tưởng 4) ─────────────────────
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF5C6BC0).copy(alpha = 0.1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "📊 Tóm tắt lịch trình",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5C6BC0)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "${result.scheduledCount}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF5C6BC0)
                                    )
                                    Text("địa điểm",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "${result.totalDays}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1976D2)
                                    )
                                    Text("ngày",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "~${result.estimatedHours}h",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Text("tham quan",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            // Chi phí ước tính
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("💰", fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Chi phí ước tính: ${formatCost(result.estimatedMinCost)} – ${formatCost(result.estimatedMaxCost)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // ── Gợi ý thêm địa điểm (ý tưởng 2) ────────────────────
                    if (result.suggestions.isNotEmpty()) {
                        Text(
                            "💡 Gợi ý thêm địa điểm",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        result.suggestions.forEach { suggestion ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1976D2).copy(alpha = 0.06f)
                                ),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = suggestion.place.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            suggestion.place.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "📅 Gợi ý cho ngày ${suggestion.date.take(5)}  •  ⭐ ${suggestion.place.rating}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            suggestion.place.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF1976D2)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAutoScheduleResult = false
                        tripViewModel.clearAutoScheduleResult()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0))
                ) { Text("Xem lịch trình") }
            }
        )
    }

    // ── Dialog xác nhận auto-schedule ───────────────────────────────────────
    if (showAutoScheduleConfirm) {
        val unscheduledCount = tripPlacesWithDetail.count { it.tripPlace.visitDate.isBlank() }
        val totalCount = tripPlacesWithDetail.size
        val isReschedule = unscheduledCount == 0
        AlertDialog(
            onDismissRequest = { showAutoScheduleConfirm = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✨", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isReschedule) "Sắp xếp lại lịch trình" else "Tự động xếp lịch")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isReschedule)
                            "App sẽ phân bổ lại toàn bộ $totalCount địa điểm vào các ngày trong chuyến đi."
                        else
                            "App sẽ tự động phân bổ $unscheduledCount địa điểm chưa có lịch vào các ngày trong chuyến đi.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF5C6BC0).copy(alpha = 0.1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("📅 Phân bổ đều theo số ngày",
                                style = MaterialTheme.typography.bodySmall)
                            Text("⏰ Tự động gán giờ gợi ý (8h, 10h, 13h...)",
                                style = MaterialTheme.typography.bodySmall)
                            Text("✏️ Bạn có thể chỉnh sửa lại sau",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (trip?.startDate.isNullOrBlank() || trip?.endDate.isNullOrBlank()) {
                        Text(
                            "⚠️ Chuyến đi chưa có ngày bắt đầu/kết thúc. Hãy chỉnh sửa trip trước.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        trip?.let { t ->
                            tripViewModel.autoSchedule(
                                tripId, t.startDate, t.endDate,
                                rescheduleAll = isReschedule,
                                allAvailablePlaces = allPlaces
                            )
                        }
                        showAutoScheduleConfirm = false
                    },
                    enabled = !trip?.startDate.isNullOrBlank() && !trip?.endDate.isNullOrBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0))
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isReschedule) "Sắp xếp lại" else "Xếp lịch ngay")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAutoScheduleConfirm = false }) { Text("Hủy") }
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
                        value = editDestination, onValueChange = { editDestination = it },
                        label = { Text("Điểm đến") },
                        placeholder = { Text("Vd: Đà Nẵng, Huế, Hà Nội...") },
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
                                t.copy(tripName = editTripName, startDate = editStartDate,
                                    endDate = editEndDate, destination = editDestination),
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

// ── Weather Card ─────────────────────────────────────────────────────────────
@Composable
fun WeatherCard(
    weatherState: WeatherState,
    modifier: Modifier = Modifier
) {
    when (weatherState) {
        is WeatherState.Loading -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1976D2).copy(alpha = 0.08f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = SkyBlue40
                    )
                    Text(
                        "Đang tải thời tiết...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        is WeatherState.Success -> {
            val weatherMap = weatherState.weatherMap
            if (weatherMap.isEmpty()) return

            Card(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1565C0).copy(alpha = 0.08f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, Color(0xFF1976D2).copy(alpha = 0.2f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Tiêu đề
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🌤️", fontSize = 16.sp)
                        Text(
                            "Dự báo thời tiết",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0)
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "OpenWeather",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Danh sách ngày — scroll ngang nếu nhiều ngày
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(weatherMap.entries.toList()) { (_, weather) ->
                            WeatherDayItem(weather = weather)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        is WeatherState.Error -> {
            // Hiển thị lỗi nhỏ, không che nội dung chính
            Card(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚠️", fontSize = 14.sp)
                    Text(
                        "Không tải được thời tiết",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        else -> Unit // Idle — không hiện gì
    }
}

@Composable
private fun WeatherDayItem(weather: WeatherInfo) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1976D2).copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .width(80.dp)
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Ngày (chỉ dd/MM)
            Text(
                text = weather.date.take(5), // "20/07"
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1565C0)
            )

            // Emoji thời tiết
            Text(weather.emoji, fontSize = 26.sp)

            // Nhiệt độ max / min
            Text(
                text = "${weather.tempMax}°",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFE65100)
            )
            Text(
                text = "${weather.tempMin}°",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF1976D2)
            )

            // Mô tả ngắn
            Text(
                text = weather.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )

            // Độ ẩm
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("💧", fontSize = 10.sp)
                Text(
                    "${weather.humidity}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
                    // Chi phí ước tính
                    if (item.tripPlace.estimatedCost > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💰", fontSize = 10.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                formatCostFull(item.tripPlace.estimatedCost),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Medium
                            )
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
    onAdd: (placeId: String, visitDate: String, visitTime: String, note: String, estimatedCost: Long) -> Unit,
    onAddCustom: (customName: String, customAddress: String, customCategory: String,
                  customImageUrl: String, visitDate: String, visitTime: String,
                  note: String, estimatedCost: Long) -> Unit,
    onDismiss: () -> Unit,
    estimateCostFn: (Place) -> Long = { 0L }
) {
    var selectedTab by remember { mutableStateOf(0) } // 0=Chọn từ ds, 1=Tự nhập

    // Tab 0 — chọn từ danh sách
    var searchQuery by remember { mutableStateOf("") }
    var selectedPlace by remember { mutableStateOf<Place?>(null) }
    var visitDate by remember { mutableStateOf(tripStartDate) }
    var visitTime by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var costInput by remember { mutableStateOf("") }

    // Tab 1 — tự nhập
    var customName by remember { mutableStateOf("") }
    var customAddress by remember { mutableStateOf("") }
    var customImageUrl by remember { mutableStateOf("") }
    var customDate by remember { mutableStateOf(tripStartDate) }
    var customTime by remember { mutableStateOf("") }
    var customNote by remember { mutableStateOf("") }
    var customCost by remember { mutableStateOf("") }

    LaunchedEffect(selectedPlace) {
        selectedPlace?.let { costInput = estimateCostFn(it).let { v -> if (v > 0) v.toString() else "" } }
    }

    val filteredPlaces = allPlaces.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.address.contains(searchQuery, ignoreCase = true)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)
        ) {
            Text("Thêm địa điểm vào chuyến đi",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            // ── Tab toggle ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("🗺️ Chọn từ danh sách", "✏️ Tự nhập").forEachIndexed { idx, label ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedTab == idx) SkyBlue40
                                else MaterialTheme.colorScheme.surfaceVariant,
                        onClick = { selectedTab = idx; selectedPlace = null }
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedTab == idx) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (selectedTab == 0) {
                // ── Tab 0: Chọn từ danh sách ───────────────────────────────
                if (selectedPlace == null) {
                    OutlinedTextField(
                        value = searchQuery, onValueChange = { searchQuery = it },
                        placeholder = { Text("Tìm kiếm địa điểm...") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        filteredPlaces.forEach { place ->
                            val alreadyAdded = existingPlaceIds.contains(place.placeId)
                            PlacePickerItem(place, alreadyAdded) {
                                if (!alreadyAdded) selectedPlace = place
                            }
                        }
                    }
                } else {
                    // Preview + form ngày/giờ/chi phí
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = selectedPlace!!.imageUrl.ifBlank { "https://via.placeholder.com/48" },
                                contentDescription = null,
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(selectedPlace!!.name, fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall)
                                Text(selectedPlace!!.address, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            TextButton(onClick = { selectedPlace = null; costInput = "" }) { Text("Đổi") }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    PlaceFormFields(
                        visitDate = visitDate, onDateChange = { visitDate = it },
                        visitTime = visitTime, onTimeChange = { visitTime = it },
                        costInput = costInput, onCostChange = { costInput = it.filter { c -> c.isDigit() } },
                        note = note, onNoteChange = { note = it },
                        costHint = selectedPlace?.let { estimateCostFn(it) } ?: 0L
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val cost = costInput.toLongOrNull() ?: estimateCostFn(selectedPlace!!)
                            onAdd(selectedPlace!!.placeId, visitDate.trim(), visitTime.trim(), note.trim(), cost)
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Add, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Thêm vào chuyến đi", fontWeight = FontWeight.Bold)
                    }
                }

            } else {
                // ── Tab 1: Tự nhập địa điểm ────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = customName, onValueChange = { customName = it },
                        label = { Text("Tên địa điểm *") },
                        placeholder = { Text("Vd: Quán hải sản Biển Xanh") },
                        leadingIcon = { Icon(Icons.Filled.LocationOn, null, tint = SkyBlue40) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = customAddress, onValueChange = { customAddress = it },
                        label = { Text("Địa chỉ") },
                        placeholder = { Text("Vd: 123 Trần Phú, Đà Nẵng") },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = customImageUrl, onValueChange = { customImageUrl = it },
                        label = { Text("Link ảnh (tuỳ chọn)") },
                        placeholder = { Text("https://...") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    PlaceFormFields(
                        visitDate = customDate, onDateChange = { customDate = it },
                        visitTime = customTime, onTimeChange = { customTime = it },
                        costInput = customCost, onCostChange = { customCost = it.filter { c -> c.isDigit() } },
                        note = customNote, onNoteChange = { customNote = it },
                        costHint = 0L
                    )

                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            onAddCustom(
                                customName.trim(), customAddress.trim(),
                                "Khác", customImageUrl.trim(),
                                customDate.trim(), customTime.trim(),
                                customNote.trim(), customCost.toLongOrNull() ?: 0L
                            )
                        },
                        enabled = customName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) {
                        Text("✏️", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Thêm địa điểm tự nhập", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Form fields tái sử dụng cho cả 2 tab ────────────────────────────────────
@Composable
private fun PlaceFormFields(
    visitDate: String, onDateChange: (String) -> Unit,
    visitTime: String, onTimeChange: (String) -> Unit,
    costInput: String, onCostChange: (String) -> Unit,
    note: String, onNoteChange: (String) -> Unit,
    costHint: Long
) {
    OutlinedTextField(
        value = visitDate, onValueChange = onDateChange,
        label = { Text("Ngày tham quan") },
        placeholder = { Text("dd/MM/yyyy") },
        leadingIcon = { Icon(Icons.Filled.CalendarToday, null) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        shape = RoundedCornerShape(14.dp),
        supportingText = { Text("Để trống nếu chưa xác định") }
    )
    OutlinedTextField(
        value = visitTime, onValueChange = onTimeChange,
        label = { Text("Giờ tham quan") },
        placeholder = { Text("HH:mm  (vd: 09:00)") },
        leadingIcon = { Icon(Icons.Filled.Schedule, null) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        shape = RoundedCornerShape(14.dp),
        supportingText = { Text("Để trống nếu chưa xác định") }
    )
    OutlinedTextField(
        value = costInput, onValueChange = onCostChange,
        label = { Text("Chi phí ước tính (VNĐ)") },
        placeholder = { Text(if (costHint > 0) "Gợi ý: ${formatCost(costHint)}" else "0") },
        leadingIcon = { Text("💰", modifier = Modifier.padding(start = 12.dp)) },
        trailingIcon = {
            val v = costInput.toLongOrNull() ?: 0L
            if (v > 0) Text(formatCostFull(v),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF2E7D32),
                modifier = Modifier.padding(end = 12.dp),
                fontWeight = FontWeight.Bold)
        },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
        ),
        supportingText = {
            if (costHint > 0 && costInput.isBlank())
                Text("App ước tính: ${formatCost(costHint)} • Chỉnh lại nếu biết chính xác",
                    color = MaterialTheme.colorScheme.primary)
        }
    )
    OutlinedTextField(
        value = note, onValueChange = onNoteChange,
        label = { Text("Ghi chú") },
        placeholder = { Text("Vd: Đặt vé trước, mang kem chống nắng...") },
        leadingIcon = { Icon(Icons.Filled.Notes, null) },
        modifier = Modifier.fillMaxWidth(), minLines = 2,
        shape = RoundedCornerShape(14.dp)
    )
}

// ── Helper format tiền ───────────────────────────────────────────────────────
private fun formatCost(amount: Long): String = when {
    amount >= 1_000_000 -> "${amount / 1_000_000}tr"
    amount >= 1_000     -> "${amount / 1_000}k"
    else                -> "${amount}đ"
}

private fun formatCostFull(amount: Long): String {
    return when {
        amount >= 1_000_000 -> {
            val millions = amount / 1_000_000
            val remainder = (amount % 1_000_000) / 1_000
            if (remainder > 0) "${millions},${remainder.toString().padStart(3,'0').trimEnd('0')}tr"
            else "${millions}tr"
        }
        amount >= 1_000 -> "${String.format("%,d", amount)}đ"
        else            -> "${amount}đ"
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
    var costInput by remember {
        mutableStateOf(if (item.tripPlace.estimatedCost > 0) item.tripPlace.estimatedCost.toString() else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa: ${item.place.name}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = visitDate, onValueChange = { visitDate = it },
                    label = { Text("Ngày tham quan") },
                    placeholder = { Text("dd/MM/yyyy") },
                    leadingIcon = { Icon(Icons.Filled.CalendarToday, null) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = visitTime, onValueChange = { visitTime = it },
                    label = { Text("Giờ tham quan") },
                    placeholder = { Text("HH:mm") },
                    leadingIcon = { Icon(Icons.Filled.Schedule, null) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = costInput,
                    onValueChange = { v -> costInput = v.filter { it.isDigit() } },
                    label = { Text("Chi phí ước tính (VNĐ)") },
                    leadingIcon = { Text("💰", modifier = Modifier.padding(start = 12.dp)) },
                    trailingIcon = {
                        val v = costInput.toLongOrNull() ?: 0L
                        if (v > 0) Text(
                            formatCostFull(v),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(end = 12.dp),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    leadingIcon = { Icon(Icons.Filled.Notes, null) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(item.tripPlace.copy(
                    visitDate = visitDate.trim(),
                    visitTime = visitTime.trim(),
                    note = note.trim(),
                    estimatedCost = costInput.toLongOrNull() ?: item.tripPlace.estimatedCost
                ))
            }) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}
