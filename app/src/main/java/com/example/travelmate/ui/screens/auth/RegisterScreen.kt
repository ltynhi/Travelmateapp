package com.example.travelmate.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelmate.ui.theme.*
import com.example.travelmate.viewmodel.AuthState
import com.example.travelmate.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf("") }
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onRegisterSuccess()
            authViewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF5ED4B8),   // mint top
                        Color(0xFF4A90D9),   // sky mid
                        Color(0xFF2A6DB5)    // deep blue bottom
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier.size(250.dp).align(Alignment.TopEnd).offset(x = 60.dp, y = (-40).dp)
                .background(
                    Brush.radialGradient(colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)),
                    shape = RoundedCornerShape(50)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            Text("🌏", fontSize = 52.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Tạo tài khoản",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                "Bắt đầu hành trình của bạn ngay hôm nay",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBlue40,
                        unfocusedBorderColor = SkyBlue90,
                        focusedContainerColor = SkyBlue99,
                        unfocusedContainerColor = SkyBlue99,
                        focusedTextColor = DarkSlate,
                        unfocusedTextColor = DarkSlate,
                        cursorColor = SkyBlue40
                    )
                    val fieldShape = RoundedCornerShape(14.dp)

                    OutlinedTextField(
                        value = fullName, onValueChange = { fullName = it },
                        label = { Text("Họ tên") },
                        leadingIcon = { Icon(Icons.Filled.Person, null, tint = SkyBlue40) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = fieldShape, colors = fieldColors
                    )
                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Filled.Email, null, tint = SkyBlue40) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = fieldShape, colors = fieldColors
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("Mật khẩu") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = SkyBlue40) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    null, tint = MidGray
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = fieldShape, colors = fieldColors
                    )
                    OutlinedTextField(
                        value = confirmPassword, onValueChange = { confirmPassword = it },
                        label = { Text("Xác nhận mật khẩu") },
                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = Mint40) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = fieldShape, colors = fieldColors
                    )

                    val errorMsg = when {
                        localError.isNotBlank() -> localError
                        authState is AuthState.Error -> (authState as AuthState.Error).message
                        else -> ""
                    }
                    if (errorMsg.isNotBlank()) {
                        Text(errorMsg, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(GradOceanStart, GradSkyEnd)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            TextButton(
                                onClick = {
                                    localError = ""
                                    when {
                                        fullName.isBlank() -> localError = "Vui lòng nhập họ tên"
                                        email.isBlank() -> localError = "Vui lòng nhập email"
                                        password.length < 6 -> localError = "Mật khẩu ít nhất 6 ký tự"
                                        password != confirmPassword -> localError = "Mật khẩu không khớp"
                                        else -> authViewModel.register(fullName.trim(), email.trim(), password)
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                enabled = authState !is AuthState.Loading
                            ) {
                                Text("Tạo tài khoản", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Đã có tài khoản?", color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onNavigateToLogin) {
                    Text("Đăng nhập", color = SunshineLight,
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
