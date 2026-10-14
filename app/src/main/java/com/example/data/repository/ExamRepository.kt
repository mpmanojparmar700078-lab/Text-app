package com.example.data.repository

import com.example.data.local.AnswerDao
import com.example.data.local.AnswerEntity
import com.example.data.local.AttemptDao
import com.example.data.local.AttemptEntity
import com.example.data.local.PdfEntity
import com.example.domain.exam.AttemptDetail
import com.example.domain.exam.ExamQuestion
import com.example.domain.exam.ExamScorer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import java.util.UUID

class ExamRepository(
    private val attemptDao: AttemptDao,
    private val answerDao: AnswerDao
) {

    fun getAttemptsForUser(userId: String): Flow<List<AttemptEntity>> {
        return attemptDao.getAttemptsForUser(userId)
    }

    fun getAttemptDetail(attemptId: String, userId: String): Flow<AttemptDetail?> {
        val attemptFlow = attemptDao.getAttemptById(attemptId, userId)
        val answersFlow = answerDao.getAnswersForAttempt(attemptId, userId)

        return combine(attemptFlow, answersFlow) { attempt, answers ->
            if (attempt != null) {
                AttemptDetail(attempt, answers)
            } else {
                null
            }
        }
    }

    suspend fun getAttemptDetailDirect(attemptId: String, userId: String): AttemptDetail? {
        val attempt = attemptDao.getAttemptByIdDirect(attemptId, userId) ?: return null
        val answers = answerDao.getAnswersForAttemptDirect(attemptId, userId)
        return AttemptDetail(attempt, answers)
    }

    suspend fun submitExam(
        attemptId: String,
        userId: String,
        pdf: PdfEntity,
        questions: List<ExamQuestion>
    ): AttemptEntity {
        // Prevent duplicate submission
        val existing = attemptDao.getAttemptByIdDirect(attemptId, userId)
        if (existing != null && existing.status == "SUBMITTED") {
            return existing
        }

        val scoreResult = ExamScorer.calculateScore(questions)

        val attempt = AttemptEntity(
            id = attemptId,
            userId = userId,
            pdfId = pdf.id,
            pdfTitleSnapshot = pdf.title,
            questionCount = scoreResult.totalQuestions,
            score = scoreResult.scorePercentage,
            correctCount = scoreResult.correctAnswers,
            wrongCount = scoreResult.wrongAnswers,
            unansweredCount = scoreResult.unanswered,
            startedAt = existing?.startedAt ?: System.currentTimeMillis(),
            submittedAt = System.currentTimeMillis(),
            status = "SUBMITTED"
        )

        // Generate answers with snapshots of the question at time of taking exam
        val answerEntities = questions.map { q ->
            AnswerEntity(
                id = UUID.randomUUID().toString(),
                attemptId = attemptId,
                questionId = q.id,
                questionTextSnapshot = q.questionText,
                optionsJsonSnapshot = JSONArray(q.options).toString(),
                correctIndexSnapshot = q.correctIndex,
                explanationSnapshot = q.explanation,
                selectedIndex = q.selectedIndex,
                isCorrect = q.selectedIndex == q.correctIndex,
                pageNumber = q.pageNumber
            )
        }

        attemptDao.insertAttempt(attempt)
        answerDao.deleteAnswersForAttempt(attemptId)
        answerDao.insertAnswers(answerEntities)

        return attempt
    }

    suspend fun deleteAttempt(attemptId: String, userId: String): Boolean {
        val count = attemptDao.deleteAttempt(attemptId, userId)
        if (count > 0) {
            answerDao.deleteAnswersForAttempt(attemptId)
            return true
        }
        return false
    }

    suspend fun clearAllHistoryForUser(userId: String): Boolean {
        val count = attemptDao.deleteAllAttemptsForUser(userId)
        return count > 0
    }
}
