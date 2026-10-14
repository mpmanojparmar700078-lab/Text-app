package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val activeProfileName: String = "",
    val activeProfileRole: String = "USER",
    val customApiKey: String = "",
    val isApiKeyActive: Boolean = false,
    val language: String = "en",
    val message: String? = null,
    val isError: Boolean = false
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val user = container.userRepository.getActiveUserDirect()
            val customKey = container.preferences.geminiApiKeyFlow.firstOrNull() ?: ""
            val lang = container.preferences.appLanguageFlow.firstOrNull() ?: "en"
            val resolvedKey = container.geminiClient.resolveApiKey(customKey)

            _uiState.value = SettingsUiState(
                activeProfileName = user?.displayName ?: "Student",
                activeProfileRole = user?.role ?: "USER",
                customApiKey = customKey,
                isApiKeyActive = resolvedKey.isNotBlank(),
                language = lang
            )
        }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            container.preferences.setGeminiApiKey(key)
            val resolved = container.geminiClient.resolveApiKey(key)
            _uiState.value = _uiState.value.copy(
                customApiKey = key.trim(),
                isApiKeyActive = resolved.isNotBlank(),
                message = "Gemini API key saved successfully.",
                isError = false
            )
        }
    }

    fun changeAdminPin(oldPin: String, newPin: String) {
        viewModelScope.launch {
            if (newPin.trim().length < 4) {
                _uiState.value = _uiState.value.copy(
                    message = "Admin PIN must be at least 4 digits.",
                    isError = true
                )
                return@launch
            }

            val valid = container.userRepository.verifyAndAuthorizeAdmin(oldPin)
            if (!valid) {
                _uiState.value = _uiState.value.copy(
                    message = "Incorrect current Admin PIN. Verification failed.",
                    isError = true
                )
                return@launch
            }

            container.preferences.setAdminPin(newPin)
            _uiState.value = _uiState.value.copy(
                message = "Admin PIN updated successfully.",
                isError = false
            )
        }
    }

    fun changeLanguage(lang: String) {
        viewModelScope.launch {
            container.preferences.setAppLanguage(lang)
            _uiState.value = _uiState.value.copy(language = lang)
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
