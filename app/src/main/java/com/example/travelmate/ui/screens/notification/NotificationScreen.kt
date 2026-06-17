package com.example.travelmate.ui.screens.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.travelmate.viewmodel.AuthViewModel
import com.example.travelmate.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    authViewModel: AuthViewModel,
    notificationViewModel: NotificationViewModel,
    onBack: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val notifications by notificationViewModel.notifications.collectAsState()
    val isLoading by notificationViewModel.isLoading.collectAsState()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()

    LaunchedEffect(currentUser) {
        currentUser?.let { notificationViewModel.loadNotificationsForUser(it.userId) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Thông báo", fontWeight = FontWeight.Bold)
                        if (unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = Peach40
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(onClick = {
                            currentUser?.let { notificationViewModel.markAllAsRead(it.userId) }
                        }) {
                            Text("Đọc tất cả", color = SkyBlue40,
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SkyBlue40)
            }
        } else if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔔", fontSize = 56.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Chưa có thông báo nào",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium)
                    Text("Các thông báo từ TravelMate sẽ hiện ở đây",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(notifications, key = { it.notificationId }) { notification ->
                    val isRead = notification.readBy.contains(currentUser?.userId)
                    NotificationItem(
                        notification = notification,
                        isRead = isRead,
                        onClick = {
                            currentUser?.let { user ->
                                notificationViewModel.markAsRead(notification.notificationId, user.userId)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: AppNotification,
    isRead: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isRead) Color.Transparent
    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon theo loại thông báo
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(notificationIconBg(notification.type)),
            contentAlignment = Alignment.Center
        ) {
            Text(notificationEmoji(notification.type), fontSize = 22.sp)
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isRead) FontWeight.Normal else FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatTime(notification.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodySmall,
                color = if (isRead) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Chấm xanh chưa đọc
        if (!isRead) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(SkyBlue40)
                    .align(Alignment.CenterVertically)
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 78.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

private fun notificationEmoji(type: String): String = when (type) {
    "new_place"  -> "🗺️"
    "promotion"  -> "🎉"
    "system"     -> "⚙️"
    "trip"       -> "✈️"
    else         -> "🔔"
}

private fun notificationIconBg(type: String): Color = when (type) {
    "new_place"  -> Color(0xFFE3F2FD)
    "promotion"  -> Color(0xFFFFF3E0)
    "system"     -> Color(0xFFF3E5F5)
    "trip"       -> Color(0xFFE8F5E9)
    else         -> Color(0xFFE3F2FD)
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Vừa xong"
        diff < 3_600_000 -> "${diff / 60_000} phút trước"
        diff < 86_400_000 -> "${diff / 3_600_000} giờ trước"
        diff < 604_800_000 -> "${diff / 86_400_000} ngày trước"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
