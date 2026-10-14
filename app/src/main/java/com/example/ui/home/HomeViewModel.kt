package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.AttemptEntity
import com.example.data.local.PdfEntity
import com.example.data.local.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val activeUser: UserEntity? = null,
    val availablePdfsCount: Int = 0,
    val completedExamsCount: Int = 0,
    val averageScore: Int = 0,
    val isApiKeyConfigured: Boolean = false,
    val recentPdfs: List<PdfEntity> = emptyList()
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    val activeUser: StateFlow<UserEntity?> = container.userRepository.activeUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val availablePdfs: StateFlow<List<PdfEntity>> = activeUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList<PdfEntity>())
        else container.pdfRepository.getAvailablePdfsForExam(user.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val userAttempts: StateFlow<List<AttemptEntity>> = activeUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList<AttemptEntity>())
        else container.examRepository.getAttemptsForUser(user.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<HomeUiState> = combine(
        activeUser,
        availablePdfs,
        userAttempts,
        container.preferences.geminiApiKeyFlow
    ) { user, pdfs, attempts, customKey ->
        val resolvedKey = container.geminiClient.resolveApiKey(customKey)
        val completed = attempts.filter { it.status == "SUBMITTED" }
        val avgScore = if (completed.isNotEmpty()) {
            completed.sumOf { it.score } / completed.size
        } else {
            0
        }

        HomeUiState(
            activeUser = user,
            availablePdfsCount = pdfs.size,
            completedExamsCount = completed.size,
            averageScore = avgScore,
            isApiKeyConfigured = resolvedKey.isNotBlank(),
            recentPdfs = pdfs.take(3)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    suspend fun verifyAdminPin(pin: String): Boolean {
        return container.userRepository.verifyAndAuthorizeAdmin(pin)
    }
}
