package com.example.travelmate.ui.screens.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelmate.ui.theme.*
import com.example.travelmate.viewmodel.AuthViewModel
import com.example.travelmate.viewmodel.TripInviteViewModel
import com.example.travelmate.viewmodel.TripViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    authViewModel: AuthViewModel,
    tripViewModel: TripViewModel,
    inviteViewModel: TripInviteViewModel,
    onTripClick: (String) -> Unit,
    onInviteInbox: () -> Unit,
    onBack: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val myTrips by tripViewModel.trips.collectAsState()
    val joinedTrips by inviteViewModel.joinedTrips.collectAsState()
    val pendingCount by inviteViewModel.pendingCount.collectAsState()
    val isLoading by tripViewModel.isLoading.collectAsState()
    val successMessage by tripViewModel.successMessage.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var tripName by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            tripViewModel.loadTrips(user.userId)
            inviteViewModel.loadJoinedTrips(user.userId)
            inviteViewModel.loadPendingCount(user.userId)
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            tripViewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chuyến đi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    // Nút inbox lời mời
                    Box {
                        IconButton(onClick = onInviteInbox) {
                            Icon(Icons.Filled.Mail, null, tint = SkyBlue40)
                        }
                        if (pendingCount > 0) {
                            Surface(
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-4).dp, y = 4.dp),
                                shape = CircleShape,
                                color = Peach40
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        if (pendingCount > 9) "9+" else pendingCount.toString(),
                                        fontSize = 9.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = SkyBlue40,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, null)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab: Của tôi / Được mời
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = SkyBlue40
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text("Của tôi (${myTrips.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Được mời (${joinedTrips.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            val displayTrips = if (selectedTab == 0) myTrips else joinedTrips

            if (isLoading && displayTrips.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SkyBlue40)
                }
            } else if (displayTrips.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (selectedTab == 0) "✈️" else "👥", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (selectedTab == 0) "Chưa có chuyến đi nào"
                            else "Chưa được mời vào chuyến đi nào",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            if (selectedTab == 0) "Nhấn + để tạo chuyến đi mới"
                            else "Khi bạn bè mời bạn, chuyến đi sẽ hiện ở đây",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayTrips) { trip ->
                        TripCard(
                            trip = trip,
                            onClick = { onTripClick(trip.tripId) },
                            onDelete = if (selectedTab == 0) {
                                {
                                    currentUser?.let { user ->
                                        tripViewModel.deleteTrip(trip.tripId, user.userId)
                                    }
                                }
                            } else null,
                            showMemberCount = trip.memberIds.isNotEmpty()
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Tạo chuyến đi mới", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tripName, onValueChange = { tripName = it },
                        label = { Text("Tên chuyến đi") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = startDate, onValueChange = { startDate = it },
                        label = { Text("Ngày bắt đầu (dd/MM/yyyy)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = endDate, onValueChange = { endDate = it },
                        label = { Text("Ngày kết thúc (dd/MM/yyyy)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tripName.isNotBlank() && startDate.isNotBlank() && endDate.isNotBlank()) {
                            currentUser?.let { user ->
                                tripViewModel.createTrip(user.userId, tripName, startDate, endDate)
                            }
                            showCreateDialog = false
                            tripName = ""; startDate = ""; endDate = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlue40)
                ) { Text("Tạo") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Hủy") }
            }
        )
    }
}
