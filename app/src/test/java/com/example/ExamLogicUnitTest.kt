package com.example

import com.example.ai.GeneratedMcq
import com.example.ai.QuestionParser
import com.example.ai.QuestionValidator
import com.example.domain.exam.ExamQuestion
import com.example.domain.exam.ExamScorer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExamLogicUnitTest {

    @Test
    fun calculateScore_perfectScore() {
        val questions = listOf(
            ExamQuestion("1", "Q1", listOf("A", "B", "C", "D"), 0, "Exp", 1, null, selectedIndex = 0),
            ExamQuestion("2", "Q2", listOf("A", "B", "C", "D"), 2, "Exp", 1, null, selectedIndex = 2)
        )
        val result = ExamScorer.calculateScore(questions)
        assertEquals(2, result.totalQuestions)
        assertEquals(2, result.correctAnswers)
        assertEquals(0, result.wrongAnswers)
        assertEquals(0, result.unanswered)
        assertEquals(100, result.scorePercentage)
    }

    @Test
    fun calculateScore_zeroDivisionPrevention() {
        val result = ExamScorer.calculateScore(emptyList())
        assertEquals(0, result.totalQuestions)
        assertEquals(0, result.correctAnswers)
        assertEquals(0, result.wrongAnswers)
        assertEquals(0, result.unanswered)
        assertEquals(0, result.scorePercentage)
    }

    @Test
    fun calculateScore_mixedAnswers() {
        val questions = listOf(
            ExamQuestion("1", "Q1", listOf("A", "B", "C", "D"), 1, "Exp", 1, null, selectedIndex = 1), // Correct
            ExamQuestion("2", "Q2", listOf("A", "B", "C", "D"), 2, "Exp", 1, null, selectedIndex = 0), // Wrong
            ExamQuestion("3", "Q3", listOf("A", "B", "C", "D"), 3, "Exp", 1, null, selectedIndex = null), // Unanswered
            ExamQuestion("4", "Q4", listOf("A", "B", "C", "D"), 0, "Exp", 1, null, selectedIndex = 0) // Correct
        )
        val result = ExamScorer.calculateScore(questions)
        assertEquals(4, result.totalQuestions)
        assertEquals(2, result.correctAnswers)
        assertEquals(1, result.wrongAnswers)
        assertEquals(1, result.unanswered)
        // 2 / 4 * 100 = 50%
        assertEquals(50, result.scorePercentage)
    }

    @Test
    fun questionValidation_validatesStructure() {
        val valid = GeneratedMcq(
            question = "What is the unit of resistance?",
            options = listOf("Volt", "Ampere", "Ohm", "Watt"),
            correctIndex = 2,
            explanation = "Resistance is measured in Ohms."
        )
        assertTrue(QuestionValidator.validateQuestion(valid))

        // Less than 4 options
        val invalidOptions = valid.copy(options = listOf("Volt", "Ampere", "Ohm"))
        assertFalse(QuestionValidator.validateQuestion(invalidOptions))

        // Index out of range
        val invalidIndex = valid.copy(correctIndex = 4)
        assertFalse(QuestionValidator.validateQuestion(invalidIndex))

        // Blank question
        val blankQ = valid.copy(question = "    ")
        assertFalse(QuestionValidator.validateQuestion(blankQ))

        // Duplicate options
        val duplicateOpts = valid.copy(options = listOf("Ohm", "Ohm", "Volt", "Ampere"))
        assertFalse(QuestionValidator.validateQuestion(duplicateOpts))
    }

    @Test
    fun questionParser_parsesFakeGeminiResponseInHindi() {
        val fakeAiResponse = """
            ```json
            {
              "questions": [
                {
                  "question": "विद्युत आग लगने पर किस प्रकार के अग्निशामक यंत्र का उपयोग किया जाना चाहिए?",
                  "options": ["जल प्रकार अग्निशामक", "कार्बन डाइऑक्साइड (CO2) / हैलॉन प्रकार", "फोम प्रकार", "गीला रासायनिक प्रकार"],
                  "correctIndex": 1,
                  "explanation": "विद्युत उपकरणों की आग बुझाने के लिए CO2 या CTC शुष्क पाउडर का उपयोग किया जाता है।",
                  "pageNumber": 1,
                  "topic": "विद्युत सुरक्षा"
                },
                {
                  "question": "ओम के नियमानुसार वोल्टेज का सूत्र क्या है?",
                  "options": ["V = I * R", "V = I / R", "V = R / I", "V = I² * R"],
                  "correctIndex": 0,
                  "explanation": "स्थिर तापमान पर वोल्टेज सीधे धारा और प्रतिरोध के गुणनफल के बराबर होता है।",
                  "pageNumber": 2,
                  "topic": "ओम का नियम"
                }
              ]
            }
            ```
        """.trimIndent()

        val parsed = QuestionParser.parseMcqs(fakeAiResponse)
        assertEquals(2, parsed.size)
        assertEquals("विद्युत आग लगने पर किस प्रकार के अग्निशामक यंत्र का उपयोग किया जाना चाहिए?", parsed[0].question)
        assertEquals(4, parsed[0].options.size)
        assertEquals("कार्बन डाइऑक्साइड (CO2) / हैलॉन प्रकार", parsed[0].options[1])
        assertEquals(1, parsed[0].correctIndex)
        assertEquals(1, parsed[0].pageNumber)
        assertTrue(QuestionValidator.validateQuestion(parsed[0]))
    }

    @Test
    fun duplicateQuestionFiltering() {
        val mcqs = listOf(
            GeneratedMcq("What is Ohm's Law?", listOf("A", "B", "C", "D"), 0, "Exp"),
            GeneratedMcq("what is ohm's law?", listOf("A", "B", "C", "D"), 0, "Exp"), // Duplicate
            GeneratedMcq("What is PMMC meter?", listOf("A", "B", "C", "D"), 1, "Exp")
        )

        val filtered = QuestionValidator.filterAndDeduplicate(mcqs, 10)
        assertEquals(2, filtered.size)
        assertEquals("What is Ohm's Law?", filtered[0].question)
        assertEquals("What is PMMC meter?", filtered[1].question)
    }
}
