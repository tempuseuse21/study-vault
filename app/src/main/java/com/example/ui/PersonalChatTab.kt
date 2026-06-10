package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File
import com.example.model.PersonalMessage
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PersonalChatTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedPartner by viewModel.selectedChatPartner.collectAsState()
    val predefinedUsers by viewModel.predefinedUnameList.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentUsername = currentUser?.username ?: ""

    // Filter out the active user to get the interlocutors roster
    val partnersRoster = remember(predefinedUsers, currentUsername) {
        predefinedUsers.filterNot { it.trim().lowercase() == currentUsername.trim().lowercase() }
    }

    AnimatedContent(
        targetState = selectedPartner,
        transitionSpec = {
            if (targetState == null) {
                // Moving back to roster list
                slideInHorizontally(initialOffsetX = { -it }) + fadeIn() with slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            } else {
                // Moving forward to active chat thread
                slideInHorizontally(initialOffsetX = { it }) + fadeIn() with slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
            }
        },
        modifier = modifier.fillMaxSize()
    ) { partner ->
        if (partner == null) {
            // Roster List View
            RosterSelectionScreen(
                partners = partnersRoster,
                onPartnerClick = { viewModel.selectChatPartner(it) }
            )
        } else {
            // Private Conversation Thread View
            PrivateChatThreadScreen(
                partnerName = partner,
                viewModel = viewModel,
                onBackClick = { viewModel.selectChatPartner(null) }
            )
        }
    }
}

@Composable
fun RosterSelectionScreen(
    partners: List<String>,
    onPartnerClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            "Private Study Chats",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            "Choose a teammate below to open a private end-to-end messaging vault:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(partners) { partner ->
                val capitalized = partner.replaceFirstChar { it.uppercase() }
                
                Card(
                    onClick = { onPartnerClick(partner) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("private_roster_item_$partner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = capitalized,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Click to chat privately",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrivateChatThreadScreen(
    partnerName: String,
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val messages by viewModel.personalMessages.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentUsername = currentUser?.username ?: ""
    
    var textInput by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()

    val context = LocalContext.current
    val personalFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.uploadPersonalFile(it, context.contentResolver, partnerName)
        }
    }

    // Auto-scroll to bottom of chat when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }

    val partnerCapitalized = partnerName.replaceFirstChar { it.uppercase() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Chat Header Title Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("private_chat_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Return to teammate roster"
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = partnerName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = partnerCapitalized,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Private chat with teammate",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Messages Feed Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            "Start your safe discussion!",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Type your first greeting to begin a private conversation with $partnerCapitalized.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isCurrentUser = msg.sender.trim().lowercase() == currentUsername.trim().lowercase()
                        PrivateMessageBubble(msg = msg, isCurrentUser = isCurrentUser)
                    }
                }
            }
        }

        // Send Text Section
        Surface(
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        personalFileLauncher.launch("*/*")
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("private_chat_attach_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach private document",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Write a message...", fontSize = 14.sp) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    maxLines = 4,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("private_chat_input_field")
                )

                IconButton(
                    onClick = {
                        if (textInput.trim().isNotEmpty()) {
                            viewModel.sendPersonalMessage(textInput.trim())
                            textInput = ""
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("private_chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Send,
                        contentDescription = "Send private message",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PrivateMessageBubble(
    msg: PersonalMessage,
    isCurrentUser: Boolean
) {
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(msg.timestamp) {
        if (msg.timestamp > 0) timeFormatter.format(Date(msg.timestamp)) else ""
    }
    val isFile = msg.messageText.startsWith("[FILE]")
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (isFile) {
            val fileData = msg.messageText.substring(6)
            val parts = fileData.split("|")
            val fileName = parts.getOrNull(0) ?: "FileAttachment"
            val fileUrl = parts.getOrNull(1) ?: ""
            val fileExtension = fileName.substringAfterLast('.', "").lowercase()

            val iconRes = when (fileExtension) {
                "pdf" -> Icons.Default.PictureAsPdf
                "csv" -> Icons.Default.GridOn
                "xls", "xlsx" -> Icons.Default.TableChart
                "doc", "docx" -> Icons.Default.Description
                "zip", "rar" -> Icons.Default.Source
                else -> Icons.Default.InsertDriveFile
            }
            val iconColor = when (fileExtension) {
                "pdf" -> Color(0xFFEF4444)
                "csv" -> Color(0xFF10B981)
                "xls", "xlsx" -> Color(0xFF10B981)
                "doc", "docx" -> Color(0xFF3B82F6)
                "zip", "rar" -> Color(0xFF8B5CF6)
                else -> Color(0xFF6B7280)
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clickable { openFileInApp(context, fileUrl) }
                    .testTag("private_file_bubble")
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(iconColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconRes,
                            contentDescription = "File Type Icon",
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Private Memo",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            if (formattedTime.isNotEmpty()) {
                                Text(
                                    text = "•",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Text(
                                    text = formattedTime,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open File Action",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isCurrentUser) 16.dp else 2.dp,
                    bottomEnd = if (isCurrentUser) 2.dp else 16.dp
                ),
                color = if (isCurrentUser) MaterialTheme.colorScheme.secondary 
                        else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isCurrentUser) MaterialTheme.colorScheme.onSecondary 
                               else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = msg.messageText,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedTime,
                        fontSize = 9.sp,
                        color = if (isCurrentUser) MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

private fun openFileInApp(context: Context, urlOrPath: String) {
    try {
        if (urlOrPath.startsWith("/")) {
            val file = File(urlOrPath)
            if (file.exists()) {
                android.widget.Toast.makeText(
                    context, 
                    "Local Demo Mode: Opened file: ${file.name}", 
                    android.widget.Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                android.widget.Toast.makeText(context, "Error: File path does not exist locally.", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlOrPath)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error: Unable to open file.", android.widget.Toast.LENGTH_SHORT).show()
    }
}
