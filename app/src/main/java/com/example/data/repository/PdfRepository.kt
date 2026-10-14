package com.example.data.repository

import com.example.data.local.PdfDao
import com.example.data.local.PdfEntity
import com.example.data.local.QuestionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.io.File

class PdfRepository(
    private val pdfDao: PdfDao,
    private val questionDao: QuestionDao
) {

    /**
     * Returns all PDFs accessible to the current user for taking exams.
     * This combines the user's private PDFs with published (visible) Admin PDFs.
     */
    fun getAvailablePdfsForExam(userId: String): Flow<List<PdfEntity>> {
        val privateFlow = pdfDao.getPrivatePdfsForUser(userId)
        val adminFlow = pdfDao.getVisibleAdminPdfs()
        return combine(privateFlow, adminFlow) { privateList, adminList ->
            privateList + adminList
        }
    }

    /**
     * Returns only the private PDFs owned by the user.
     */
    fun getPrivatePdfs(userId: String): Flow<List<PdfEntity>> {
        return pdfDao.getPrivatePdfsForUser(userId)
    }

    /**
     * Returns Admin library PDFs.
     * Only Admin can see hidden PDFs; non-admin only sees published ones.
     */
    fun getAdminLibraryPdfs(isAdmin: Boolean): Flow<List<PdfEntity>> {
        return if (isAdmin) {
            pdfDao.getAllAdminPdfs()
        } else {
            pdfDao.getVisibleAdminPdfs()
        }
    }

    /**
     * Enforces ownership validation when fetching a specific PDF.
     */
    fun getPdfForUser(pdfId: String, userId: String, isAdmin: Boolean): Flow<PdfEntity?> {
        return if (isAdmin) {
            // Admin can view any admin PDF or their own PDF
            pdfDao.getPdfForUser(pdfId, userId)
        } else {
            pdfDao.getPdfForUser(pdfId, userId)
        }
    }

    suspend fun getPdfDirect(pdfId: String, userId: String, isAdmin: Boolean): PdfEntity? {
        return if (isAdmin) {
            val adminPdf = pdfDao.getAdminPdfDirect(pdfId)
            adminPdf ?: pdfDao.getPdfForUserDirect(pdfId, userId)
        } else {
            pdfDao.getPdfForUserDirect(pdfId, userId)
        }
    }

    suspend fun toggleAdminPdfVisibility(pdfId: String, isVisible: Boolean, isAdmin: Boolean): Boolean {
        if (!isAdmin) return false
        pdfDao.updateAdminPdfVisibility(pdfId, isVisible)
        return true
    }

    suspend fun deletePdf(pdfId: String, userId: String, isAdmin: Boolean): Boolean {
        val pdf = pdfDao.getPdfByIdRaw(pdfId) ?: return false

        val deletedCount = if (pdf.sourceType == "ADMIN") {
            if (!isAdmin) return false // Forbidden
            pdfDao.deleteAdminPdf(pdfId)
        } else {
            if (pdf.ownerUserId != userId && !isAdmin) return false // User B cannot delete User A's PDF
            pdfDao.deleteUserPdf(pdfId, pdf.ownerUserId ?: userId)
        }

        if (deletedCount > 0) {
            // Delete generated questions for this PDF
            questionDao.deleteQuestionsForPdf(pdfId)

            // Safely delete physical file on disk
            try {
                val file = File(pdf.filePath)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
            return true
        }
        return false
    }
}
