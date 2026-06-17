package com.example.travelmate.ui.screens.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import coil.compose.AsyncImage
import com.example.travelmate.data.model.User
import com.example.travelmate.ui.theme.*
import com.example.travelmate.viewmodel.AuthViewModel
import com.example.travelmate.viewmodel.TripInviteViewModel
import com.example.travelmate.viewmodel.TripViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripInviteScreen(
    tripId: String,
    authViewModel: AuthViewModel,
    tripViewModel: TripInviteViewModel,
    tripVm: TripViewModel,
    onBack: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val selectedTrip by tripVm.selectedTrip.collectAsState()
    val searchResults by tripViewModel.searchResults.collectAsState()
    val tripMembers by tripViewModel.tripMembers.collectAsState()
    val isSearching by tripViewModel.isSearching.collectAsState()
    val successMessage by tripViewModel.successMessage.collectAsState()
    val error by tripViewModel.error.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(selectedTrip) {
        selectedTrip?.let { trip ->
            if (trip.memberIds.isNotEmpty()) {
                tripViewModel.loadTripMembers(trip.memberIds)
            }
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            tripViewModel.clearMessages()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            tripViewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Thành viên", fontWeight = FontWeight.Bold)
                        selectedTrip?.let {
                            Text(it.tripName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Tìm kiếm & mời ───────────────────────────────────────────────
            item {
                Text("Mời bạn bè",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        tripViewModel.searchUsers(it)
                    },
                    placeholder = { Text("Tìm theo tên hoặc email...") },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = SkyBlue40) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                tripViewModel.clearSearch()
                            }) {
                                Icon(Icons.Filled.Close, null,
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // ── Kết quả tìm kiếm ─────────────────────────────────────────────
            if (isSearching) {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp),
                            color = SkyBlue40, strokeWidth = 2.dp)
                    }
                }
            } else if (searchResults.isNotEmpty()) {
                item {
                    Text("Kết quả tìm kiếm",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold)
                }
                items(searchResults) { user ->
                    val isAlreadyMember = selectedTrip?.memberIds?.contains(user.userId) == true
                    val isOwner = user.userId == selectedTrip?.userId
                    val isMe = user.userId == currentUser?.userId

                    SearchUserItem(
                        user = user,
                        isAlreadyMember = isAlreadyMember || isOwner || isMe,
                        statusLabel = when {
                            isMe -> "Bạn"
                            isOwner -> "Chủ trip"
                            isAlreadyMember -> "Đã tham gia"
                            else -> null
                        },
                        onInvite = {
                            currentUser?.let { me ->
                                selectedTrip?.let { trip ->
                                    tripViewModel.sendInvite(
                                        tripId = trip.tripId,
                                        tripName = trip.tripName,
                                        fromUser = me,
                                        toUser = user
                                    )
                                }
                            }
                        }
                    )
                }
            } else if (searchQuery.isNotBlank()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center) {
                        Text("Không tìm thấy người dùng nào",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Danh sách thành viên ──────────────────────────────────────────
            item {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Thành viên",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    val total = (tripMembers.size + 1)
                    Surface(shape = CircleShape, color = SkyBlue40.copy(alpha = 0.15f)) {
                        Text("$total",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyBlue40, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Chủ trip
            selectedTrip?.let { trip ->
                item {
                    // Tìm thông tin chủ trip
                    val ownerInfo = if (trip.userId == currentUser?.userId) currentUser else null
                    MemberItem(
                        name = ownerInfo?.fullName ?: "Chủ chuyến đi",
                        email = ownerInfo?.email ?: "",
                        avatarUrl = ownerInfo?.avatarUrl ?: "",
                        badge = "Chủ trip",
                        badgeColor = SkyBlue40,
                        onRemove = null
                    )
                }
            }

            // Các thành viên
            items(tripMembers) { member ->
                val isMe = member.userId == currentUser?.userId
                MemberItem(
                    name = member.fullName,
                    email = member.email,
                    avatarUrl = member.avatarUrl,
                    badge = if (isMe) "Bạn" else null,
                    badgeColor = Mint40,
                    onRemove = if (currentUser?.userId == selectedTrip?.userId && !isMe) {
                        {
                            tripViewModel.leaveTrip(tripId, member.userId)
                            // Reload members
                            selectedTrip?.let { trip ->
                                val updated = trip.memberIds.filter { it != member.userId }
                                tripViewModel.loadTripMembers(updated)
                            }
                        }
                    } else null
                )
            }

            if (tripMembers.isEmpty() && selectedTrip?.memberIds.isNullOrEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.PersonAdd, null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("Chưa có thành viên nào",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Tìm kiếm và mời bạn bè ở trên",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchUserItem(
    user: User,
    isAlreadyMember: Boolean,
    statusLabel: String?,
    onInvite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatarUrl.ifBlank {
                "https://ui-avatars.com/api/?name=${user.fullName}&background=4A90D9&color=fff&size=64"
            },
            contentDescription = null,
            modifier = Modifier.size(44.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.fullName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(user.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        if (isAlreadyMember && statusLabel != null) {
            Surface(shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(statusLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            FilledTonalButton(
                onClick = onInvite,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = SkyBlue40.copy(alpha = 0.12f),
                    contentColor = SkyBlue40
                )
            ) {
                Icon(Icons.Filled.PersonAdd, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Mời", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MemberItem(
    name: String,
    email: String,
    avatarUrl: String,
    badge: String?,
    badgeColor: Color,
    onRemove: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = avatarUrl.ifBlank {
                "https://ui-avatars.com/api/?name=$name&background=4A90D9&color=fff&size=64"
            },
            contentDescription = null,
            modifier = Modifier.size(44.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (badge != null) {
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(50),
                        color = badgeColor.copy(alpha = 0.12f)) {
                        Text(badge,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Text(email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.PersonRemove, null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
