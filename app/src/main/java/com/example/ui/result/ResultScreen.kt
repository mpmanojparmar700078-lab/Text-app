package com.example.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AnswerEntity
import com.example.ui.components.ItiTopAppBar
import com.example.ui.components.StatCard
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ResultScreen(
    viewModel: ResultViewModel,
    onNavigateBackToHome: () -> Unit
) {
    val attemptDetail by viewModel.attemptDetail.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var showReviewList by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ItiTopAppBar(
                title = "Exam Result & Performance",
                canNavigateBack = true,
                onNavigateBack = onNavigateBackToHome
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (attemptDetail == null) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Result record not found or access denied.")
            }
        } else {
            val detail = attemptDetail!!
            val attempt = detail.attempt
            val answers = detail.answers

            val scoreColor = when {
                attempt.score >= 70 -> SuccessGreen
                attempt.score >= 40 -> WarningOrange
                else -> ErrorRed
            }

            val performanceTitle = when {
                attempt.score >= 80 -> "Outstanding Performance! 🏆"
                attempt.score >= 60 -> "Good Job! Passed ITI Benchmark 👍"
                attempt.score >= 40 -> "Average Score - Needs Practice 📖"
                else -> "Review Required - Study Key Topics ⚠️"
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Score Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("result_score_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = attempt.pdfTitleSnapshot,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Circular Score Badge
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(scoreColor.copy(alpha = 0.15f))
                                    .border(4.dp, scoreColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${attempt.score}%",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = scoreColor
                                    )
                                    Text(
                                        text = "SCORE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = scoreColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = performanceTitle,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = scoreColor
                            )
                        }
                    }
                }

                // Stats Breakdown
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Correct",
                            value = "${attempt.correctCount}",
                            icon = Icons.Default.CheckCircle,
                            color = SuccessGreen,
                            modifier = Modifier.weight(1f).testTag("result_stat_correct")
                        )
                        StatCard(
                            title = "Wrong",
                            value = "${attempt.wrongCount}",
                            icon = Icons.Default.Cancel,
                            color = ErrorRed,
                            modifier = Modifier.weight(1f).testTag("result_stat_wrong")
                        )
                        StatCard(
                            title = "Skipped",
                            value = "${attempt.unansweredCount}",
                            icon = Icons.Default.RemoveCircleOutline,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f).testTag("result_stat_unanswered")
                        )
                    }
                }

                // Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showReviewList = !showReviewList },
                            modifier = Modifier.weight(1f).testTag("toggle_review_button")
                        ) {
                            Icon(
                                imageVector = if (showReviewList) Icons.Default.ExpandLess else Icons.Default.Checklist,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (showReviewList) "Hide Review" else "Review Answers")
                        }

                        Button(
                            onClick = onNavigateBackToHome,
                            modifier = Modifier.weight(1f).testTag("return_home_button")
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Return Home")
                        }
                    }
                }

                // Detailed Review List
                if (showReviewList) {
                    item {
                        Text(
                            text = "Question-by-Question Review",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    itemsIndexed(answers) { idx, ans ->
                        ResultAnswerReviewCard(
                            index = idx + 1,
                            answer = ans
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
fun ResultAnswerReviewCard(
    index: Int,
    answer: AnswerEntity
) {
    val options = remember(answer.optionsJsonSnapshot) { answer.getOptions() }
    val isCorrect = answer.isCorrect
    val wasAnswered = answer.selectedIndex != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("result_review_q_$index"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Q$index",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when {
                                    isCorrect -> SuccessGreen.copy(alpha = 0.15f)
                                    !wasAnswered -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    else -> ErrorRed.copy(alpha = 0.15f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when {
                                isCorrect -> "CORRECT"
                                !wasAnswered -> "UNANSWERED"
                                else -> "WRONG"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isCorrect -> SuccessGreen
                                !wasAnswered -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> ErrorRed
                            }
                        )
                    }
                }

                if (answer.pageNumber != null) {
                    Text(
                        text = "Page ${answer.pageNumber}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = answer.questionTextSnapshot,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4 Options
            options.forEachIndexed { optIndex, optText ->
                val isSelectedByUser = answer.selectedIndex == optIndex
                val isExpectedCorrect = answer.correctIndexSnapshot == optIndex
                val prefix = ('A' + optIndex)

                val (optBg, optBorder, optTextColor) = when {
                    isExpectedCorrect -> Triple(
                        SuccessGreen.copy(alpha = 0.15f),
                        SuccessGreen,
                        SuccessGreen
                    )
                    isSelectedByUser && !isExpectedCorrect -> Triple(
                        ErrorRed.copy(alpha = 0.15f),
                        ErrorRed,
                        ErrorRed
                    )
                    else -> Triple(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        Color.Transparent,
                        MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(optBg)
                        .border(1.dp, optBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isExpectedCorrect -> SuccessGreen
                                    isSelectedByUser -> ErrorRed
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$prefix",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpectedCorrect || isSelectedByUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = optText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isExpectedCorrect || isSelectedByUser) FontWeight.Bold else FontWeight.Normal,
                        color = optTextColor,
                        modifier = Modifier.weight(1f)
                    )

                    if (isExpectedCorrect) {
                        Text(
                            text = "(Correct Answer)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    } else if (isSelectedByUser) {
                        Text(
                            text = "(Your Choice)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                    }
                }
            }

            // Explanation Box
            if (answer.explanationSnapshot.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Reference Explanation:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = answer.explanationSnapshot,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
