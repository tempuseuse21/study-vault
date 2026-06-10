package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
fun ProfileTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val useFirebase by viewModel.useFirebase.collectAsState()
    val passwordMessage by viewModel.passwordMessage.collectAsState()
    val isPasswordSuccess by viewModel.isPasswordSuccess.collectAsState()

    val mongoConfig = remember { viewModel.getMongoConfig() }
    var mongoAppId by remember { mutableStateOf(mongoConfig?.get("appId") ?: "al-t3OP_hbvd10LXKAnMH0VuEhmqiwZLsucsJFmRgtD5X6") }
    var mongoApiKey by remember { mutableStateOf(mongoConfig?.get("apiKey") ?: "al-t3OP_hbvd10LXKAnMH0VuEhmqiwZLsucsJFmRgtD5X6") }
    var mongoDatasource by remember { mutableStateOf(mongoConfig?.get("datasource") ?: "studyvault") }
    var mongoDatabase by remember { mutableStateOf(mongoConfig?.get("database") ?: "studyvault") }

    val cloudinaryConfig = remember { viewModel.getCloudinaryConfig() }
    var cloudName by remember { mutableStateOf(cloudinaryConfig?.get("cloudName") ?: "dt1vudtwp") }
    var uploadPreset by remember { mutableStateOf(cloudinaryConfig?.get("uploadPreset") ?: "study vault") }

    var dbStatusMessage by remember { mutableStateOf<String?>(null) }
    var dbStatusSuccess by remember { mutableStateOf(false) }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var pwordVisible1 by remember { mutableStateOf(false) }
    var pwordVisible2 by remember { mutableStateOf(false) }
    var pwordVisible3 by remember { mutableStateOf(false) }

    val usernameDisplay = currentUser?.username?.replaceFirstChar { it.uppercase() } ?: "User"
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Avatar Large
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentUser?.username?.take(2)?.uppercase() ?: "SG",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = usernameDisplay,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = "Study Member • Private Group Roster",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Database Active status chip
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = if (viewModel.getMongoConfig() != null) "MongoDB + Cloudinary Connected" else "Local Database Storage Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46)
                        )
                    }
                }
            }
        }

        // Dynamics MongoDB & Cloudinary cloud configuration card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "MongoDB Atlas & Cloudinary Setup",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Provide your MongoDB Atlas App Services Data API coordinates and Cloudinary details below to link live chats and remote file hosting.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "MongoDB Atlas Database Configuration",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                // Mongo App ID
                OutlinedTextField(
                    value = mongoAppId,
                    onValueChange = { mongoAppId = it },
                    label = { Text("MongoDB Atlas App ID") },
                    placeholder = { Text("e.g. data-api-xxxxx") },
                    leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("mongo_app_id_input")
                )

                // Mongo API Key
                OutlinedTextField(
                    value = mongoApiKey,
                    onValueChange = { mongoApiKey = it },
                    label = { Text("MDB Data API Key") },
                    placeholder = { Text("Paste Data API Key") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("mongo_api_key_input")
                )

                // Mongo Cluster / Datasource Name
                OutlinedTextField(
                    value = mongoDatasource,
                    onValueChange = { mongoDatasource = it },
                    label = { Text("Data Source (Cluster Name)") },
                    placeholder = { Text("e.g. Cluster0") },
                    leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("mongo_datasource_input")
                )

                // Database name
                OutlinedTextField(
                    value = mongoDatabase,
                    onValueChange = { mongoDatabase = it },
                    label = { Text("Database Name") },
                    placeholder = { Text("e.g. study_group_vault") },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("mongo_database_input")
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "Cloudinary File Storage CDN",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                // Cloudinary Cloud Name
                OutlinedTextField(
                    value = cloudName,
                    onValueChange = { cloudName = it },
                    label = { Text("Cloud Name") },
                    placeholder = { Text("e.g. dxxxxx") },
                    leadingIcon = { Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("cloudinary_cloud_name_input")
                )

                // Cloudinary Upload Preset (Unsigned)
                OutlinedTextField(
                    value = uploadPreset,
                    onValueChange = { uploadPreset = it },
                    label = { Text("Unsigned Preset Name") },
                    placeholder = { Text("e.g. preset_study_vault") },
                    leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("cloudinary_preset_input")
                )

                // Feedback Alert Config Panel
                AnimatedVisibility(
                    visible = dbStatusMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    dbStatusMessage?.let { msg ->
                        val surfaceColor = if (dbStatusSuccess) Color(0xFFDEF7EC) else MaterialTheme.colorScheme.errorContainer
                        val contentColor = if (dbStatusSuccess) Color(0xFF03543F) else MaterialTheme.colorScheme.onErrorContainer
                        val visualIcon = if (dbStatusSuccess) Icons.Default.CheckCircle else Icons.Default.Error

                        Card(
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = visualIcon,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    color = contentColor,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { dbStatusMessage = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss info panel", tint = contentColor, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Clear configuration keys
                    OutlinedButton(
                        onClick = {
                            viewModel.clearMongoConfig()
                            viewModel.clearCloudinaryConfig()
                            mongoAppId = ""
                            mongoApiKey = ""
                            mongoDatasource = "Cluster0"
                            mongoDatabase = "study_group_vault"
                            cloudName = ""
                            uploadPreset = ""
                            dbStatusSuccess = true
                            dbStatusMessage = "Cleared custom MongoDB & Cloudinary configurations."
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Clear Keys")
                    }

                    // Save
                    Button(
                        onClick = {
                            if (mongoAppId.isBlank() || mongoApiKey.isBlank() || cloudName.isBlank() || uploadPreset.isBlank()) {
                                dbStatusSuccess = false
                                dbStatusMessage = "Please enter MongoDB App ID, API Key, Cloud Name and Preset."
                            } else {
                                val successMongo = viewModel.saveMongoConfig(
                                    appId = mongoAppId,
                                    apiKey = mongoApiKey,
                                    datasource = mongoDatasource.ifBlank { "Cluster0" },
                                    database = mongoDatabase.ifBlank { "study_group_vault" }
                                )
                                val successCloudinary = viewModel.saveCloudinaryConfig(
                                    cloudName = cloudName,
                                    uploadPreset = uploadPreset,
                                    apiKey = null,
                                    apiSecret = null
                                )
                                dbStatusSuccess = successMongo && successCloudinary
                                if (dbStatusSuccess) {
                                    dbStatusMessage = "MongoDB Atlas & Cloudinary successfully connected!"
                                } else {
                                    val err = viewModel.firebaseRepo.getFirebaseConfigError() ?: "Error verifying connection coordinates."
                                    dbStatusMessage = "Connection failed: $err"
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Save & Connect")
                    }
                }
            }
        }

        // Change Password Section Form Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Modify Vault Security PIN",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // Current Password Input
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password PIN") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { pwordVisible1 = !pwordVisible1 }) {
                            val icon = if (pwordVisible1) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            Icon(icon, contentDescription = "Toggle visibility", modifier = Modifier.size(20.dp))
                        }
                    },
                    visualTransformation = if (pwordVisible1) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("current_password_input")
                )

                // New Password Input
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Security PIN") },
                    placeholder = { Text("Min 4 characters") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { pwordVisible2 = !pwordVisible2 }) {
                            val icon = if (pwordVisible2) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            Icon(icon, contentDescription = "Toggle visibility", modifier = Modifier.size(20.dp))
                        }
                    },
                    visualTransformation = if (pwordVisible2) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_password_input")
                )

                // Confirm Password Input
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Security PIN") },
                    leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { pwordVisible3 = !pwordVisible3 }) {
                            val icon = if (pwordVisible3) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            Icon(icon, contentDescription = "Toggle visibility", modifier = Modifier.size(20.dp))
                        }
                    },
                    visualTransformation = if (pwordVisible3) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_password_input")
                )

                // Feedback Alert Message Panel
                AnimatedVisibility(
                    visible = passwordMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    passwordMessage?.let { msg ->
                        val surfaceColor = if (isPasswordSuccess) Color(0xFFDEF7EC) else MaterialTheme.colorScheme.errorContainer
                        val contentColor = if (isPasswordSuccess) Color(0xFF03543F) else MaterialTheme.colorScheme.onErrorContainer
                        val visualIcon = if (isPasswordSuccess) Icons.Default.CheckCircle else Icons.Default.Error

                        Card(
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = visualIcon,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    color = contentColor,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearPasswordMessage() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss info panel", tint = contentColor, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                // Password Change Trigger Button
                Button(
                    onClick = {
                        viewModel.changePassword(
                            currentPass = currentPassword,
                            newPass = newPassword,
                            confirmPass = confirmPassword
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("change_password_button")
                ) {
                    Text("Update Vault Security PIN", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Logout Settings / Extra Info
        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("logout_profile_action")
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Secure logout from device session", fontWeight = FontWeight.SemiBold)
        }
    }
}
