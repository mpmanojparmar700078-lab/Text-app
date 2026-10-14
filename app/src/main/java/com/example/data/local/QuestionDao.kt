package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("""
        SELECT q.* FROM questions q 
        INNER JOIN pdfs p ON q.pdfId = p.id 
        WHERE q.pdfId = :pdfId 
          AND (p.ownerUserId = :userId OR (p.sourceType = 'ADMIN' AND p.isVisible = 1))
        ORDER BY q.pageNumber ASC, q.createdAt ASC
    """)
    fun getQuestionsForAuthorizedPdf(pdfId: String, userId: String): Flow<List<QuestionEntity>>

    @Query("""
        SELECT q.* FROM questions q 
        INNER JOIN pdfs p ON q.pdfId = p.id 
        WHERE q.pdfId = :pdfId 
          AND (p.ownerUserId = :userId OR (p.sourceType = 'ADMIN' AND p.isVisible = 1))
        ORDER BY q.pageNumber ASC, q.createdAt ASC
    """)
    suspend fun getQuestionsForAuthorizedPdfDirect(pdfId: String, userId: String): List<QuestionEntity>

    @Query("""
        SELECT q.* FROM questions q 
        INNER JOIN pdfs p ON q.pdfId = p.id 
        WHERE q.pdfId = :pdfId AND p.sourceType = 'ADMIN'
        ORDER BY q.pageNumber ASC, q.createdAt ASC
    """)
    suspend fun getQuestionsForAdminPdfDirect(pdfId: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE pdfId = :pdfId")
    suspend fun getQuestionsForPdfRaw(pdfId: String): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions WHERE pdfId = :pdfId")
    suspend fun deleteQuestionsForPdf(pdfId: String)
}
