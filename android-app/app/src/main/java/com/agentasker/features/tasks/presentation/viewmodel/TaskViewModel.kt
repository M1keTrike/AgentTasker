package com.agentasker.features.tasks.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentasker.core.hardware.HapticFeedbackManager
import com.agentasker.core.hardware.ReminderScheduler
import com.agentasker.features.tasks.data.workers.TaskSyncScheduler
import com.agentasker.features.tasks.domain.entities.Subtask
import com.agentasker.features.tasks.domain.entities.Task
import com.agentasker.features.tasks.domain.repositories.TaskReminderRepository
import com.agentasker.features.tasks.domain.repositories.TaskRepository
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
import java.time.Instant
import javax.inject.Inject

/**
 * Convierte un timestamp (millis desde epoch) a un string ISO 8601 en UTC,
 * formato aceptado por `@IsDateString()` del backend NestJS.
 *
 * Ejemplo: `1744409383353` → `"2026-04-11T20:09:43.353Z"`.
 */
private fun Long.toIsoString(): String = Instant.ofEpochMilli(this).toString()

/**
 * Parsea un string ISO 8601 a millis. Retorna null si el string es inválido.
 */
private fun String.toEpochMillisOrNull(): Long? = try {
    Instant.parse(this).toEpochMilli()
} catch (_: Exception) {
    null
}

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val hapticFeedbackManager: HapticFeedbackManager,
    private val reminderScheduler: ReminderScheduler,
    private val taskReminderRepository: TaskReminderRepository,
    private val taskRepository: TaskRepository,
    private val taskSyncScheduler: TaskSyncScheduler
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

            // Si el usuario puso un recordatorio desde el picker, eso se envía
            // como `dueDate` al backend para que el cron del server dispare el
            // push. El `reminderAt` sigue programando el AlarmManager local
            // como respaldo offline.
            val effectiveDueDate = dueDate ?: reminderAt?.toIsoString()

            createTaskUseCase(title, description, priority, status, effectiveDueDate).fold(
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

            // Misma unificación que en createTask. Si el usuario cambió el
            // reminder en el picker, el nuevo valor va como dueDate al backend
            // para que el cron lo vuelva a disparar con la fecha actualizada.
            val effectiveDueDate = dueDate ?: reminderAt?.toIsoString()

            updateTaskUseCase(id, title, description, priority, status, effectiveDueDate).fold(
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
        // Si la task ya viene del backend con un dueDate, lo usamos como
        // reminderAt inicial para que el picker muestre la fecha correcta.
        // Si no, intentamos leer el reminder local guardado en Room.
        val initialReminderFromDueDate = task.dueDate?.toEpochMillisOrNull()

        _uiState.value = _uiState.value.copy(
            showDialog = true,
            taskToEdit = task,
            formTitle = task.title,
            formDescription = task.description,
            formPriority = task.priority,
            formStatus = task.status,
            formDueDate = task.dueDate,
            formReminderAt = initialReminderFromDueDate
        )
        viewModelScope.launch {
            val localReminder = taskReminderRepository.getReminderAt(task.id)
            if (localReminder != null) {
                _uiState.value = _uiState.value.copy(formReminderAt = localReminder)
            }
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

    // ---------- Subtasks + IA ----------

    /**
     * Encola el worker que pide a DeepSeek que descomponga la task en
     * subtareas. El worker corre como foreground y sobrevive al kill de
     * la app. El usuario verá una notificación de progreso y luego otra
     * de éxito/fallo cuando termine. No bloqueamos la UI aquí.
     */
    fun splitWithAi(taskId: String) {
        taskSyncScheduler.scheduleAiSplit(taskId)
        _uiState.value = _uiState.value.copy(
            infoMessage = "Generando subtareas con IA… verás una notificación cuando termine."
        )
    }

    fun toggleSubtask(subtask: Subtask) {
        viewModelScope.launch {
            try {
                taskRepository.updateSubtask(
                    subtaskId = subtask.id,
                    isCompleted = !subtask.isCompleted
                )
                hapticFeedbackManager.success()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Error al actualizar la subtarea"
                )
            }
        }
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }
}
