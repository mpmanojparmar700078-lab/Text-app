package com.example.ui.examsetup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ItiTopAppBar
import com.example.ui.components.StatusBadge
import com.example.ui.library.FeedbackCard
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@Composable
fun ExamSetupScreen(
    viewModel: ExamSetupViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPreview: (pdfId: String) -> Unit,
    onNavigateToExam: (pdfId: String, questionCount: Int) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val pdf by viewModel.pdf.collectAsStateWithLifecycle()
    val existingQuestions by viewModel.existingQuestions.collectAsStateWithLifecycle()
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()
    val selectedCount by viewModel.selectedQuestionCount.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()

    val countOptions = listOf(10, 20, 30, 40, 50)

    Scaffold(
        topBar = {
            ItiTopAppBar(
                title = "Exam Setup",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // PDF Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("exam_setup_pdf_info_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = pdf?.title ?: "Loading PDF...",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (pdf?.trade?.isNotBlank() == true) {
                                    Text(
                                        text = "${pdf?.trade} • ${pdf?.subject}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(status = pdf?.extractionStatus ?: "PENDING")
                            if (pdf?.pageCount != null) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "${pdf?.pageCount} Pages Available",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Generation Status / Progress
            when (val state = generationState) {
                is GenerationUiState.Generating -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = state.progressMessage,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                is GenerationUiState.Success -> {
                    item {
                        FeedbackCard(
                            title = "AI Questions Ready!",
                            message = if (state.generatedCount < state.requestedCount)
                                "Generated ${state.generatedCount} valid MCQs (PDF content supported ${state.generatedCount} questions without repetition)."
                            else
                                "Successfully generated ${state.generatedCount} structured MCQs from this PDF.",
                            color = SuccessGreen,
                            icon = Icons.Default.CheckCircle,
                            onDismiss = { viewModel.dismissState() }
                        )
                    }
                }
                is GenerationUiState.Error -> {
                    item {
                        Column {
                            FeedbackCard(
                                title = "Generation Notice",
                                message = state.message,
                                color = ErrorRed,
                                icon = Icons.Default.Error,
                                onDismiss = { viewModel.dismissState() }
                            )
                            if (state.message.contains("API key", ignoreCase = true)) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onNavigateToSettings,
                                    modifier = Modifier.fillMaxWidth().testTag("setup_go_to_settings_btn")
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open Settings to Configure API Key")
                                }
                            }
                        }
                    }
                }
                is GenerationUiState.Idle -> {}
            }

            // Question Count Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "1. Select Number of Questions",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            countOptions.forEach { count ->
                                FilterChip(
                                    selected = selectedCount == count,
                                    onClick = { viewModel.selectQuestionCount(count) },
                                    label = { Text("$count Qs") },
                                    modifier = Modifier.weight(1f).testTag("chip_count_$count")
                                )
                            }
                        }
                    }
                }
            }

            // Language Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "2. परीक्षा भाषा (Exam Language)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = selectedLanguage == "hi",
                                onClick = { viewModel.selectLanguage("hi") },
                                label = { Text("हिंदी (Mandatory Hindi)") },
                                leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f).testTag("chip_lang_hi")
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "सभी प्रश्न, चारों विकल्प और व्याख्या हिंदी भाषा में तैयार किए जाते हैं।",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // AI Generation Action
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini AI Question Generator",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Extracts topics, formulas, and safety rules from your PDF to formulate realistic ITI multiple-choice exam questions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.generateQuestionsWithAi() },
                            enabled = generationState !is GenerationUiState.Generating,
                            modifier = Modifier.fillMaxWidth().testTag("generate_questions_btn")
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (existingQuestions.isNotEmpty()) "Regenerate $selectedCount Questions" else "Generate $selectedCount Questions with Gemini")
                        }
                    }
                }
            }

            // Stored Questions Section
            if (existingQuestions.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Exam Ready (${existingQuestions.size} Questions)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Questions are stored locally and ready for practice.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { pdf?.let { onNavigateToPreview(it.id) } },
                                    modifier = Modifier.weight(1f).testTag("preview_questions_btn")
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Preview")
                                }

                                Button(
                                    onClick = {
                                        pdf?.let { onNavigateToExam(it.id, minOf(selectedCount, existingQuestions.size)) }
                                    },
                                    modifier = Modifier.weight(1f).testTag("start_exam_now_btn")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start Exam")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
