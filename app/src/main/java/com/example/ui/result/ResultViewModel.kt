package com.example.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.domain.exam.AttemptDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResultViewModel(
    private val container: AppContainer,
    private val attemptId: String
) : ViewModel() {

    private val _attemptDetail = MutableStateFlow<AttemptDetail?>(null)
    val attemptDetail: StateFlow<AttemptDetail?> = _attemptDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadAttempt()
    }

    private fun loadAttempt() {
        viewModelScope.launch {
            val user = container.userRepository.getActiveUserDirect()
            val userId = user?.id ?: ""
            val detail = container.examRepository.getAttemptDetailDirect(attemptId, userId)
            _attemptDetail.value = detail
            _isLoading.value = false
        }
    }
}
