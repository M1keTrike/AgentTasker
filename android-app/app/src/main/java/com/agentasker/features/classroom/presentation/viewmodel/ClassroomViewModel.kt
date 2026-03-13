package com.agentasker.features.classroom.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentasker.features.classroom.domain.entities.ClassroomCourse
import com.agentasker.features.classroom.domain.entities.ClassroomTask
import com.agentasker.features.classroom.domain.usecases.ConnectClassroomUseCase
import com.agentasker.features.classroom.domain.usecases.GetClassroomCoursesUseCase
import com.agentasker.features.classroom.domain.usecases.GetClassroomTasksUseCase
import com.agentasker.features.login.data.datasources.local.SecureDataStoreTokenStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClassroomUiState(
    val isLoading: Boolean = true,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val courses: List<ClassroomCourse> = emptyList(),
    val tasks: List<ClassroomTask> = emptyList(),
    val selectedCourseId: String? = null,
    val error: String? = null,
    val needsReauth: Boolean = false
)

@HiltViewModel
class ClassroomViewModel @Inject constructor(
    private val getCoursesUseCase: GetClassroomCoursesUseCase,
    private val getTasksUseCase: GetClassroomTasksUseCase,
    private val connectClassroomUseCase: ConnectClassroomUseCase,
    private val secureTokenStorage: SecureDataStoreTokenStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClassroomUiState())
    val uiState: StateFlow<ClassroomUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            getCoursesUseCase().fold(
                onSuccess = { courses ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isConnected = true,
                        courses = courses
                    )
                    loadAllTasks()
                },
                onFailure = { error ->
                    val is401 = error.message?.contains("401") == true ||
                            error.message?.contains("Unauthorized") == true
                    if (is401) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isConnected = false,
                            needsReauth = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isConnected = false,
                            error = null
                        )
                    }
                }
            )
        }
    }

    private fun loadAllTasks() {
        viewModelScope.launch {
            getTasksUseCase().fold(
                onSuccess = { tasks ->
                    _uiState.value = _uiState.value.copy(tasks = tasks)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Error al cargar tareas de Classroom"
                    )
                }
            )
        }
    }

    fun loadTasksByCourse(courseId: String?) {
        _uiState.value = _uiState.value.copy(selectedCourseId = courseId)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            getTasksUseCase(courseId).fold(
                onSuccess = { tasks ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        tasks = tasks
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error al cargar tareas"
                    )
                }
            )
        }
    }

    fun onClassroomConnected(authorizationCode: String, codeVerifier: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true, error = null)

            val idToken = secureTokenStorage.getAuthToken()?.idToken
            if (idToken == null) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    error = "No se encontro token de sesion. Inicia sesion nuevamente."
                )
                return@launch
            }

            connectClassroomUseCase(idToken, authorizationCode, codeVerifier).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        isConnected = true
                    )
                    loadData()
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
