package com.agentasker.features.tasks.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentasker.core.hardware.HapticFeedbackManager
import com.agentasker.core.hardware.ReminderScheduler
import com.agentasker.features.tasks.domain.entities.Task
import com.agentasker.features.tasks.domain.usecases.CreateTaskUseCase
import com.agentasker.features.tasks.domain.usecases.DeleteTaskUseCase
import com.agentasker.features.tasks.domain.usecases.GetTasksUseCase
import com.agentasker.features.tasks.domain.usecases.UpdateTaskUseCase
import com.agentasker.features.tasks.presentation.screens.TaskUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val hapticFeedbackManager: HapticFeedbackManager,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    init {
        observeTasks()
        refreshTasks()
    }

    private fun observeTasks() {
        viewModelScope.launch {
            getTasksUseCase()
                .onStart {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar las tareas"
                    )
                }
                .collect { tasks ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        tasks = tasks,
                        error = null
                    )
                }
        }
    }

    private fun refreshTasks() {
        viewModelScope.launch {
            try {
                getTasksUseCase.refresh()
            } catch (_: Exception) {
                // Offline: Room Flow already provides cached data
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun loadTasks() {
        refreshTasks()
    }

    fun createTask(title: String, description: String, priority: String, reminderAt: Long? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            createTaskUseCase(title, description, priority).fold(
                onSuccess = { task ->
                    if (reminderAt != null) {
                        reminderScheduler.scheduleReminder(
                            taskId = task.id,
                            title = "Recordatorio: $title",
                            body = description,
                            triggerAtMillis = reminderAt
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error al crear la tarea"
                    )
                }
            )
        }
    }

    fun updateTask(id: String, title: String?, description: String?, priority: String?, reminderAt: Long? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            updateTaskUseCase(id, title, description, priority).fold(
                onSuccess = {
                    hapticFeedbackManager.success()
                    reminderScheduler.cancelReminder(id)
                    if (reminderAt != null) {
                        reminderScheduler.scheduleReminder(
                            taskId = id,
                            title = "Recordatorio: ${title ?: "Tarea"}",
                            body = description ?: "",
                            triggerAtMillis = reminderAt
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error al actualizar la tarea"
                    )
                }
            )
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null)

            deleteTaskUseCase(id).fold(
                onSuccess = {
                    hapticFeedbackManager.warning()
                    reminderScheduler.cancelReminder(id)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Error al eliminar la tarea"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(
            showDialog = true,
            taskToEdit = null,
            formTitle = "",
            formDescription = "",
            formPriority = "medium",
            formReminderAt = null
        )
    }

    fun showEditDialog(task: Task) {
        _uiState.value = _uiState.value.copy(
            showDialog = true,
            taskToEdit = task,
            formTitle = task.title,
            formDescription = task.description,
            formPriority = task.priority,
            formReminderAt = task.reminderAt
        )
    }

    fun hideDialog() {
        _uiState.value = _uiState.value.copy(
            showDialog = false,
            taskToEdit = null,
            formTitle = "",
            formDescription = "",
            formPriority = "medium",
            formReminderAt = null
        )
    }

    fun updateFormTitle(title: String) {
        _uiState.value = _uiState.value.copy(formTitle = title)
    }

    fun updateFormDescription(description: String) {
        _uiState.value = _uiState.value.copy(formDescription = description)
    }

    fun updateFormPriority(priority: String) {
        _uiState.value = _uiState.value.copy(formPriority = priority)
    }

    fun updateFormReminderAt(reminderAt: Long?) {
        _uiState.value = _uiState.value.copy(formReminderAt = reminderAt)
    }
}
