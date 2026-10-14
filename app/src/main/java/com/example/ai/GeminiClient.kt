package com.example.ai

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse
}

sealed class AiResult<out T> {
    data class Success<out T>(val data: T) : AiResult<T>()
    data class Error(val message: String, val canRetry: Boolean = true) : AiResult<Nothing>()
}

class GeminiClient {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Resolves the API key to use. Checks custom key first, then BuildConfig.GEMINI_API_KEY.
     */
    fun resolveApiKey(customKey: String?): String {
        val trimmedCustom = customKey?.trim().orEmpty()
        if (trimmedCustom.isNotBlank()) return trimmedCustom

        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
        return if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
    }

    suspend fun generateQuestions(
        apiKey: String,
        prompt: String
    ): AiResult<String> = withContext(Dispatchers.IO) {
        val validKey = apiKey.trim()
        if (validKey.isBlank() || validKey == "MY_GEMINI_API_KEY") {
            return@withContext AiResult.Error(
                "Gemini API key is missing or not configured. Please enter your API key in Settings or set it via AI Studio Secrets.",
                canRetry = false
            )
        }

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        try {
            val response = service.generateContent(validKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text.isNullOrBlank()) {
                AiResult.Error("Gemini AI returned an empty response. Please try again.")
            } else {
                AiResult.Success(text)
            }
        } catch (e: HttpException) {
            val code = e.code()
            val errorMessage = when (code) {
                400 -> "Invalid request or invalid Gemini API key. Please check your key in Settings."
                403 -> "Gemini API key is unauthorized or lacks permission. Check your Google AI Studio project."
                429 -> "Gemini API rate limit or quota exceeded. Please wait a moment and try again."
                500, 503 -> "Google Gemini server is currently busy or unavailable. Please retry shortly."
                else -> "Gemini API error (HTTP $code): ${e.message()}"
            }
            AiResult.Error(errorMessage, canRetry = code != 400 && code != 403)
        } catch (e: IOException) {
            AiResult.Error("No internet connection or network timed out. Please check your network and retry.", canRetry = true)
        } catch (e: Exception) {
            AiResult.Error("Unexpected error during question generation: ${e.localizedMessage ?: e.message}", canRetry = true)
        }
    }
}
