package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM pdfs WHERE sourceType = 'ADMIN' AND isVisible = 1 ORDER BY createdAt DESC")
    fun getVisibleAdminPdfs(): Flow<List<PdfEntity>>

    @Query("SELECT * FROM pdfs WHERE sourceType = 'ADMIN' AND isVisible = 1 ORDER BY createdAt DESC")
    suspend fun getVisibleAdminPdfsDirect(): List<PdfEntity>

    @Query("SELECT * FROM pdfs WHERE sourceType = 'ADMIN' ORDER BY createdAt DESC")
    fun getAllAdminPdfs(): Flow<List<PdfEntity>>

    @Query("SELECT * FROM pdfs WHERE ownerUserId = :userId AND sourceType = 'USER' ORDER BY createdAt DESC")
    fun getPrivatePdfsForUser(userId: String): Flow<List<PdfEntity>>

    @Query("SELECT * FROM pdfs WHERE ownerUserId = :userId AND sourceType = 'USER' ORDER BY createdAt DESC")
    suspend fun getPrivatePdfsForUserDirect(userId: String): List<PdfEntity>

    @Query("SELECT * FROM pdfs WHERE id = :pdfId AND (ownerUserId = :userId OR (sourceType = 'ADMIN' AND isVisible = 1)) LIMIT 1")
    fun getPdfForUser(pdfId: String, userId: String): Flow<PdfEntity?>

    @Query("SELECT * FROM pdfs WHERE id = :pdfId AND (ownerUserId = :userId OR (sourceType = 'ADMIN' AND isVisible = 1)) LIMIT 1")
    suspend fun getPdfForUserDirect(pdfId: String, userId: String): PdfEntity?

    @Query("SELECT * FROM pdfs WHERE id = :pdfId AND sourceType = 'ADMIN' LIMIT 1")
    suspend fun getAdminPdfDirect(pdfId: String): PdfEntity?

    @Query("SELECT * FROM pdfs WHERE id = :pdfId LIMIT 1")
    suspend fun getPdfByIdRaw(pdfId: String): PdfEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdf(pdf: PdfEntity)

    @Update
    suspend fun updatePdf(pdf: PdfEntity)

    @Query("UPDATE pdfs SET isVisible = :isVisible WHERE id = :pdfId AND sourceType = 'ADMIN'")
    suspend fun updateAdminPdfVisibility(pdfId: String, isVisible: Boolean)

    @Query("UPDATE pdfs SET extractionStatus = :status, pageCount = :pageCount, extractedText = :extractedText WHERE id = :pdfId")
    suspend fun updateExtractionResults(pdfId: String, status: String, pageCount: Int, extractedText: String)

    @Query("DELETE FROM pdfs WHERE id = :pdfId AND ownerUserId = :userId")
    suspend fun deleteUserPdf(pdfId: String, userId: String): Int

    @Query("DELETE FROM pdfs WHERE id = :pdfId AND sourceType = 'ADMIN'")
    suspend fun deleteAdminPdf(pdfId: String): Int
}
