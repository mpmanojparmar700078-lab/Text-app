package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray

@Entity(
    tableName = "answers",
    indices = [
        Index("attemptId"),
        Index("questionId")
    ]
)
data class AnswerEntity(
    @PrimaryKey val id: String,
    val attemptId: String,
    val questionId: String,
    val questionTextSnapshot: String,
    val optionsJsonSnapshot: String,
    val correctIndexSnapshot: Int,
    val explanationSnapshot: String,
    val selectedIndex: Int?, // Nullable if skipped/unanswered
    val isCorrect: Boolean,
    val pageNumber: Int?
) {
    fun getOptions(): List<String> {
        return try {
            val jsonArray = JSONArray(optionsJsonSnapshot)
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
