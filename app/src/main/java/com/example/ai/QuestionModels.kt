package com.example.ai

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val responseMimeType: String = "application/json",
    val temperature: Float = 0.2f
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContentResponse?
)

@JsonClass(generateAdapter = true)
data class GeminiContentResponse(
    val parts: List<GeminiPartResponse>?
)

@JsonClass(generateAdapter = true)
data class GeminiPartResponse(
    val text: String?
)

data class GeneratedMcq(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val pageNumber: Int? = null,
    val topic: String? = null
)
