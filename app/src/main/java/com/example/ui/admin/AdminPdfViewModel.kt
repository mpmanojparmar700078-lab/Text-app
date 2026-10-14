package com.example.ui.admin

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.local.PdfEntity
import com.example.pdf.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AdminActionState {
    object Idle : AdminActionState()
    data class Loading(val message: String) : AdminActionState()
    data class Success(val message: String) : AdminActionState()
    data class Error(val message: String) : AdminActionState()
}

class AdminPdfViewModel(private val container: AppContainer) : ViewModel() {

    val activeUser = container.userRepository.activeUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val adminPdfs: StateFlow<List<PdfEntity>> = activeUser.flatMapLatest { user ->
        val isAdmin = user?.isAdmin == true
        container.pdfRepository.getAdminLibraryPdfs(isAdmin)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _actionState = MutableStateFlow<AdminActionState>(AdminActionState.Idle)
    val actionState: StateFlow<AdminActionState> = _actionState.asStateFlow()

    fun toggleVisibility(pdfId: String, currentVisibility: Boolean) {
        val user = activeUser.value
        if (user?.isAdmin != true) {
            _actionState.value = AdminActionState.Error("Unauthorized: Only an Admin can change PDF visibility.")
            return
        }

        viewModelScope.launch {
            val newVisibility = !currentVisibility
            val success = container.pdfRepository.toggleAdminPdfVisibility(pdfId, newVisibility, true)
            if (success) {
                _actionState.value = AdminActionState.Success(
                    if (newVisibility) "PDF is now published and visible to all student profiles."
                    else "PDF is now hidden from student profiles."
                )
            } else {
                _actionState.value = AdminActionState.Error("Failed to update PDF visibility.")
            }
        }
    }

    fun importAdminPdf(
        uri: Uri,
        title: String,
        subject: String,
        trade: String,
        topic: String,
        description: String,
        isVisible: Boolean
    ) {
        val user = activeUser.value
        if (user?.isAdmin != true) {
            _actionState.value = AdminActionState.Error("Unauthorized: Only Admin can publish to the Admin Library.")
            return
        }

        viewModelScope.launch {
            _actionState.value = AdminActionState.Loading("Importing and publishing Admin syllabus PDF...")
            when (val result = container.pdfImporter.importAdminPdf(uri, title, subject, trade, topic, description, isVisible)) {
                is ImportResult.Success -> {
                    _actionState.value = AdminActionState.Success("Admin PDF published successfully.")
                }
                is ImportResult.OcrNeeded -> {
                    _actionState.value = AdminActionState.Success("PDF saved to Admin library, but scanned images were detected (OCR needed).")
                }
                is ImportResult.Error -> {
                    _actionState.value = AdminActionState.Error(result.message)
                }
            }
        }
    }

    fun deleteAdminPdf(pdfId: String) {
        val user = activeUser.value
        if (user?.isAdmin != true) {
            _actionState.value = AdminActionState.Error("Unauthorized: Only Admin can delete library PDFs.")
            return
        }

        viewModelScope.launch {
            val success = container.pdfRepository.deletePdf(pdfId, user.id, true)
            if (success) {
                _actionState.value = AdminActionState.Success("Admin PDF deleted.")
            } else {
                _actionState.value = AdminActionState.Error("Failed to delete Admin PDF.")
            }
        }
    }

    fun dismissMessage() {
        _actionState.value = AdminActionState.Idle
    }
}
