package com.example.travelmate.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelmate.ui.theme.*
import com.example.travelmate.utils.CloudinaryUploader
import com.example.travelmate.viewmodel.AuthViewModel
import com.example.travelmate.viewmodel.FavoriteViewModel
import com.example.travelmate.viewmodel.TravelPostViewModel
import com.example.travelmate.viewmodel.TripViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    favoriteViewModel: FavoriteViewModel,
    tripViewModel: TripViewModel,
    postViewModel: TravelPostViewModel,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by authViewModel.currentUser.collectAsState()
    val myPosts by postViewModel.myPosts.collectAsState()
    val trips by tripViewModel.trips.collectAsState()
    val favorites by favoriteViewModel.favorites.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var isUploadingAvatar by remember { mutableStateOf(false) }

    var newNameInput by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Picker ảnh avatar
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isUploadingAvatar = true
                val result = CloudinaryUploader.uploadImage(context, it, "avatars")
                result.fold(
                    onSuccess = { url ->
                        authViewModel.updateAvatar(url)
                        snackbarMessage = "Đã cập nhật ảnh đại diện!"
                    },
                    onFailure = { e ->
                        snackbarMessage = "Upload thất bại: ${e.message}"
                    }
                )
                isUploadingAvatar = false
            }
        }
    }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            postViewModel.loadMyPosts(user.userId)
            tripViewModel.loadTrips(user.userId)
            favoriteViewModel.loadFavorites(user.userId)
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hồ sơ cá nhân") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Đăng xuất",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Hero header với gradient ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(GradSkyStart, GradSkyEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Decorative circle
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 30.dp, y = (-20).dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
                // Avatar
                Box(contentAlignment = Alignment.BottomEnd) {
                    val avatarUrl = currentUser?.avatarUrl?.ifBlank {
                        "https://ui-avatars.com/api/?name=${currentUser?.fullName ?: "User"}&background=4A90D9&color=fff&size=128"
                    } ?: "https://ui-avatars.com/api/?name=User&background=4A90D9&color=fff&size=128"

                    if (isUploadingAvatar) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f))
                                .clickable { avatarPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentScale = ContentScale.Crop
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier
                            .size(26.dp)
                            .clickable { avatarPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Edit, null,
                                modifier = Modifier.size(14.dp),
                                tint = SkyBlue40)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Tên + email
            Text(
                text = currentUser?.fullName ?: "",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = currentUser?.email ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            // ── Thống kê ─────────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(count = trips.size, label = "Chuyến đi", icon = "✈️")
                    VerticalDivider(modifier = Modifier.height(48.dp))
                    StatItem(count = myPosts.size, label = "Bài đăng", icon = "📸")
                    VerticalDivider(modifier = Modifier.height(48.dp))
                    StatItem(count = favorites.size, label = "Yêu thích", icon = "❤️")
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Thông tin cá nhân ─────────────────────────────────────────────
            ProfileSection(title = "Thông tin cá nhân") {
                ProfileMenuItem(
                    icon = Icons.Filled.Person,
                    iconBg = SkyBlue40,
                    title = "Đổi tên hiển thị",
                    subtitle = currentUser?.fullName ?: "",
                    onClick = {
                        newNameInput = currentUser?.fullName ?: ""
                        showEditNameDialog = true
                    }
                )
                ProfileMenuItem(
                    icon = Icons.Filled.Lock,
                    iconBg = Mint40,
                    title = "Đổi mật khẩu",
                    subtitle = "Cập nhật mật khẩu mới",
                    onClick = {
                        newPassword = ""
                        confirmNewPassword = ""
                        showChangePasswordDialog = true
                    }
                )
                ProfileMenuItem(
                    icon = Icons.Filled.Image,
                    iconBg = Peach40,
                    title = "Đổi ảnh đại diện",
                    subtitle = "Chọn ảnh từ điện thoại",
                    onClick = { avatarPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Hoạt động của tôi ─────────────────────────────────────────────
            ProfileSection(title = "Hoạt động của tôi") {
                ProfileMenuItem(
                    icon = Icons.Filled.FlightTakeoff,
                    iconBg = SkyBlue40,
                    title = "Chuyến đi",
                    subtitle = "${trips.size} chuyến đi đã lên kế hoạch",
                    onClick = {}
                )
                ProfileMenuItem(
                    icon = Icons.Filled.PhotoLibrary,
                    iconBg = Peach40,
                    title = "Bài đăng timeline",
                    subtitle = "${myPosts.size} khoảnh khắc đã chia sẻ",
                    onClick = {}
                )
                ProfileMenuItem(
                    icon = Icons.Filled.Favorite,
                    iconBg = Color(0xFFE91E63),
                    title = "Địa điểm yêu thích",
                    subtitle = "${favorites.size} địa điểm đã lưu",
                    onClick = {}
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Về ứng dụng ───────────────────────────────────────────────────
            ProfileSection(title = "Về ứng dụng") {
                ProfileMenuItem(
                    icon = Icons.Filled.Info,
                    iconBg = Color(0xFF9C27B0),
                    title = "Phiên bản",
                    subtitle = "TravelMate v1.0.0",
                    onClick = {},
                    showArrow = false
                )
                ProfileMenuItem(
                    icon = Icons.Filled.Description,
                    iconBg = Color(0xFF607D8B),
                    title = "Điều khoản sử dụng",
                    subtitle = "Xem điều khoản & chính sách",
                    onClick = {}
                )
                ProfileMenuItem(
                    icon = Icons.Filled.Star,
                    iconBg = Sunshine,
                    title = "Đánh giá ứng dụng",
                    subtitle = "Hãy cho chúng tôi 5 sao ⭐",
                    onClick = {}
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Đăng xuất ─────────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { showLogoutDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Logout, null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Đăng xuất",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Dialog đổi tên ───────────────────────────────────────────────────────
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Đổi tên hiển thị") },
            text = {
                OutlinedTextField(
                    value = newNameInput,
                    onValueChange = { newNameInput = it },
                    label = { Text("Tên mới") },
                    leadingIcon = { Icon(Icons.Filled.Person, null, tint = SkyBlue40) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNameInput.isNotBlank()) {
                            authViewModel.updateFullName(newNameInput.trim()) { success ->
                                snackbarMessage = if (success) "Đã cập nhật tên!" else "Cập nhật thất bại"
                            }
                        }
                        showEditNameDialog = false
                    },
                    enabled = newNameInput.isNotBlank()
                ) { Text("Lưu") }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("Hủy") }
            }
        )
    }

    // ── Dialog đổi mật khẩu ──────────────────────────────────────────────────
    if (showChangePasswordDialog) {
        var localError by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Đổi mật khẩu") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Mật khẩu mới") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = SkyBlue40) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it },
                        label = { Text("Xác nhận mật khẩu mới") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = Mint40) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (localError.isNotBlank()) {
                        Text(localError, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            newPassword.length < 6 -> localError = "Mật khẩu ít nhất 6 ký tự"
                            newPassword != confirmNewPassword -> localError = "Mật khẩu không khớp"
                            else -> {
                                authViewModel.changePassword(newPassword) { success, msg ->
                                    snackbarMessage = msg
                                }
                                showChangePasswordDialog = false
                            }
                        }
                    }
                ) { Text("Đổi mật khẩu") }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) { Text("Hủy") }
            }
        )
    }

    // ── Dialog đăng xuất ─────────────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Đăng xuất") },
            text = { Text("Bạn có chắc muốn đăng xuất không?") },
            confirmButton = {
                Button(
                    onClick = { showLogoutDialog = false; onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Đăng xuất") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Hủy") }
            }
        )
    }
}

// ── Section wrapper ───────────────────────────────────────────────────────────
@Composable
private fun ProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column { content() }
        }
    }
}

// ── Menu item ─────────────────────────────────────────────────────────────────
@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showArrow: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon với nền màu tròn
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconBg, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (showArrow) {
            Icon(Icons.Filled.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp))
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

// ── Stat item ─────────────────────────────────────────────────────────────────
@Composable
private fun StatItem(count: Int, label: String, icon: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 24.sp)
        Text(count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
    }
}
