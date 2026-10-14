package com.example.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val users: List<UserEntity> = emptyList(),
    val activeUser: UserEntity? = null,
    val isCreatingProfile: Boolean = false,
    val feedbackMessage: String? = null
)

class ProfileViewModel(private val container: AppContainer) : ViewModel() {

    val allUsers: StateFlow<List<UserEntity>> = container.userRepository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeUser: StateFlow<UserEntity?> = container.userRepository.activeUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    fun switchProfile(targetUser: UserEntity, adminPin: String? = null, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (targetUser.isAdmin) {
                if (adminPin.isNullOrBlank()) {
                    onError("Admin PIN is required to switch to an Admin account.")
                    return@launch
                }
                val valid = container.userRepository.verifyAndAuthorizeAdmin(adminPin)
                if (!valid) {
                    onError("Incorrect Admin PIN. Access denied.")
                    return@launch
                }
            }

            container.userRepository.switchProfile(targetUser.id)
            _feedbackMessage.value = "Switched to ${targetUser.displayName}"
            onSuccess()
        }
    }

    fun createProfile(
        name: String,
        role: String,
        adminPin: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                onError("Please enter a valid display name.")
                return@launch
            }

            if (role.equals("ADMIN", ignoreCase = true)) {
                if (adminPin.isNullOrBlank()) {
                    onError("Admin PIN is required to create an Admin profile.")
                    return@launch
                }
                val valid = container.userRepository.verifyAndAuthorizeAdmin(adminPin)
                if (!valid) {
                    onError("Incorrect Admin PIN. Cannot create Admin profile.")
                    return@launch
                }
            }

            container.userRepository.createProfile(trimmedName, role)
            _feedbackMessage.value = "Profile created successfully."
            onSuccess()
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }
}
