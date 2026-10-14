package com.example.ui.library

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

sealed class LibraryActionState {
    object Idle : LibraryActionState()
    data class Importing(val message: String = "Importing and extracting text from PDF...") : LibraryActionState()
    data class Success(val message: String) : LibraryActionState()
    data class Warning(val message: String) : LibraryActionState()
    data class Error(val message: String) : LibraryActionState()
}

class PdfLibraryViewModel(private val container: AppContainer) : ViewModel() {

    val activeUser = container.userRepository.activeUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userPdfs: StateFlow<List<PdfEntity>> = activeUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList())
        else container.pdfRepository.getPrivatePdfs(user.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _actionState = MutableStateFlow<LibraryActionState>(LibraryActionState.Idle)
    val actionState: StateFlow<LibraryActionState> = _actionState.asStateFlow()

    fun importPdf(uri: Uri, customTitle: String? = null) {
        val user = activeUser.value ?: run {
            _actionState.value = LibraryActionState.Error("No active profile selected.")
            return
        }

        viewModelScope.launch {
            _actionState.value = LibraryActionState.Importing("Reading and extracting text from PDF...")
            when (val result = container.pdfImporter.importUserPdf(uri, user.id, customTitle)) {
                is ImportResult.Success -> {
                    _actionState.value = LibraryActionState.Success(result.message)
                }
                is ImportResult.OcrNeeded -> {
                    _actionState.value = LibraryActionState.Warning(result.message)
                }
                is ImportResult.Error -> {
                    _actionState.value = LibraryActionState.Error(result.message)
                }
            }
        }
    }

    fun deletePdf(pdfId: String) {
        val user = activeUser.value ?: return
        viewModelScope.launch {
            val success = container.pdfRepository.deletePdf(pdfId, user.id, user.isAdmin)
            if (success) {
                _actionState.value = LibraryActionState.Success("PDF and associated questions deleted.")
            } else {
                _actionState.value = LibraryActionState.Error("Could not delete PDF. Access denied.")
            }
        }
    }

    fun dismissMessage() {
        _actionState.value = LibraryActionState.Idle
    }
}
