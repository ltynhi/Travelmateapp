package com.example.travelmate.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelmate.viewmodel.AdminViewModel
import com.example.travelmate.viewmodel.AuthViewModel

// ── Palette ──────────────────────────────────────────────────────────────────
private val StatColors = listOf(
    listOf(Color(0xFF6C63FF), Color(0xFF9B93FF)),   // purple  – người dùng
    listOf(Color(0xFF00B4D8), Color(0xFF48CAE4)),   // cyan    – địa điểm
    listOf(Color(0xFF06D6A0), Color(0xFF40E0B0)),   // teal    – chuyến đi
    listOf(Color(0xFFFF6B6B), Color(0xFFFF9A9A)),   // coral   – bài đăng
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel,
    onNavigateToPlaces: () -> Unit,
    onNavigateToTimeline: () -> Unit,
    onNavigateToReviews: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onLogout: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val stats by adminViewModel.dashboardStats.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { adminViewModel.loadDashboardStats() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            Icons.Filled.Logout,
                            contentDescription = "Đăng xuất",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Hero banner ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF6C63FF), Color(0xFF48CAE4))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                // decorative circle
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 30.dp, y = (-20).dp)
                        .background(
                            Color.White.copy(alpha = 0.12f),
                            shape = CircleShape
                        )
                )
                Column {
                    Text(
                        text = "Xin chào 👋",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = currentUser?.fullName ?: "Admin",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "TravelMate Admin",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Stats section ─────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SectionLabel("Thống kê")
                Spacer(Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                } else {
                    val statItems = listOf(
                        Triple("👥", stats.totalUsers, "Người dùng"),
                        Triple("🗺️", stats.totalPlaces, "Địa điểm"),
                        Triple("✈️", stats.totalTrips, "Chuyến đi"),
                        Triple("📸", stats.totalPosts, "Bài đăng"),
                    )
                    // 2 columns × 2 rows
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        statItems.chunked(2).forEachIndexed { rowIdx, rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEachIndexed { colIdx, (icon, count, label) ->
                                    val colorIdx = rowIdx * 2 + colIdx
                                    StatCard(
                                        modifier = Modifier.weight(1f),
                                        emoji = icon,
                                        count = count,
                                        label = label,
                                        gradient = StatColors[colorIdx]
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // ── Management section ────────────────────────────────────────
                SectionLabel("Quản lý")
                Spacer(Modifier.height(12.dp))

                val menuItems = listOf(
                    MenuItemData(
                        icon = Icons.Filled.Place,
                        title = "Địa điểm",
                        subtitle = "Thêm, sửa, xóa địa điểm du lịch",
                        gradient = listOf(Color(0xFF6C63FF), Color(0xFF9B93FF)),
                        onClick = onNavigateToPlaces
                    ),
                    MenuItemData(
                        icon = Icons.Filled.Star,
                        title = "Review",
                        subtitle = "Kiểm soát nội dung đánh giá",
                        gradient = listOf(Color(0xFFFF6B6B), Color(0xFFFF9A9A)),
                        onClick = onNavigateToReviews
                    ),
                    MenuItemData(
                        icon = Icons.Filled.People,
                        title = "Người dùng",
                        subtitle = "Xem và quản lý tài khoản",
                        gradient = listOf(Color(0xFF06D6A0), Color(0xFF40E0B0)),
                        onClick = onNavigateToUsers
                    ),
                    MenuItemData(
                        icon = Icons.Filled.Notifications,
                        title = "Thông báo",
                        subtitle = "Gửi thông báo đến người dùng",
                        gradient = listOf(Color(0xFF00B4D8), Color(0xFF48CAE4)),
                        onClick = onNavigateToNotifications
                    ),
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    menuItems.forEach { item ->
                        AdminMenuCard(item)
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Đăng xuất") },
            text = { Text("Bạn có chắc muốn đăng xuất?") },
            confirmButton = {
                Button(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) { Text("Đăng xuất") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Hủy") }
            }
        )
    }
}

// ── Section label ─────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF6C63FF))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Stat card — icon left, number + label right ───────────────────────────────
@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    emoji: String,
    count: Int,
    label: String,
    gradient: List<Color>
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradient))
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Icon circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.25f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 22.sp)
                }
                // Number + label
                Column {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ── Menu item data class ──────────────────────────────────────────────────────
private data class MenuItemData(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val gradient: List<Color>,
    val onClick: () -> Unit
)

// ── Menu card ─────────────────────────────────────────────────────────────────
@Composable
private fun AdminMenuCard(item: MenuItemData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gradient icon box
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(item.gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
