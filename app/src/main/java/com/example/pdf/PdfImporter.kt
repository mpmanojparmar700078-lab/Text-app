package com.example.pdf

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.AppDatabase
import com.example.data.local.PdfEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

sealed class ImportResult {
    data class Success(val pdf: PdfEntity, val message: String) : ImportResult()
    data class OcrNeeded(val pdf: PdfEntity, val message: String) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

class PdfImporter(
    private val context: Context,
    private val database: AppDatabase,
    private val textExtractor: PdfTextExtractor
) {

    suspend fun importUserPdf(
        uri: Uri,
        userId: String,
        customTitle: String? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        val fileName = getDisplayNameFromUri(uri) ?: "document.pdf"
        val title = if (!customTitle.isNullOrBlank()) customTitle.trim() else fileName.removeSuffix(".pdf")
        val pdfId = UUID.randomUUID().toString()

        // Secure app-private directory isolated by userId
        val userDir = File(context.filesDir, "pdfs/$userId")
        if (!userDir.exists()) userDir.mkdirs()

        val destFile = File(userDir, "$pdfId.pdf")

        val copySuccess = copyUriToFile(uri, destFile)
        if (!copySuccess) {
            return@withContext ImportResult.Error("Failed to read and copy PDF from the selected storage location.")
        }

        // Extract text
        when (val extraction = textExtractor.extractText(destFile)) {
            is ExtractionResult.Success -> {
                val pdfEntity = PdfEntity(
                    id = pdfId,
                    title = title,
                    ownerUserId = userId,
                    sourceType = "USER",
                    isVisible = false, // Private to user
                    filePath = destFile.absolutePath,
                    pageCount = extraction.pageCount,
                    extractionStatus = "EXTRACTED",
                    extractedText = extraction.fullText
                )
                database.pdfDao().insertPdf(pdfEntity)
                ImportResult.Success(pdfEntity, "Successfully imported ${extraction.pageCount} pages.")
            }
            is ExtractionResult.OcrNeeded -> {
                val pdfEntity = PdfEntity(
                    id = pdfId,
                    title = title,
                    ownerUserId = userId,
                    sourceType = "USER",
                    isVisible = false,
                    filePath = destFile.absolutePath,
                    pageCount = extraction.pageCount,
                    extractionStatus = "OCR_NEEDED",
                    extractedText = ""
                )
                database.pdfDao().insertPdf(pdfEntity)
                ImportResult.OcrNeeded(pdfEntity, extraction.reason)
            }
            is ExtractionResult.Error -> {
                destFile.delete()
                ImportResult.Error(extraction.message)
            }
        }
    }

    suspend fun importAdminPdf(
        uri: Uri,
        title: String,
        subject: String,
        trade: String,
        topic: String,
        description: String,
        isVisible: Boolean
    ): ImportResult = withContext(Dispatchers.IO) {
        val fileName = getDisplayNameFromUri(uri) ?: "admin_doc.pdf"
        val pdfTitle = if (title.isNotBlank()) title.trim() else fileName.removeSuffix(".pdf")
        val pdfId = UUID.randomUUID().toString()

        val adminDir = File(context.filesDir, "pdfs/admin")
        if (!adminDir.exists()) adminDir.mkdirs()

        val destFile = File(adminDir, "$pdfId.pdf")

        val copySuccess = copyUriToFile(uri, destFile)
        if (!copySuccess) {
            return@withContext ImportResult.Error("Failed to copy PDF file into Admin library.")
        }

        when (val extraction = textExtractor.extractText(destFile)) {
            is ExtractionResult.Success -> {
                val pdfEntity = PdfEntity(
                    id = pdfId,
                    title = pdfTitle,
                    ownerUserId = null,
                    sourceType = "ADMIN",
                    isVisible = isVisible,
                    filePath = destFile.absolutePath,
                    pageCount = extraction.pageCount,
                    extractionStatus = "EXTRACTED",
                    subject = subject.trim(),
                    trade = trade.trim(),
                    topic = topic.trim(),
                    description = description.trim(),
                    extractedText = extraction.fullText
                )
                database.pdfDao().insertPdf(pdfEntity)
                ImportResult.Success(pdfEntity, "Admin PDF published successfully.")
            }
            is ExtractionResult.OcrNeeded -> {
                val pdfEntity = PdfEntity(
                    id = pdfId,
                    title = pdfTitle,
                    ownerUserId = null,
                    sourceType = "ADMIN",
                    isVisible = isVisible,
                    filePath = destFile.absolutePath,
                    pageCount = extraction.pageCount,
                    extractionStatus = "OCR_NEEDED",
                    subject = subject.trim(),
                    trade = trade.trim(),
                    topic = topic.trim(),
                    description = description.trim(),
                    extractedText = ""
                )
                database.pdfDao().insertPdf(pdfEntity)
                ImportResult.OcrNeeded(pdfEntity, extraction.reason)
            }
            is ExtractionResult.Error -> {
                destFile.delete()
                ImportResult.Error(extraction.message)
            }
        }
    }

    private fun copyUriToFile(uri: Uri, destFile: File): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.exists() && destFile.length() > 0L
        } catch (_: Exception) {
            false
        }
    }

    private fun getDisplayNameFromUri(uri: Uri): String? {
        var name: String? = null
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {}
        return name
    }
}
