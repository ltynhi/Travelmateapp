package com.example.travelmate.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.travelmate.data.model.User
import com.example.travelmate.viewmodel.AdminViewModel
import com.example.travelmate.viewmodel.ReviewViewModel
import com.example.travelmate.viewmodel.TravelPostViewModel
import com.example.travelmate.viewmodel.TripViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    adminViewModel: AdminViewModel,
    tripViewModel: TripViewModel,
    postViewModel: TravelPostViewModel,
    reviewViewModel: ReviewViewModel,
    onBack: () -> Unit
) {
    val users by adminViewModel.users.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()
    val successMessage by adminViewModel.successMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        adminViewModel.loadUsers()
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            adminViewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý người dùng") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "${users.size} người dùng",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(users) { user ->
                    AdminUserItem(
                        user = user,
                        onToggleBlock = {
                            adminViewModel.blockUser(user.userId, !user.isBlocked)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminUserItem(
    user: User,
    onToggleBlock: () -> Unit
) {
    var showBlockDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (user.isBlocked) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.avatarUrl.ifBlank { "https://via.placeholder.com/48" },
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (user.role == "admin") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "Admin",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    if (user.isBlocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                text = "Đã khóa",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (user.role != "admin") {
                IconButton(onClick = { showBlockDialog = true }) {
                    Icon(
                        imageVector = if (user.isBlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                        contentDescription = if (user.isBlocked) "Mở khóa" else "Khóa",
                        tint = if (user.isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text(if (user.isBlocked) "Mở khóa tài khoản" else "Khóa tài khoản") },
            text = {
                Text(
                    if (user.isBlocked)
                        "Bạn có chắc muốn mở khóa tài khoản của ${user.fullName}?"
                    else
                        "Bạn có chắc muốn khóa tài khoản của ${user.fullName}?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onToggleBlock()
                        showBlockDialog = false
                    },
                    colors = if (user.isBlocked) ButtonDefaults.buttonColors()
                    else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (user.isBlocked) "Mở khóa" else "Khóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("Hủy") }
            }
        )
    }
}
