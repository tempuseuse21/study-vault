package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontFamily
import com.example.viewmodel.MainViewModel

@Composable
fun ProfessionalHeader(
    username: String,
    useFirebase: Boolean,
    onLogoutClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_top_bar")
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "GOOD MORNING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Hello, $username 👋",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Live Portal connection pill
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Text(
                                text = "Live Portal",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }
                    }

                    // Logout icon / avatar
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable { onLogoutClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = username.take(1).uppercase(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentSection by viewModel.currentSection.collectAsState()
    val useFirebase by viewModel.useFirebase.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedChatPartner by viewModel.selectedChatPartner.collectAsState()

    val isChatOpen = (currentSection == "personal_chat" && selectedChatPartner != null)
    val usernameDisplay = currentUser?.username?.replaceFirstChar { it.uppercase() } ?: "User"

    Scaffold(
        topBar = {
            if (!isChatOpen) {
                ProfessionalHeader(
                    username = usernameDisplay,
                    useFirebase = useFirebase,
                    onLogoutClick = { viewModel.logout() }
                )
            }
        },
        bottomBar = {
            if (!isChatOpen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("dashboard_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = currentSection == "materials",
                        onClick = { viewModel.setSection("materials") },
                        icon = { Icon(Icons.Default.FolderZip, contentDescription = "Study Files Collection") },
                        label = { Text("Materials") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_materials")
                    )

                    NavigationBarItem(
                        selected = currentSection == "group_chat",
                        onClick = { viewModel.setSection("group_chat") },
                        icon = { Icon(Icons.Default.Groups, contentDescription = "Group Chat Feed") },
                        label = { Text("Group Chat") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_group_chat")
                    )

                    NavigationBarItem(
                        selected = currentSection == "personal_chat",
                        onClick = { viewModel.setSection("personal_chat") },
                        icon = { Icon(Icons.Default.Chat, contentDescription = "Personal Chat") },
                        label = { Text("Personal Chat") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_personal_chat")
                    )

                    NavigationBarItem(
                        selected = currentSection == "profile",
                        onClick = { viewModel.setSection("profile") },
                        icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile and Settings") },
                        label = { Text("Profile") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_profile")
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentSection,
            transitionSpec = {
                fadeIn() with fadeOut()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { section ->
            when (section) {
                "materials" -> StudyMaterialsTab(viewModel)
                "group_chat" -> GroupChatTab(viewModel)
                "personal_chat" -> PersonalChatTab(viewModel)
                "profile" -> ProfileTab(viewModel)
                else -> StudyMaterialsTab(viewModel)
            }
        }
    }
}
