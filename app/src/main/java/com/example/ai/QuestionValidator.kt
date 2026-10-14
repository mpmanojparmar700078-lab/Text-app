package com.example.ai

object QuestionValidator {

    fun validateQuestion(mcq: GeneratedMcq): Boolean {
        if (mcq.question.isBlank() || mcq.question.length < 5) return false
        if (mcq.options.size != 4) return false
        if (mcq.options.any { it.isBlank() }) return false
        if (mcq.options.toSet().size != 4) return false // Ensure 4 unique options
        if (mcq.correctIndex !in 0..3) return false
        if (mcq.explanation.isBlank()) return false
        return true
    }

    /**
     * Filters a list of MCQs to retain only valid, unique questions.
     */
    fun filterAndDeduplicate(mcqs: List<GeneratedMcq>, targetCount: Int): List<GeneratedMcq> {
        val uniqueQuestions = mutableListOf<GeneratedMcq>()
        val seenTexts = mutableSetOf<String>()

        for (item in mcqs) {
            if (!validateQuestion(item)) continue

            val normalized = item.question.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
            if (seenTexts.contains(normalized)) continue

            seenTexts.add(normalized)
            uniqueQuestions.add(item)

            if (uniqueQuestions.size >= targetCount) break
        }

        return uniqueQuestions
    }
}
