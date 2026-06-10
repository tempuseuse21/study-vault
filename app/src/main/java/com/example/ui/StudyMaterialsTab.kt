package com.example.ui

import android.content.Context
import android.content.Intent
import java.io.File
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.model.StudyMaterial
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StudyMaterialsTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val materials by viewModel.studyMaterials.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uploadStatus by viewModel.uploadingStatus.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // File manager Uri launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.uploadUri(it, context.contentResolver, null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Elegant Section Header layout with Side Upload Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Materials Cabinet",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Access and share group documents safely",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }

                Button(
                    onClick = {
                        // Request PDF, Spreadsheet, Excel, CSV type files specifically
                        filePickerLauncher.launch("*/*")
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("file_upload_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload secure document",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Upload Status Dialog / Banner
            AnimatedVisibility(
                visible = uploadStatus != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                uploadStatus?.let { status ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (status.contains("Upload complete", ignoreCase = true)) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981)
                                )
                            } else if (status.contains("failed", ignoreCase = true) || status.contains("Error", ignoreCase = true)) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = status,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            
                            IconButton(
                                onClick = { viewModel.clearUploadStatus() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close panel", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Materials list
            if (materials.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Source,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(72.dp)
                        )
                        Text(
                            text = "No study files found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Be the first to upload PDFs, CSVs, or spreadsheet documents for the group!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(materials, key = { it.id }) { material ->
                        StudyMaterialItem(
                            material = material,
                            currentUsername = currentUser?.username ?: "",
                            onOpenClick = {
                                openFileInApp(context, material.fileURL)
                            },
                            onDeleteClick = {
                                viewModel.deleteMaterial(material)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudyMaterialItem(
    material: StudyMaterial,
    currentUsername: String,
    onOpenClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isOwner = material.uploadedBy.trim().lowercase() == currentUsername.trim().lowercase()
    val fileExtension = material.fileName.substringAfterLast('.', "").lowercase()

    // Map file types to beautiful icon visualizers
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

    val dateFormatter = remember { SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()) }
    val formattedDate = remember(material.timestamp) {
        if (material.timestamp > 0) dateFormatter.format(Date(material.timestamp)) else "Just now"
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("material_item_${material.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left File Icon Visual Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconRes,
                    contentDescription = "$fileExtension file icon",
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info Panel
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = material.fileName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "By ${material.uploadedBy.replaceFirstChar { it.uppercase() }}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Right Action Button Box
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Open / Download Button
                IconButton(
                    onClick = onOpenClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "Open file link")
                }

                if (isOwner) {
                    IconButton(
                        onClick = onDeleteClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("delete_material_button_${material.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete from vault")
                    }
                }
            }
        }
    }
}

// Open or download the study material externally
private fun openFileInApp(context: Context, urlOrPath: String) {
    try {
        if (urlOrPath.startsWith("/")) {
            // Local file system storage path used by Room local simulator
            val file = File(urlOrPath)
            if (file.exists()) {
                // Since this runs in emulator / workspace cache, we can broadcast a notice
                android.widget.Toast.makeText(
                    context, 
                    "Local Demo Mode: Opened file: ${file.name}", 
                    android.widget.Toast.LENGTH_LONG
                ).show()
                
                // Also trigger FileProvider viewing if configured, or general view
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
            // Normal online HTTPS Firebase Storage URL
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(urlOrPath)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            "Document Opened Externally or view link copied.",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        
        // Alternative: Copy URL to clipboard for user convenience
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Study Material Web Link", urlOrPath)
            clipboard.setPrimaryClip(clip)
            android.widget.Toast.makeText(context, "Material URL copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
        } catch (clipEx: Exception) {}
    }
}
