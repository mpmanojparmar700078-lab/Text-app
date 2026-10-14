package com.example.ui.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.AttemptEntity
import com.example.data.local.PdfEntity
import com.example.domain.exam.ExamQuestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ExamUiState(
    val attemptId: String = UUID.randomUUID().toString(),
    val pdf: PdfEntity? = null,
    val questions: List<ExamQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val submittedAttempt: AttemptEntity? = null,
    val errorMessage: String? = null
) {
    val currentQuestion: ExamQuestion? get() = questions.getOrNull(currentIndex)
    val totalQuestions: Int get() = questions.size
    val answeredCount: Int get() = questions.count { it.isAnswered }
    val progress: Float get() = if (totalQuestions > 0) (currentIndex + 1).toFloat() / totalQuestions else 0f
}

class ExamViewModel(
    private val container: AppContainer,
    private val pdfId: String,
    private val questionCount: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    init {
        loadExamQuestions()
    }

    private fun loadExamQuestions() {
        viewModelScope.launch {
            val user = container.userRepository.getActiveUserDirect()
            val userId = user?.id ?: ""
            val isAdmin = user?.isAdmin == true

            val pdf = container.pdfRepository.getPdfDirect(pdfId, userId, isAdmin)
            val allQuestions = container.questionRepository.getQuestionsForPdfDirect(pdfId, userId, isAdmin)

            // Select up to questionCount questions
            val selected = allQuestions.take(questionCount).map { q ->
                ExamQuestion(
                    id = q.id,
                    questionText = q.questionText,
                    options = q.getOptions(),
                    correctIndex = q.correctIndex,
                    explanation = q.explanation,
                    pageNumber = q.pageNumber,
                    topic = q.topic,
                    selectedIndex = null
                )
            }

            _uiState.value = _uiState.value.copy(
                pdf = pdf,
                questions = selected,
                currentIndex = 0
            )
        }
    }

    fun selectOption(optionIndex: Int) {
        val state = _uiState.value
        val currentQ = state.currentQuestion ?: return

        val updatedQuestions = state.questions.toMutableList()
        updatedQuestions[state.currentIndex] = currentQ.copy(selectedIndex = optionIndex)

        _uiState.value = state.copy(questions = updatedQuestions)
    }

    fun clearOption() {
        val state = _uiState.value
        val currentQ = state.currentQuestion ?: return

        val updatedQuestions = state.questions.toMutableList()
        updatedQuestions[state.currentIndex] = currentQ.copy(selectedIndex = null)

        _uiState.value = state.copy(questions = updatedQuestions)
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (state.currentIndex < state.totalQuestions - 1) {
            _uiState.value = state.copy(currentIndex = state.currentIndex + 1)
        }
    }

    fun previousQuestion() {
        val state = _uiState.value
        if (state.currentIndex > 0) {
            _uiState.value = state.copy(currentIndex = state.currentIndex - 1)
        }
    }

    fun jumpToQuestion(index: Int) {
        val state = _uiState.value
        if (index in 0 until state.totalQuestions) {
            _uiState.value = state.copy(currentIndex = index)
        }
    }

    fun submitExam(onSuccess: (attemptId: String) -> Unit) {
        val state = _uiState.value
        if (state.isSubmitting || state.isSubmitted) return

        val pdf = state.pdf ?: return
        viewModelScope.launch {
            _uiState.value = state.copy(isSubmitting = true)

            val user = container.userRepository.getActiveUserDirect()
            val userId = user?.id ?: ""

            val attempt = container.examRepository.submitExam(
                attemptId = state.attemptId,
                userId = userId,
                pdf = pdf,
                questions = state.questions
            )

            _uiState.value = _uiState.value.copy(
                isSubmitting = false,
                isSubmitted = true,
                submittedAttempt = attempt
            )

            onSuccess(attempt.id)
        }
    }
}
