package com.example.ai

object QuestionPrompt {

    fun buildPrompt(
        pdfTitle: String,
        contextText: String,
        questionCount: Int,
        language: String = "hi"
    ): String {
        return """
You are a senior ITI (Industrial Training Institute) technical examination creator.
Your task is to generate exactly $questionCount multiple-choice questions (MCQs) strictly based on the provided technical course material from the PDF document "$pdfTitle".

MANDATORY HINDI LANGUAGE REQUIREMENTS:
1. ALL QUESTIONS MUST BE STRICTLY IN HINDI (हिंदी भाषा).
2. ALL FOUR OPTIONS (चारों विकल्प) MUST BE IN HINDI.
3. THE EXPLANATION (व्याख्या) MUST BE IN CLEAR HINDI based strictly on the source material.
4. If the source PDF text is in English, translate the facts and concepts faithfully into Hindi without altering the meaning.
5. Standard ITI technical terms, trade names, units, and abbreviations (e.g., ओम (Ω), एम्पीयर (A), वोल्ट (V), PMMC, Moving Iron, Earthing, MCB, etc.) may be written in their standard Hindi transliteration or with technical terms in parentheses for student clarity.
6. Each question must have EXACTLY 4 distinct, unambiguous options.
7. Exactly ONE option must be correct.
8. "correctIndex" must be the 0-based integer index (0, 1, 2, or 3) of the correct option.
9. If [Page X] tags exist in the text, include the exact page number integer for "pageNumber".
10. Do NOT fabricate outside knowledge. Base every question solely on the provided text.
11. Output STRICT JSON ONLY. No markdown conversational text, no greetings.

Expected JSON format:
{
  "questions": [
    {
      "question": "विद्युत आग लगने पर किस प्रकार के अग्निशामक यंत्र का उपयोग किया जाना चाहिए?",
      "options": [
        "जल प्रकार अग्निशामक",
        "कार्बन डाइऑक्साइड (CO2) / हैलॉन प्रकार",
        "फोम (झाग) प्रकार",
        "गीला रासायनिक प्रकार"
      ],
      "correctIndex": 1,
      "explanation": "विद्युत उपकरणों पर लगी आग (वर्ग D) के लिए CO2 या CTC अग्निशामक का उपयोग किया जाता है। लाइव तारों पर कभी पानी नहीं डालना चाहिए।",
      "pageNumber": 1,
      "topic": "विद्युत सुरक्षा (Electrical Safety)"
    }
  ]
}

SOURCE MATERIAL:
$contextText
        """.trimIndent()
    }
}
