package com.agentasker.features.tasks.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentasker.core.hardware.HapticFeedbackManager
import com.agentasker.core.hardware.ReminderScheduler
import com.agentasker.features.tasks.domain.entities.Task
import com.agentasker.features.tasks.domain.repositories.TaskReminderRepository
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
    private val reminderScheduler: ReminderScheduler,
    private val taskReminderRepository: TaskReminderRepository
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
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun loadTasks() {
        refreshTasks()
    }

    fun createTask(title: String, description: String, priority: String, status: String = "pending", dueDate: String? = null, reminderAt: Long? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            createTaskUseCase(title, description, priority, status, dueDate).fold(
                onSuccess = { task ->
                    if (reminderAt != null) {
                        taskReminderRepository.saveReminder(task.id, title, description, reminderAt)
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

    fun updateTask(id: String, title: String?, description: String?, priority: String?, status: String? = null, dueDate: String? = null, reminderAt: Long? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            updateTaskUseCase(id, title, description, priority, status, dueDate).fold(
                onSuccess = {
                    hapticFeedbackManager.success()
                    taskReminderRepository.deleteReminder(id)
                    reminderScheduler.cancelReminder(id)
                    if (reminderAt != null) {
                        taskReminderRepository.saveReminder(id, title ?: "Tarea", description ?: "", reminderAt)
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
                    taskReminderRepository.deleteReminder(id)
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
            formStatus = "pending",
            formDueDate = null,
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
            formStatus = task.status,
            formDueDate = task.dueDate,
            formReminderAt = null
        )
        viewModelScope.launch {
            val reminderAt = taskReminderRepository.getReminderAt(task.id)
            _uiState.value = _uiState.value.copy(formReminderAt = reminderAt)
        }
    }

    fun hideDialog() {
        _uiState.value = _uiState.value.copy(
            showDialog = false,
            taskToEdit = null,
            formTitle = "",
            formDescription = "",
            formPriority = "medium",
            formStatus = "pending",
            formDueDate = null,
            formReminderAt = null
        )
    }

    fun updateFormStatus(status: String) {
        _uiState.value = _uiState.value.copy(formStatus = status)
    }

    fun updateFormDueDate(dueDate: String?) {
        _uiState.value = _uiState.value.copy(formDueDate = dueDate)
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
