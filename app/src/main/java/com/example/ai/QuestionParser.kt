package com.example.ai

import org.json.JSONArray
import org.json.JSONObject

object QuestionParser {

    fun parseMcqs(rawResponse: String): List<GeneratedMcq> {
        val result = mutableListOf<GeneratedMcq>()
        if (rawResponse.isBlank()) return emptyList()

        val cleanJson = extractJsonContent(rawResponse)

        try {
            if (cleanJson.startsWith("[")) {
                // Direct array
                val array = JSONArray(cleanJson)
                result.addAll(parseArray(array))
            } else if (cleanJson.startsWith("{")) {
                val obj = JSONObject(cleanJson)
                if (obj.has("questions")) {
                    val array = obj.getJSONArray("questions")
                    result.addAll(parseArray(array))
                } else if (obj.has("mcqs")) {
                    val array = obj.getJSONArray("mcqs")
                    result.addAll(parseArray(array))
                }
            }
        } catch (_: Exception) {
            // Attempt fallback extraction if enclosing tags are slightly malformed
            val match = Regex("""\[\s*\{.*\}\s*\]""", RegexOption.DOT_MATCHES_ALL).find(cleanJson)
            if (match != null) {
                try {
                    val array = JSONArray(match.value)
                    result.addAll(parseArray(array))
                } catch (_: Exception) {}
            }
        }

        return result
    }

    private fun parseArray(array: JSONArray): List<GeneratedMcq> {
        val list = mutableListOf<GeneratedMcq>()
        for (i in 0 until array.length()) {
            try {
                val item = array.getJSONObject(i)
                val question = item.optString("question", "").trim()
                val explanation = item.optString("explanation", "").trim()
                val correctIndex = item.optInt("correctIndex", -1)
                val pageNumber = if (item.has("pageNumber") && !item.isNull("pageNumber")) item.optInt("pageNumber") else null
                val topic = if (item.has("topic") && !item.isNull("topic")) item.optString("topic") else null

                val options = mutableListOf<String>()
                if (item.has("options")) {
                    val optArr = item.getJSONArray("options")
                    for (j in 0 until optArr.length()) {
                        options.add(optArr.getString(j).trim())
                    }
                }

                if (question.isNotEmpty() && options.isNotEmpty()) {
                    list.add(
                        GeneratedMcq(
                            question = question,
                            options = options,
                            correctIndex = correctIndex,
                            explanation = explanation.ifEmpty { "Refer to the source material." },
                            pageNumber = pageNumber,
                            topic = topic
                        )
                    )
                }
            } catch (_: Exception) {
                // Skip malformed individual question
            }
        }
        return list
    }

    private fun extractJsonContent(text: String): String {
        val trimmed = text.trim()
        val firstBrace = trimmed.indexOf('{')
        val firstBracket = trimmed.indexOf('[')

        val startIndex = when {
            firstBrace != -1 && firstBracket != -1 -> minOf(firstBrace, firstBracket)
            firstBrace != -1 -> firstBrace
            firstBracket != -1 -> firstBracket
            else -> 0
        }

        val lastBrace = trimmed.lastIndexOf('}')
        val lastBracket = trimmed.lastIndexOf(']')
        val endIndex = maxOf(lastBrace, lastBracket)

        return if (startIndex != -1 && endIndex != -1 && endIndex >= startIndex) {
            trimmed.substring(startIndex, endIndex + 1)
        } else {
            trimmed
        }
    }
}
