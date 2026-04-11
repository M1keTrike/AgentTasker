package com.agentasker.features.dashboard.presentation.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentasker.features.classroom.data.services.ClassroomAuthService
import com.agentasker.features.classroom.domain.repositories.ClassroomRepository
import com.agentasker.features.classroom.domain.usecases.ConnectClassroomUseCase
import com.agentasker.features.classroom.domain.usecases.SyncClassroomToTasksUseCase
import com.agentasker.features.login.domain.repositories.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de la sección "Integraciones → Classroom" del Dashboard.
 */
data class ClassroomIntegrationUiState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncCount: Int? = null,
    val error: String? = null,
    val infoMessage: String? = null
)

@HiltViewModel
class ClassroomIntegrationViewModel @Inject constructor(
    private val classroomAuthService: ClassroomAuthService,
    private val connectClassroomUseCase: ConnectClassroomUseCase,
    private val syncClassroomToTasksUseCase: SyncClassroomToTasksUseCase,
    private val classroomRepository: ClassroomRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClassroomIntegrationUiState())
    val uiState: StateFlow<ClassroomIntegrationUiState> = _uiState.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            classroomRepository.isClassroomConnected().fold(
                onSuccess = { connected ->
                    _uiState.value = _uiState.value.copy(isConnected = connected)
                },
                onFailure = { /* si falla, dejamos el estado como estaba */ }
            )
        }
    }

    /**
     * Devuelve el Intent para abrir el consentimiento OAuth. El llamador lo
     * pasa a un `ActivityResultLauncher`. Al volver, se llama `onAuthResult`.
     */
    fun createAuthIntent(): Intent = classroomAuthService.createAuthIntent()

    fun onAuthResult(data: Intent) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true, error = null)

            val idToken = authRepository.getAuthToken()?.idToken
            if (idToken == null) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    error = "No se encontró token de sesión. Inicia sesión nuevamente."
                )
                return@launch
            }

            val authResult = try {
                classroomAuthService.handleAuthResponse(data)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    error = e.message ?: "No se pudo procesar la autenticación"
                )
                return@launch
            }

            connectClassroomUseCase(
                idToken = idToken,
                authorizationCode = authResult.authorizationCode,
                codeVerifier = authResult.codeVerifier
            ).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        isConnected = true,
                        infoMessage = "Classroom conectado. Puedes sincronizar tus tareas."
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        error = error.message ?: "Error al conectar Google Classroom"
                    )
                }
            )
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, error = null)
            syncClassroomToTasksUseCase().fold(
                onSuccess = { count ->
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        lastSyncCount = count,
                        infoMessage = "Se importaron $count tareas pendientes."
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        error = error.message ?: "Error sincronizando Classroom"
                    )
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, infoMessage = null)
    }
}
