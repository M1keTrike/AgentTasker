package com.agentasker.features.tasks.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentasker.features.tasks.domain.entities.Task
import com.agentasker.features.tasks.domain.usecases.CreateTaskUseCase
import com.agentasker.features.tasks.domain.usecases.DeleteTaskUseCase
import com.agentasker.features.tasks.domain.usecases.GetTasksUseCase
import com.agentasker.features.tasks.domain.usecases.UpdateTaskUseCase
import com.agentasker.features.tasks.presentation.screens.TaskUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskViewModel(
    private val getTasksUseCase: GetTasksUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            getTasksUseCase().fold(
                onSuccess = { tasks ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        tasks = tasks,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error al cargar las tareas"
                    )
                }
            )
        }
    }

    fun createTask(title: String, description: String, priority: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            createTaskUseCase(title, description, priority).fold(
                onSuccess = {
                    loadTasks()
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

    fun updateTask(id: String, title: String?, description: String?, priority: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)


            val currentTasks = _uiState.value.tasks
            val updatedTasks = currentTasks.map { task ->
                if (task.id == id) {
                    task.copy(
                        title = title ?: task.title,
                        description = description ?: task.description,
                        priority = priority ?: task.priority
                    )
                } else {
                    task
                }
            }
            _uiState.value = _uiState.value.copy(
                tasks = updatedTasks,
                isLoading = false
            )

            updateTaskUseCase(id, title, description, priority).fold(
                onSuccess = {

                    loadTasks()
                },
                onFailure = { exception ->

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        tasks = currentTasks,
                        error = exception.message ?: "Error al actualizar la tarea"
                    )
                }
            )
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null)


            val currentTasks = _uiState.value.tasks
            _uiState.value = _uiState.value.copy(
                tasks = currentTasks.filter { it.id != id }
            )

            deleteTaskUseCase(id).fold(
                onSuccess = {

                    loadTasks()
                },
                onFailure = { exception ->

                    _uiState.value = _uiState.value.copy(
                        tasks = currentTasks,
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
            formPriority = "medium"
        )
    }

    fun showEditDialog(task: Task) {
        _uiState.value = _uiState.value.copy(
            showDialog = true,
            taskToEdit = task,
            formTitle = task.title,
            formDescription = task.description,
            formPriority = task.priority
        )
    }

    fun hideDialog() {
        _uiState.value = _uiState.value.copy(
            showDialog = false,
            taskToEdit = null,
            formTitle = "",
            formDescription = "",
            formPriority = "medium"
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
}

