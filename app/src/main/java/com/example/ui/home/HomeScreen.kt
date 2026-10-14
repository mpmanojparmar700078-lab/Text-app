package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.PdfEntity
import com.example.ui.components.AdminPinDialog
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToStartExam: () -> Unit,
    onNavigateToMyPdfs: () -> Unit,
    onNavigateToAdminLibrary: () -> Unit,
    onNavigateToTestHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onSelectPdfForExam: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showAdminPinDialog by remember { mutableStateOf(false) }
    var pinErrorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("home_screen"),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Top App Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ITI PDF AI Exam",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Technical Exam Preparation with AI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Settings Icon
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // User Profile Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_profile_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.activeUser?.isAdmin == true) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = uiState.activeUser?.displayName ?: "Loading profile...",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (uiState.activeUser?.isAdmin == true) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (uiState.activeUser?.isAdmin == true) "ITI INSTRUCTOR / ADMIN" else "STUDENT PROFILE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (uiState.activeUser?.isAdmin == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // Switch Profile Button
                        OutlinedButton(
                            onClick = onNavigateToProfiles,
                            modifier = Modifier.testTag("switch_profile_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Switch", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Gemini API Status Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSettings() }
                        .testTag("gemini_status_banner"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isApiKeyConfigured) SuccessGreen.copy(alpha = 0.12f) else WarningOrange.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.isApiKeyConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (uiState.isApiKeyConfigured) SuccessGreen else WarningOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.isApiKeyConfigured) "Gemini AI Engine: Active" else "Gemini API Key Needed",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (uiState.isApiKeyConfigured) SuccessGreen else WarningOrange
                            )
                            Text(
                                text = if (uiState.isApiKeyConfigured) "Ready to generate exam questions from PDF theory." else "Tap to configure your API key in Settings.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "PDFs",
                        value = "${uiState.availablePdfsCount}",
                        icon = Icons.Default.PictureAsPdf,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f).testTag("stat_card_pdfs")
                    )
                    StatCard(
                        title = "Tests",
                        value = "${uiState.completedExamsCount}",
                        icon = Icons.Default.AssignmentTurnedIn,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f).testTag("stat_card_tests")
                    )
                    StatCard(
                        title = "Avg Score",
                        value = "${uiState.averageScore}%",
                        icon = Icons.Default.BarChart,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f).testTag("stat_card_avg_score")
                    )
                }
            }

            // Section Header
            item {
                Text(
                    text = "Exam Hub",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Navigation Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Start Exam Card
                    HomeActionCard(
                        title = "Start Exam",
                        description = "Choose an available PDF syllabus and start an AI mock test",
                        icon = Icons.Default.Quiz,
                        iconBg = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToStartExam,
                        testTag = "home_action_start_exam"
                    )

                    // My PDFs Card
                    HomeActionCard(
                        title = "My PDFs",
                        description = "Import private study materials and trade theory documents",
                        icon = Icons.Default.FolderShared,
                        iconBg = MaterialTheme.colorScheme.secondary,
                        onClick = onNavigateToMyPdfs,
                        testTag = "home_action_my_pdfs"
                    )

                    // Admin PDF Library Card (Visible to all, but requires Admin role or PIN verification)
                    HomeActionCard(
                        title = "Admin PDF Library",
                        description = if (uiState.activeUser?.isAdmin == true) "Manage official ITI trade PDFs & publish to students" else "Authorized instructor trade syllabus portal (PIN Protected)",
                        icon = Icons.Default.AccountBalance,
                        iconBg = MaterialTheme.colorScheme.tertiary,
                        badge = if (uiState.activeUser?.isAdmin == true) "Admin Mode" else "Instructor Only",
                        onClick = {
                            if (uiState.activeUser?.isAdmin == true) {
                                onNavigateToAdminLibrary()
                            } else {
                                showAdminPinDialog = true
                            }
                        },
                        testTag = "home_action_admin_library"
                    )

                    // Test History Card
                    HomeActionCard(
                        title = "Test History",
                        description = "Review past scores, correct answers, and question explanations",
                        icon = Icons.Default.HistoryEdu,
                        iconBg = Color(0xFF059669),
                        onClick = onNavigateToTestHistory,
                        testTag = "home_action_test_history"
                    )
                }
            }

            // Recent Ready PDFs
            if (uiState.recentPdfs.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ready for Practice",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = onNavigateToStartExam) {
                            Text("See All")
                        }
                    }
                }

                items(uiState.recentPdfs) { pdf ->
                    RecentPdfItemCard(
                        pdf = pdf,
                        onTakeExam = { onSelectPdfForExam(pdf.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAdminPinDialog) {
        AdminPinDialog(
            onDismiss = { showAdminPinDialog = false },
            onPinEntered = { pin ->
                scope.launch {
                    val authorized = viewModel.verifyAdminPin(pin)
                    if (authorized) {
                        showAdminPinDialog = false
                        onNavigateToAdminLibrary()
                    } else {
                        // Show error or toast
                    }
                }
            }
        )
    }
}

@Composable
fun HomeActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconBg: Color,
    badge: String? = null,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconBg.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconBg,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecentPdfItemCard(
    pdf: PdfEntity,
    onTakeExam: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTakeExam)
            .testTag("recent_pdf_${pdf.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pdf.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status = pdf.extractionStatus)
                    if (pdf.pageCount != null && pdf.pageCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${pdf.pageCount} pages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Button(
                onClick = onTakeExam,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("take_exam_btn_${pdf.id}")
            ) {
                Text("Take Exam", fontSize = 12.sp)
            }
        }
    }
}
