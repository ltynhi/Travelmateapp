package com.example.travelmate.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelmate.data.model.AppNotification
import com.example.travelmate.ui.theme.*
import com.example.travelmate.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNotificationScreen(
    notificationViewModel: NotificationViewModel,
    onBack: () -> Unit
) {
    val notifications by notificationViewModel.allNotifications.collectAsState()
    val isLoading by notificationViewModel.isLoading.collectAsState()
    val successMessage by notificationViewModel.successMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSendDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { notificationViewModel.loadAllNotifications() }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            notificationViewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý thông báo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSendDialog = true },
                icon = { Icon(Icons.Filled.Send, null) },
                text = { Text("Gửi thông báo") },
                containerColor = SkyBlue40,
                contentColor = Color.White
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (isLoading && notifications.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SkyBlue40)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatChip(
                            modifier = Modifier.weight(1f),
                            label = "Tổng gửi",
                            value = notifications.size.toString(),
                            color = SkyBlue40
                        )
                        StatChip(
                            modifier = Modifier.weight(1f),
                            label = "Gửi tất cả",
                            value = notifications.count { it.targetUserId.isEmpty() }.toString(),
                            color = Mint40
                        )
                        StatChip(
                            modifier = Modifier.weight(1f),
                            label = "Gửi riêng",
                            value = notifications.count { it.targetUserId.isNotEmpty() }.toString(),
                            color = Peach40
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Lịch sử thông báo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (notifications.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📭", fontSize = 48.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("Chưa có thông báo nào",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(notifications, key = { it.notificationId }) { notification ->
                        AdminNotificationItem(
                            notification = notification,
                            onDelete = { notificationViewModel.deleteNotification(notification.notificationId) }
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // ── Dialog gửi thông báo ──────────────────────────────────────────────────
    if (showSendDialog) {
        SendNotificationDialog(
            onSend = { title, message, type ->
                notificationViewModel.sendNotification(title, message, type)
                showSendDialog = false
            },
            onDismiss = { showSendDialog = false }
        )
    }
}

@Composable
private fun StatChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AdminNotificationItem(
    notification: AppNotification,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Type icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(typeColor(notification.type).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(typeEmoji(notification.type), fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                            .format(Date(notification.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Target badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (notification.targetUserId.isEmpty()) Mint40.copy(alpha = 0.15f)
                        else Peach40.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (notification.targetUserId.isEmpty()) "👥 Tất cả" else "👤 Cá nhân",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (notification.targetUserId.isEmpty()) Mint40 else Peach40,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    // Type badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = typeColor(notification.type).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = typeLabel(notification.type),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor(notification.type),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Filled.Delete, null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa thông báo") },
            text = { Text("Xóa thông báo \"${notification.title}\"?") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy") }
            }
        )
    }
}

// ── Dialog gửi thông báo ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SendNotificationDialog(
    onSend: (title: String, message: String, type: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("general") }
    var expandedType by remember { mutableStateOf(false) }

    val types = listOf(
        "general"   to "🔔 Thông báo chung",
        "new_place" to "🗺️ Địa điểm mới",
        "promotion" to "🎉 Khuyến mãi / Sự kiện",
        "trip"      to "✈️ Chuyến đi",
        "system"    to "⚙️ Hệ thống"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📣", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text("Gửi thông báo đến tất cả", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Loại thông báo
                ExposedDropdownMenuBox(
                    expanded = expandedType,
                    onExpandedChange = { expandedType = it }
                ) {
                    OutlinedTextField(
                        value = types.find { it.first == selectedType }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Loại thông báo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        types.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { selectedType = key; expandedType = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề *") },
                    placeholder = { Text("Vd: Địa điểm mới tại Đà Nẵng!") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Nội dung *") },
                    placeholder = { Text("Nhập nội dung thông báo...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                // Preview
                if (title.isNotBlank() || message.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SkyBlue40.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(typeEmoji(selectedType), fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    title.ifBlank { "Tiêu đề..." },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (title.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    message.ifBlank { "Nội dung..." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(title, message, selectedType) },
                enabled = title.isNotBlank() && message.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SkyBlue40)
            ) {
                Icon(Icons.Filled.Send, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Gửi ngay")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

private fun typeEmoji(type: String) = when (type) {
    "new_place"  -> "🗺️"
    "promotion"  -> "🎉"
    "system"     -> "⚙️"
    "trip"       -> "✈️"
    else         -> "🔔"
}

private fun typeLabel(type: String) = when (type) {
    "new_place"  -> "Địa điểm mới"
    "promotion"  -> "Khuyến mãi"
    "system"     -> "Hệ thống"
    "trip"       -> "Chuyến đi"
    else         -> "Chung"
}

private fun typeColor(type: String) = when (type) {
    "new_place"  -> Color(0xFF1976D2)
    "promotion"  -> Color(0xFFE65100)
    "system"     -> Color(0xFF7B1FA2)
    "trip"       -> Color(0xFF2E7D32)
    else         -> Color(0xFF0288D1)
}
