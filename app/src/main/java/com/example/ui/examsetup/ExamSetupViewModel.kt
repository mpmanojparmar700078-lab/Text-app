package com.example.ui.examsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.ai.AiResult
import com.example.ai.GeneratedMcq
import com.example.ai.QuestionParser
import com.example.ai.QuestionPrompt
import com.example.ai.QuestionValidator
import com.example.data.local.PdfEntity
import com.example.data.local.QuestionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class GenerationUiState {
    object Idle : GenerationUiState()
    data class Generating(val progressMessage: String) : GenerationUiState()
    data class Success(val generatedCount: Int, val requestedCount: Int) : GenerationUiState()
    data class Error(val message: String, val canRetry: Boolean = true) : GenerationUiState()
}

class ExamSetupViewModel(
    private val container: AppContainer,
    private val pdfId: String
) : ViewModel() {

    val activeUser = container.userRepository.activeUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _pdf = MutableStateFlow<PdfEntity?>(null)
    val pdf: StateFlow<PdfEntity?> = _pdf.asStateFlow()

    private val _existingQuestions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    val existingQuestions: StateFlow<List<QuestionEntity>> = _existingQuestions.asStateFlow()

    private val _generationState = MutableStateFlow<GenerationUiState>(GenerationUiState.Idle)
    val generationState: StateFlow<GenerationUiState> = _generationState.asStateFlow()

    private val _selectedQuestionCount = MutableStateFlow(10)
    val selectedQuestionCount: StateFlow<Int> = _selectedQuestionCount.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("hi") // Mandatory Hindi language
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    init {
        loadPdfAndQuestions()
    }

    private fun loadPdfAndQuestions() {
        viewModelScope.launch {
            val user = container.userRepository.getActiveUserDirect()
            val userId = user?.id ?: ""
            val isAdmin = user?.isAdmin == true

            val loadedPdf = container.pdfRepository.getPdfDirect(pdfId, userId, isAdmin)
            _pdf.value = loadedPdf

            if (loadedPdf != null) {
                val questions = container.questionRepository.getQuestionsForPdfDirect(pdfId, userId, isAdmin)
                _existingQuestions.value = questions
            }
        }
    }

    fun selectQuestionCount(count: Int) {
        _selectedQuestionCount.value = count
    }

    fun selectLanguage(lang: String) {
        _selectedLanguage.value = lang
    }

    fun generateQuestionsWithAi() {
        val currentPdf = _pdf.value ?: run {
            _generationState.value = GenerationUiState.Error("PDF details could not be found.")
            return
        }

        if (currentPdf.extractedText.isBlank()) {
            _generationState.value = GenerationUiState.Error(
                if (currentPdf.extractionStatus == "OCR_NEEDED")
                    "This PDF contains scanned images without digital text. OCR is needed to extract text."
                else
                    "Extracted text is empty. Please verify the PDF contains readable text."
            )
            return
        }

        viewModelScope.launch {
            _generationState.value = GenerationUiState.Generating("Preparing syllabus content...")

            val customKey = container.preferences.geminiApiKeyFlow.firstOrNull()
            val apiKey = container.geminiClient.resolveApiKey(customKey)

            if (apiKey.isBlank()) {
                _generationState.value = GenerationUiState.Error(
                    "Gemini API key is not configured. Please open Settings to set your Gemini API key.",
                    canRetry = false
                )
                return@launch
            }

            _generationState.value = GenerationUiState.Generating("Prompting Gemini AI to generate ${_selectedQuestionCount.value} MCQs...")

            val prompt = QuestionPrompt.buildPrompt(
                pdfTitle = currentPdf.title,
                contextText = currentPdf.extractedText.take(12000), // bounded context window
                questionCount = _selectedQuestionCount.value,
                language = _selectedLanguage.value
            )

            when (val aiResponse = container.geminiClient.generateQuestions(apiKey, prompt)) {
                is AiResult.Success -> {
                    _generationState.value = GenerationUiState.Generating("Validating question structures and answer keys...")
                    val parsedMcqs = QuestionParser.parseMcqs(aiResponse.data)
                    val validated = QuestionValidator.filterAndDeduplicate(parsedMcqs, _selectedQuestionCount.value)

                    if (validated.isEmpty()) {
                        _generationState.value = GenerationUiState.Error(
                            "AI generated questions could not be validated. Please retry.",
                            canRetry = true
                        )
                    } else {
                        val user = container.userRepository.getActiveUserDirect()
                        // Replace previous questions
                        container.questionRepository.clearQuestionsForPdf(currentPdf.id)
                        container.questionRepository.saveGeneratedQuestions(
                            pdfId = currentPdf.id,
                            ownerUserId = if (currentPdf.sourceType == "ADMIN") null else user?.id,
                            mcqs = validated
                        )

                        // Reload questions
                        val refreshed = container.questionRepository.getQuestionsForPdfDirect(
                            pdfId = currentPdf.id,
                            userId = user?.id ?: "",
                            isAdmin = user?.isAdmin == true
                        )
                        _existingQuestions.value = refreshed

                        _generationState.value = GenerationUiState.Success(
                            generatedCount = validated.size,
                            requestedCount = _selectedQuestionCount.value
                        )
                    }
                }
                is AiResult.Error -> {
                    _generationState.value = GenerationUiState.Error(
                        message = aiResponse.message,
                        canRetry = aiResponse.canRetry
                    )
                }
            }
        }
    }

    fun dismissState() {
        _generationState.value = GenerationUiState.Idle
    }
}
