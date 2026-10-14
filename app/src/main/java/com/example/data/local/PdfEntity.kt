package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pdfs",
    indices = [
        Index("ownerUserId"),
        Index("sourceType"),
        Index("isVisible")
    ]
)
data class PdfEntity(
    @PrimaryKey val id: String,
    val title: String,
    val ownerUserId: String?, // Nullable for admin library
    val sourceType: String, // "ADMIN" or "USER"
    val isVisible: Boolean, // Visible to all users if ADMIN
    val filePath: String,
    val pageCount: Int?,
    val extractionStatus: String, // "PENDING", "EXTRACTED", "FAILED", "OCR_NEEDED"
    val subject: String = "",
    val trade: String = "",
    val topic: String = "",
    val description: String = "",
    val extractedText: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
