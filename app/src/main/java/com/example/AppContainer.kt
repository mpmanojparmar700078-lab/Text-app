package com.example

import android.content.Context
import com.example.ai.GeminiClient
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.ExamRepository
import com.example.data.repository.PdfRepository
import com.example.data.repository.QuestionRepository
import com.example.data.repository.UserRepository
import com.example.pdf.PdfImporter
import com.example.pdf.PdfTextExtractor

class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.getDatabase(context)
    val preferences: UserPreferences = UserPreferences(context)

    val userRepository: UserRepository by lazy {
        UserRepository(database.userDao(), preferences)
    }

    val questionRepository: QuestionRepository by lazy {
        QuestionRepository(database.questionDao())
    }

    val pdfRepository: PdfRepository by lazy {
        PdfRepository(database.pdfDao(), database.questionDao())
    }

    val examRepository: ExamRepository by lazy {
        ExamRepository(database.attemptDao(), database.answerDao())
    }

    val textExtractor: PdfTextExtractor by lazy {
        PdfTextExtractor(context)
    }

    val pdfImporter: PdfImporter by lazy {
        PdfImporter(context, database, textExtractor)
    }

    val geminiClient: GeminiClient by lazy {
        GeminiClient()
    }
}
