package com.example.domain.exam

object ExamScorer {

    fun calculateScore(questions: List<ExamQuestion>): ExamScoreResult {
        val total = questions.size
        if (total == 0) {
            return ExamScoreResult(
                totalQuestions = 0,
                correctAnswers = 0,
                wrongAnswers = 0,
                unanswered = 0,
                scorePercentage = 0
            )
        }

        var correct = 0
        var wrong = 0
        var unanswered = 0

        for (q in questions) {
            when {
                q.selectedIndex == null -> unanswered++
                q.selectedIndex == q.correctIndex -> correct++
                else -> wrong++
            }
        }

        val percentage = ((correct.toDouble() / total.toDouble()) * 100).toInt()

        return ExamScoreResult(
            totalQuestions = total,
            correctAnswers = correct,
            wrongAnswers = wrong,
            unanswered = unanswered,
            scorePercentage = percentage
        )
    }

    fun calculateScoreFromSelections(
        correctIndices: List<Int>,
        userSelections: List<Int?>
    ): ExamScoreResult {
        val total = correctIndices.size
        if (total == 0) {
            return ExamScoreResult(0, 0, 0, 0, 0)
        }

        var correct = 0
        var wrong = 0
        var unanswered = 0

        for (i in correctIndices.indices) {
            val selected = userSelections.getOrNull(i)
            val expected = correctIndices[i]

            when {
                selected == null -> unanswered++
                selected == expected -> correct++
                else -> wrong++
            }
        }

        val percentage = ((correct.toDouble() / total.toDouble()) * 100).toInt()

        return ExamScoreResult(
            totalQuestions = total,
            correctAnswers = correct,
            wrongAnswers = wrong,
            unanswered = unanswered,
            scorePercentage = percentage
        )
    }
}
