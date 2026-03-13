package com.agentasker.features.kanban.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentasker.features.classroom.domain.repositories.ClassroomRepository
import com.agentasker.features.kanban.domain.entities.KanbanColumn
import com.agentasker.features.kanban.domain.entities.KanbanItem
import com.agentasker.features.kanban.domain.usecases.CreateKanbanColumnUseCase
import com.agentasker.features.kanban.domain.usecases.DeleteKanbanColumnUseCase
import com.agentasker.features.kanban.domain.usecases.ObserveKanbanColumnsUseCase
import com.agentasker.features.kanban.domain.usecases.UpdateKanbanColumnUseCase
import com.agentasker.features.kanban.domain.repositories.KanbanRepository
import com.agentasker.features.kanban.presentation.screens.KanbanUiState
import com.agentasker.features.tasks.domain.repositories.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KanbanViewModel @Inject constructor(
    private val observeColumnsUseCase: ObserveKanbanColumnsUseCase,
    private val createColumnUseCase: CreateKanbanColumnUseCase,
    private val updateColumnUseCase: UpdateKanbanColumnUseCase,
    private val deleteColumnUseCase: DeleteKanbanColumnUseCase,
    private val kanbanRepository: KanbanRepository,
    private val taskRepository: TaskRepository,
    private val classroomRepository: ClassroomRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KanbanUiState())
    val uiState: StateFlow<KanbanUiState> = _uiState.asStateFlow()

    init {
        observeKanbanData()
        refreshData()
    }

    private fun observeKanbanData() {
        viewModelScope.launch {
            combine(
                observeColumnsUseCase(),
                taskRepository.observeTasks()
            ) { columns, tasks ->
                val taskItems = tasks.map { KanbanItem.TaskItem(it) }
                val tasksByStatus = taskItems.groupBy { it.status }
                Pair(columns, tasksByStatus)
            }
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar el tablero"
                    )
                }
                .collect { (columns, tasksByStatus) ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        columns = columns,
                        tasksByStatus = tasksByStatus,
                        error = null
                    )
                }
        }

        loadClassroomTasks()
    }

    private fun loadClassroomTasks() {
        viewModelScope.launch {
            classroomRepository.getAllTasks().onSuccess { classroomTasks ->
                val classroomItems = classroomTasks.map { KanbanItem.ClassroomItem(it) }
                val currentMap = _uiState.value.tasksByStatus.toMutableMap()
                for (item in classroomItems) {
                    val list = currentMap.getOrDefault(item.status, emptyList()).toMutableList()
                    list.add(item)
                    currentMap[item.status] = list
                }
                _uiState.value = _uiState.value.copy(tasksByStatus = currentMap)
            }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                kanbanRepository.refreshColumns()
                taskRepository.refreshTasks()
            } catch (_: Exception) { }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun refresh() {
        refreshData()
        loadClassroomTasks()
    }

    fun showCreateColumnDialog() {
        _uiState.value = _uiState.value.copy(
            showColumnDialog = true,
            columnToEdit = null,
            formTitle = "",
            formStatusKey = "",
            formColor = null
        )
    }

    fun showEditColumnDialog(column: KanbanColumn) {
        _uiState.value = _uiState.value.copy(
            showColumnDialog = true,
            columnToEdit = column,
            formTitle = column.title,
            formStatusKey = column.statusKey,
            formColor = column.color
        )
    }

    fun hideColumnDialog() {
        _uiState.value = _uiState.value.copy(
            showColumnDialog = false,
            columnToEdit = null,
            formTitle = "",
            formStatusKey = "",
            formColor = null
        )
    }

    fun updateFormTitle(title: String) {
        _uiState.value = _uiState.value.copy(formTitle = title)
    }

    fun updateFormStatusKey(statusKey: String) {
        _uiState.value = _uiState.value.copy(formStatusKey = statusKey)
    }

    fun updateFormColor(color: String?) {
        _uiState.value = _uiState.value.copy(formColor = color)
    }

    fun saveColumn() {
        val state = _uiState.value
        viewModelScope.launch {
            try {
                if (state.columnToEdit != null) {
                    updateColumnUseCase(
                        id = state.columnToEdit.id,
                        title = state.formTitle,
                        statusKey = state.formStatusKey,
                        color = state.formColor
                    )
                } else {
                    createColumnUseCase(
                        title = state.formTitle,
                        statusKey = state.formStatusKey,
                        color = state.formColor
                    )
                }
                hideColumnDialog()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Error al guardar columna"
                )
            }
        }
    }

    fun deleteColumn(id: String) {
        viewModelScope.launch {
            try {
                deleteColumnUseCase(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Error al eliminar columna"
                )
            }
        }
    }

    fun moveTask(item: KanbanItem, newStatusKey: String) {
        viewModelScope.launch {
            try {
                when (item) {
                    is KanbanItem.TaskItem -> {
                        taskRepository.updateTask(
                            id = item.task.id,
                            title = null,
                            description = null,
                            priority = null,
                            status = newStatusKey
                        )
                    }
                    is KanbanItem.ClassroomItem -> { }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Error al mover tarea"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
