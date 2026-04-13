package com.agentasker.features.dashboard.presentation.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentasker.features.classroom.data.services.ClassroomAuthService
import com.agentasker.features.classroom.domain.entities.ClassroomCourse
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

data class ClassroomIntegrationUiState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncCount: Int? = null,
    val error: String? = null,
    val infoMessage: String? = null,
    val courses: List<ClassroomCourse> = emptyList(),
    val selectedCourseIds: Set<String> = emptySet(),
    val showCoursePicker: Boolean = false,
    val isLoadingCourses: Boolean = false
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
                    if (connected && _uiState.value.courses.isEmpty()) {
                        loadCourses()
                    }
                },
                onFailure = { }
            )
        }
    }

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
                        infoMessage = "Classroom conectado. Elige los cursos a sincronizar."
                    )
                    loadCourses()
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

    fun loadCourses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingCourses = true)
            classroomRepository.getCourses().fold(
                onSuccess = { courses ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingCourses = false,
                        courses = courses,
                        selectedCourseIds = if (_uiState.value.selectedCourseIds.isEmpty())
                            courses.map { it.id }.toSet()
                        else
                            _uiState.value.selectedCourseIds
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingCourses = false,
                        error = error.message ?: "Error al cargar cursos"
                    )
                }
            )
        }
    }

    fun showCoursePicker() {
        if (_uiState.value.courses.isEmpty()) loadCourses()
        _uiState.value = _uiState.value.copy(showCoursePicker = true)
    }

    fun hideCoursePicker() {
        _uiState.value = _uiState.value.copy(showCoursePicker = false)
    }

    fun toggleCourse(courseId: String) {
        val current = _uiState.value.selectedCourseIds.toMutableSet()
        if (current.contains(courseId)) current.remove(courseId)
        else current.add(courseId)
        _uiState.value = _uiState.value.copy(selectedCourseIds = current)
    }

    fun selectAllCourses() {
        _uiState.value = _uiState.value.copy(
            selectedCourseIds = _uiState.value.courses.map { it.id }.toSet()
        )
    }

    fun deselectAllCourses() {
        _uiState.value = _uiState.value.copy(selectedCourseIds = emptySet())
    }

    fun syncSelectedCourses() {
        val ids = _uiState.value.selectedCourseIds.toList()
        if (ids.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                showCoursePicker = false,
                error = null
            )
            syncClassroomToTasksUseCase(ids).fold(
                onSuccess = { count ->
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        lastSyncCount = count,
                        infoMessage = "Se importaron $count tareas pendientes de ${ids.size} curso(s)."
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
