package com.example.pdf

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed class ExtractionResult {
    data class Success(
        val fullText: String,
        val pageCount: Int,
        val pageChunks: List<PageTextChunk>
    ) : ExtractionResult()

    data class OcrNeeded(
        val pageCount: Int,
        val reason: String
    ) : ExtractionResult()

    data class Error(val message: String) : ExtractionResult()
}

class PdfTextExtractor(private val context: Context) {

    init {
        try {
            PDFBoxResourceLoader.init(context.applicationContext)
        } catch (_: Exception) {
            // Initialized once
        }
    }

    suspend fun extractText(pdfFile: File): ExtractionResult = withContext(Dispatchers.IO) {
        if (!pdfFile.exists() || pdfFile.length() == 0L) {
            return@withContext ExtractionResult.Error("PDF file does not exist or is empty.")
        }

        var document: PDDocument? = null
        try {
            document = PDDocument.load(pdfFile)
            val pageCount = document.numberOfPages

            if (document.isEncrypted) {
                return@withContext ExtractionResult.Error("This PDF is password-protected or encrypted.")
            }

            val stripper = PDFTextStripper()
            val pageChunks = mutableListOf<PageTextChunk>()
            val fullTextBuilder = StringBuilder()

            for (page in 1..pageCount) {
                stripper.startPage = page
                stripper.endPage = page
                val text = stripper.getText(document).trim()
                if (text.isNotEmpty()) {
                    pageChunks.add(PageTextChunk(page, text))
                    fullTextBuilder.append("[Page $page]\n").append(text).append("\n\n")
                }
            }

            val fullText = fullTextBuilder.toString().trim()

            // Check if document has sufficient readable text or is scanned/image-only
            val totalExtractedChars = fullText.length
            val avgCharsPerPage = if (pageCount > 0) totalExtractedChars / pageCount else 0

            if (totalExtractedChars < 100 || avgCharsPerPage < 25) {
                return@withContext ExtractionResult.OcrNeeded(
                    pageCount = pageCount,
                    reason = "This PDF appears to be a scanned document containing images with little or no digital text (Found $totalExtractedChars characters across $pageCount pages). OCR is required to extract text from scanned images."
                )
            }

            ExtractionResult.Success(
                fullText = fullText,
                pageCount = pageCount,
                pageChunks = pageChunks
            )
        } catch (e: Exception) {
            ExtractionResult.Error("Failed to extract text from PDF: ${e.localizedMessage ?: e.message}")
        } finally {
            try {
                document?.close()
            } catch (_: Exception) {}
        }
    }
}
