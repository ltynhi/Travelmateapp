package com.example.travelmate.ui.screens.setup

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelmate.utils.SeedResult
import com.example.travelmate.viewmodel.SeedState
import com.example.travelmate.viewmodel.SeedViewModel

private val FIRESTORE_RULES = """
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
""".trimIndent()

@Composable
fun SetupScreen(onSetupComplete: () -> Unit) {
    val seedViewModel: SeedViewModel = viewModel()
    val seedState by seedViewModel.seedState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    LaunchedEffect(Unit) { seedViewModel.runSeed() }

    LaunchedEffect(seedState) {
        if (seedState is SeedState.Done) {
            kotlinx.coroutines.delay(800)
            onSetupComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF74C0FC),
                        Color(0xFF4A90D9),
                        Color(0xFF1A5FA8)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Text(text = "🌍", fontSize = 72.sp, modifier = Modifier.scale(scale))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "TravelMate",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Khám phá thế giới cùng bạn",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            when (val state = seedState) {
                // ── Đang loading ─────────────────────────────────────────────
                is SeedState.Loading -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(44.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Đang khởi tạo dữ liệu...",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(24.dp))
                    TextButton(onClick = onSetupComplete) {
                        Text("Bỏ qua", color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }

                // ── Thành công ───────────────────────────────────────────────
                is SeedState.Done -> {
                    SuccessCard(result = state.result)
                }

                // ── Cần setup Firestore Rules ─────────────────────────────────
                is SeedState.Error -> {
                    val needsRules = state.message.contains("Rules", ignoreCase = true) ||
                            state.message.contains("permission", ignoreCase = true) ||
                            state.message.contains("PERMISSION_DENIED", ignoreCase = true)

                    if (needsRules) {
                        FirestoreRulesGuide(
                            rules = FIRESTORE_RULES,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(FIRESTORE_RULES))
                                copied = true
                            },
                            copied = copied,
                            onRetry = {
                                copied = false
                                seedViewModel.runSeed()
                            },
                            onSkip = onSetupComplete
                        )
                    } else {
                        GenericErrorCard(
                            message = state.message,
                            onRetry = { seedViewModel.runSeed() },
                            onSkip = onSetupComplete
                        )
                    }
                }

                else -> {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(44.dp))
                }
            }
        }
    }
}

// ── Card thành công ───────────────────────────────────────────────────────────
@Composable
private fun SuccessCard(result: SeedResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF69F0AE),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (result.placesSeeded) "Khởi tạo thành công!" else "Sẵn sàng!",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )

            if (result.placesSeeded) {
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow("✅", "10 địa điểm du lịch Việt Nam")
                InfoRow("✅", "4 bài đăng timeline mẫu")
                InfoRow("✅", "4 đánh giá mẫu")
            }

            if (result.adminEmail.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "🔑 Tài khoản Admin",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                AdminCredentialRow("Email", result.adminEmail)
                AdminCredentialRow("Mật khẩu", result.adminPassword)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Đang vào ứng dụng...",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ── Hướng dẫn setup Firestore Rules ──────────────────────────────────────────
@Composable
private fun FirestoreRulesGuide(
    rules: String,
    onCopy: () -> Unit,
    copied: Boolean,
    onRetry: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0).copy(alpha = 0.95f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Cần cấu hình Firestore",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Các bước hướng dẫn
                StepItem("1", "Mở Firebase Console", "console.firebase.google.com")
                StepItem("2", "Chọn project TravelMateApp")
                StepItem("3", "Vào Firestore Database → Rules")
                StepItem("4", "Xóa nội dung cũ, dán Rules bên dưới")
                StepItem("5", "Nhấn Publish → quay lại app nhấn Thử lại")

                Spacer(modifier = Modifier.height(16.dp))

                // Rules code box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E1E1E)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Firestore Rules",
                                color = Color(0xFF9E9E9E),
                                style = MaterialTheme.typography.labelSmall
                            )
                            TextButton(
                                onClick = onCopy,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    if (copied) Icons.Filled.CheckCircle else Icons.Filled.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (copied) Color(0xFF69F0AE) else Color(0xFF90CAF9)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (copied) "Đã sao chép!" else "Sao chép",
                                    color = if (copied) Color(0xFF69F0AE) else Color(0xFF90CAF9),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rules,
                            color = Color(0xFFCE9178),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Bỏ qua")
            }
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF0061A4)
                )
            ) {
                Text("Thử lại", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Lỗi chung ────────────────────────────────────────────────────────────────
@Composable
private fun GenericErrorCard(message: String, onRetry: () -> Unit, onSkip: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE).copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚠️ Lỗi kết nối", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5D4037), textAlign = TextAlign.Center)
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onSkip, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
            Text("Bỏ qua")
        }
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0061A4))
        ) { Text("Thử lại") }
    }
}

// ── Helper composables ────────────────────────────────────────────────────────
@Composable
private fun InfoRow(icon: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AdminCredentialRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
        Text(value, color = Color.White, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StepItem(number: String, title: String, subtitle: String = "") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFF4A90D9),
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(number, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, color = Color(0xFF212121), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = Color(0xFF4A90D9), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
