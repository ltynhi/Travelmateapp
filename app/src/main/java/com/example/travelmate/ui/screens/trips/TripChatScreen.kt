package com.example.travelmate.ui.screens.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelmate.data.model.TripMessage
import com.example.travelmate.ui.theme.*
import com.example.travelmate.viewmodel.AuthViewModel
import com.example.travelmate.viewmodel.TripChatViewModel
import com.example.travelmate.viewmodel.TripViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripChatScreen(
    tripId: String,
    authViewModel: AuthViewModel,
    tripViewModel: TripViewModel,
    chatViewModel: TripChatViewModel,
    onBack: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val selectedTrip by tripViewModel.selectedTrip.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val isSending by chatViewModel.isSending.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(tripId) {
        chatViewModel.startListening(tripId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage() {
        if (inputText.isNotBlank()) {
            currentUser?.let { user ->
                chatViewModel.sendMessage(tripId, inputText, user)
                inputText = ""
                keyboardController?.hide()
            }
        }
    }

    // Dùng Column thay vì Scaffold bottomBar để tránh conflict với bàn phím
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()   // đặt ở đây — đẩy toàn bộ Column lên khi bàn phím mở
    ) {
        // ── TopAppBar ─────────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Column {
                    Text(
                        selectedTrip?.tripName ?: "Chat chuyến đi",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${messages.size} tin nhắn",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
            }
        )

        HorizontalDivider()

        // ── Danh sách tin nhắn ────────────────────────────────────────────────
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💬", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Chưa có tin nhắn nào",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Hãy bắt đầu trò chuyện!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val grouped = messages.groupBy { msg ->
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(Date(msg.createdAt))
                }
                grouped.forEach { (date, dayMessages) ->
                    item { DateDivider(date = date) }
                    items(dayMessages, key = { it.messageId }) { message ->
                        val isMe = message.senderId == currentUser?.userId
                        MessageBubble(
                            message = message,
                            isMe = isMe,
                            onDelete = if (isMe) {
                                { chatViewModel.deleteMessage(message.messageId) }
                            } else null
                        )
                    }
                }
            }
        }

        // ── Input gửi tin nhắn ────────────────────────────────────────────────
        Surface(
            tonalElevation = 4.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Nhắn tin...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBlue40,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank()) SkyBlue40
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { sendMessage() },
                        enabled = inputText.isNotBlank() && !isSending
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Gửi",
                                tint = if (inputText.isNotBlank()) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Divider ngày ─────────────────────────────────────────────────────────────
@Composable
private fun DateDivider(date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = formatDateLabel(date),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

// ── Bubble tin nhắn ──────────────────────────────────────────────────────────
@Composable
private fun MessageBubble(
    message: TripMessage,
    isMe: Boolean,
    onDelete: (() -> Unit)?
) {
    var showDelete by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            AsyncImage(
                model = message.senderAvatar.ifBlank {
                    "https://ui-avatars.com/api/?name=${message.senderName}&background=4A90D9&color=fff&size=64"
                },
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(6.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!isMe) {
                Text(
                    message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isMe && showDelete && onDelete != null) {
                    IconButton(
                        onClick = { onDelete(); showDelete = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete, null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }

                Surface(
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isMe) 18.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 18.dp
                    ),
                    color = if (isMe) SkyBlue40 else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(Date(message.createdAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp, end = 4.dp)
            )
        }

        if (!isMe) {
            Spacer(Modifier.width(32.dp))
        }
    }
}

private fun formatDateLabel(date: String): String {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val d = sdf.parse(date) ?: return date
        val today = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { time = d }
        when {
            today.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR) &&
                    today.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) -> "Hôm nay"
            today.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1 &&
                    today.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) -> "Hôm qua"
            else -> date
        }
    } catch (e: Exception) { date }
}
