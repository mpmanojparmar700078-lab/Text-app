package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attempts",
    indices = [
        Index("userId"),
        Index("pdfId")
    ]
)
data class AttemptEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val pdfId: String,
    val pdfTitleSnapshot: String,
    val questionCount: Int,
    val score: Int, // Percentage 0..100
    val correctCount: Int,
    val wrongCount: Int,
    val unansweredCount: Int,
    val startedAt: Long = System.currentTimeMillis(),
    val submittedAt: Long? = null,
    val status: String = "IN_PROGRESS" // "IN_PROGRESS" or "SUBMITTED"
)
