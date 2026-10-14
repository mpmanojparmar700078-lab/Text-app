package com.example.domain.exam

import com.example.data.local.AnswerEntity
import com.example.data.local.AttemptEntity
import com.example.data.local.QuestionEntity

data class ExamQuestion(
    val id: String,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val pageNumber: Int?,
    val topic: String?,
    val selectedIndex: Int? = null
) {
    val isAnswered: Boolean get() = selectedIndex != null
    val isCorrect: Boolean get() = selectedIndex == correctIndex
}

data class ExamScoreResult(
    val totalQuestions: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val unanswered: Int,
    val scorePercentage: Int
)

data class AnswerReviewItem(
    val questionText: String,
    val options: List<String>,
    val selectedIndex: Int?,
    val correctIndex: Int,
    val explanation: String,
    val pageNumber: Int?,
    val isCorrect: Boolean
)

data class AttemptDetail(
    val attempt: AttemptEntity,
    val answers: List<AnswerEntity>
)
