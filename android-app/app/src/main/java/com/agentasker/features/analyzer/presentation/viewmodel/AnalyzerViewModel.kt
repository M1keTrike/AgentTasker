package com.agentasker.features.analyzer.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.agentasker.features.tasks.data.workers.TaskSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class AnalyzerUiState(
    val selectedImageUri: Uri? = null,
    val isAnalyzing: Boolean = false,
    val infoMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class AnalyzerViewModel @Inject constructor(
    private val taskSyncScheduler: TaskSyncScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyzerUiState())
    val uiState: StateFlow<AnalyzerUiState> = _uiState.asStateFlow()

    fun setImageUri(uri: Uri?) {
        _uiState.value = _uiState.value.copy(selectedImageUri = uri, error = null)
    }

    fun analyzeSelectedImage() {
        val uri = _uiState.value.selectedImageUri ?: return
        taskSyncScheduler.scheduleImageAnalysis(uri)
        _uiState.value = _uiState.value.copy(
            isAnalyzing = false,
            infoMessage = "Analizando imagen en segundo plano. Te notificaremos cuando termine.",
            selectedImageUri = null
        )
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
