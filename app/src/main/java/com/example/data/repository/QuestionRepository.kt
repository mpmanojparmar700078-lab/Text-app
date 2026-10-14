package com.example.data.repository

import com.example.ai.GeneratedMcq
import com.example.data.local.QuestionDao
import com.example.data.local.QuestionEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import java.util.UUID

class QuestionRepository(
    private val questionDao: QuestionDao
) {

    fun getQuestionsForPdf(pdfId: String, userId: String): Flow<List<QuestionEntity>> {
        return questionDao.getQuestionsForAuthorizedPdf(pdfId, userId)
    }

    suspend fun getQuestionsForPdfDirect(pdfId: String, userId: String, isAdmin: Boolean): List<QuestionEntity> {
        return if (isAdmin) {
            val adminQs = questionDao.getQuestionsForAdminPdfDirect(pdfId)
            if (adminQs.isNotEmpty()) adminQs else questionDao.getQuestionsForAuthorizedPdfDirect(pdfId, userId)
        } else {
            questionDao.getQuestionsForAuthorizedPdfDirect(pdfId, userId)
        }
    }

    suspend fun saveGeneratedQuestions(
        pdfId: String,
        ownerUserId: String?,
        mcqs: List<GeneratedMcq>
    ) {
        val entities = mcqs.map { mcq ->
            val optionsJson = JSONArray(mcq.options).toString()
            QuestionEntity(
                id = UUID.randomUUID().toString(),
                pdfId = pdfId,
                ownerUserId = ownerUserId,
                questionText = mcq.question,
                optionsJson = optionsJson,
                correctIndex = mcq.correctIndex,
                explanation = mcq.explanation,
                pageNumber = mcq.pageNumber,
                topic = mcq.topic,
                approved = true
            )
        }
        questionDao.insertQuestions(entities)
    }

    suspend fun clearQuestionsForPdf(pdfId: String) {
        questionDao.deleteQuestionsForPdf(pdfId)
    }
}
