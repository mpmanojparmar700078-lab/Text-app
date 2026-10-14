package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray

@Entity(
    tableName = "questions",
    indices = [
        Index("pdfId"),
        Index("ownerUserId")
    ]
)
data class QuestionEntity(
    @PrimaryKey val id: String,
    val pdfId: String,
    val ownerUserId: String?,
    val questionText: String,
    val optionsJson: String, // Stored as JSON array string: ["A", "B", "C", "D"]
    val correctIndex: Int, // 0..3
    val explanation: String,
    val pageNumber: Int? = null,
    val topic: String? = null,
    val approved: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getOptions(): List<String> {
        return try {
            val jsonArray = JSONArray(optionsJson)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
