package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MainViewModel

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val authError by viewModel.authError.collectAsState()
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()
    val useFirebase by viewModel.useFirebase.collectAsState()
    val firebaseConfigError = viewModel.firebaseRepo.getFirebaseConfigError()

    val customConfig = remember { viewModel.getCustomFirebaseConfig() }
    var projectId by remember { mutableStateOf(customConfig?.get("projectId") ?: "") }
    var apiKey by remember { mutableStateOf(customConfig?.get("apiKey") ?: "") }
    var appId by remember { mutableStateOf(customConfig?.get("appId") ?: "") }
    var storageBucket by remember { mutableStateOf(customConfig?.get("storageBucket") ?: "") }

    var fbStatusMessage by remember { mutableStateOf<String?>(null) }
    var fbStatusSuccess by remember { mutableStateOf(false) }
    var isConfigExpanded by remember { mutableStateOf(false) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val gradientBg = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Icon / Branding
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Shield Vault Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp)
            )

            Text(
                text = "Study Group Vault",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Private materials vault && secure chatting repository.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Dynamic Firebase custom configuration card to replace Selector Banner
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ONLINE DATABASE STATUS:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val isConfigured = firebaseConfigError == null && projectId.isNotBlank() && apiKey.isNotBlank() && appId.isNotBlank()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (isConfigured) Color(0xFF10B981) else Color(0xFFEF4444),
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = if (isConfigured) "Connected to Live Firebase" else "Waiting for Firebase Cloud Setup",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConfigured) Color(0xFF047857) else MaterialTheme.colorScheme.error
                        )
                    }

                    Text(
                        text = "This application runs strictly online so teammate devices share materials & chats instantly in real-time.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { isConfigExpanded = !isConfigExpanded },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = if (isConfigExpanded) "Hide Config Panel" else "Setup Firebase Keys ⚙",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    AnimatedVisibility(
                        visible = isConfigExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Text(
                                text = "Enter your custom Google Firestore details to sync up messages and documents across all active group runs:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = projectId,
                                onValueChange = { projectId = it },
                                label = { Text("Firebase Project ID", fontSize = 11.sp) },
                                placeholder = { Text("e.g. study-vault-7a3") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_firebase_project_id")
                            )

                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = { apiKey = it },
                                label = { Text("API Key", fontSize = 11.sp) },
                                placeholder = { Text("e.g. AIzaSyD-X_abc...") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_firebase_api_key")
                            )

                            OutlinedTextField(
                                value = appId,
                                onValueChange = { appId = it },
                                label = { Text("App ID", fontSize = 11.sp) },
                                placeholder = { Text("e.g. 1:12345:android:xyz") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_firebase_app_id")
                            )

                            OutlinedTextField(
                                value = storageBucket,
                                onValueChange = { storageBucket = it },
                                label = { Text("Storage Bucket (Optional)", fontSize = 11.sp) },
                                placeholder = { Text("e.g. study-vault-7a3.appspot.com") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_firebase_storage_bucket")
                            )

                            fbStatusMessage?.let { msg ->
                                val alertBg = if (fbStatusSuccess) Color(0xFFDEF7EC) else Color(0xFFFDE8E8)
                                val alertText = if (fbStatusSuccess) Color(0xFF03543F) else Color(0xFF9B1C1C)
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = alertBg),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = msg,
                                        color = alertText,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.clearCustomFirebaseConfig()
                                        projectId = ""
                                        apiKey = ""
                                        appId = ""
                                        storageBucket = ""
                                        fbStatusSuccess = true
                                        fbStatusMessage = "Cleared custom inputs."
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                ) {
                                    Text("Reset", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        if (projectId.isBlank() || apiKey.isBlank() || appId.isBlank()) {
                                            fbStatusSuccess = false
                                            fbStatusMessage = "Please fill Project ID, API Key, and App ID."
                                        } else {
                                            val ok = viewModel.saveCustomFirebaseConfig(
                                                projectId = projectId,
                                                apiKey = apiKey,
                                                appId = appId,
                                                storageBucket = storageBucket.ifBlank { null }
                                            )
                                            fbStatusSuccess = ok
                                            if (ok) {
                                                fbStatusMessage = "Connected successfully!"
                                            } else {
                                                fbStatusMessage = "Connection failed! Check credentials."
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(38.dp)
                                ) {
                                    Text("Save & Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Username input
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                placeholder = { Text("e.g. om, alok, vishwa") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("username_input")
            )

            // Password input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Secret Password PIN") },
                placeholder = { Text("e.g. 1004") },
                leadingIcon = { Icon(Icons.Default.Password, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        Icon(icon, contentDescription = "Toggle password visibility")
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("password_input")
            )

            // Error display
            AnimatedVisibility(
                visible = authError != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                authError?.let { err ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error, 
                                contentDescription = "Error notification",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearAuthError() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear error",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    viewModel.login(username, password)
                },
                enabled = !isAuthenticating,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("login_button")
            ) {
                if (isAuthenticating) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Enter Vault Portal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }

            // Predefined Study Partners list cheat-sheet for user reference
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GROUP STUDY PARTNERS REFERENCE:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    Text(
                        text = "Om, Alok, Vishwa, Bhavya, Arjun, Freny, Vency, Palak, Anjli.\n(Secret Password pin defaults to: 1004)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
