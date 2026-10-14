package com.example.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.AttemptEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TestHistoryViewModel(private val container: AppContainer) : ViewModel() {

    val activeUser = container.userRepository.activeUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val attempts: StateFlow<List<AttemptEntity>> = activeUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList<AttemptEntity>())
        else container.examRepository.getAttemptsForUser(user.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun deleteAttempt(attemptId: String) {
        val user = activeUser.value ?: return
        viewModelScope.launch {
            val success = container.examRepository.deleteAttempt(attemptId, user.id)
            if (success) {
                _message.value = "Attempt deleted from history."
            }
        }
    }

    fun clearAllHistory() {
        val user = activeUser.value ?: return
        viewModelScope.launch {
            val success = container.examRepository.clearAllHistoryForUser(user.id)
            if (success) {
                _message.value = "All test history cleared for current profile."
            }
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
