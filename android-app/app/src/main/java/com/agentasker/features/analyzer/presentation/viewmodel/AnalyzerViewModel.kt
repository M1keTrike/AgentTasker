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

    /**
     * Encola el worker que hace OCR + DeepSeek y crea una nueva task con
     * sus subtasks. El worker corre como foreground + expedited, por lo
     * que el usuario puede cerrar la app y recibirá una notificación al
     * terminar. La UI solo muestra un mensaje y libera el estado.
     */
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
