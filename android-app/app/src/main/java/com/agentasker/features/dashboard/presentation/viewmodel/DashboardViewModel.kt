package com.agentasker.features.dashboard.presentation.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentasker.features.classroom.domain.entities.ClassroomTask
import com.agentasker.features.classroom.domain.entities.SubmissionState
import com.agentasker.features.classroom.domain.usecases.GetClassroomTasksUseCase
import com.agentasker.features.tasks.domain.entities.Task
import com.agentasker.features.tasks.domain.repositories.TaskRepository
import com.agentasker.features.tasks.domain.usecases.GetTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class CourseInfo(
    val name: String,
    val pendingCount: Int
)

data class UpcomingItem(
    val title: String,
    val courseName: String?,
    val dueDate: LocalDateTime?,
    val isClassroom: Boolean
)

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(
        val pendingCount: Int,
        val completedCount: Int,
        val dueSoonCount: Int,
        val upcomingDeadlines: List<UpcomingItem>,
        val highPriorityCount: Int,
        val mediumPriorityCount: Int,
        val lowPriorityCount: Int,
        val activeCourses: List<CourseInfo>,
        val archivedTasks: List<Task> = emptyList()
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val getClassroomTasksUseCase: GetClassroomTasksUseCase,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun refresh() {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            try {
                getTasksUseCase.refresh()
            } catch (_: Exception) { }

            // Combinamos el flow de tasks activas con el de archivadas
            // para que la sección "Archivados" del Dashboard se actualice
            // en vivo cuando el usuario archive o restaure una task.
            combine(
                getTasksUseCase(),
                taskRepository.observeArchivedTasks()
            ) { tasks, archived ->
                tasks to archived
            }.collect { (tasks, archived) ->
                val classroomTasks = try {
                    getClassroomTasksUseCase().getOrDefault(emptyList())
                } catch (_: Exception) {
                    emptyList()
                }
                _uiState.value = buildSuccessState(tasks, classroomTasks, archived)
            }
        }
    }

    /**
     * Elimina permanentemente una task archivada. No hay papelera — este
     * es el borrado definitivo que el usuario pide desde el Dashboard.
     */
    fun deleteArchivedPermanently(taskId: String) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(taskId)
            } catch (_: Exception) { }
        }
    }

    /**
     * Desarchiva una task — vuelve al tab Tareas.
     */
    fun restoreArchived(taskId: String) {
        viewModelScope.launch {
            try {
                taskRepository.updateTask(
                    id = taskId,
                    title = null,
                    description = null,
                    priority = null,
                    status = "pending",
                    dueDate = null,
                    isArchived = false
                )
            } catch (_: Exception) { }
        }
    }

    @SuppressLint("NewApi")
    private fun buildSuccessState(
        tasks: List<Task>,
        classroomTasks: List<ClassroomTask>,
        archivedTasks: List<Task>
    ): DashboardUiState.Success {
        val now = LocalDateTime.now()
        val threeDaysFromNow = now.plusDays(3)

        val pendingClassroom = classroomTasks.filter {
            it.submissionState != SubmissionState.TURNED_IN
        }

        val upcomingItems = mutableListOf<UpcomingItem>()

        classroomTasks.forEach { ct ->
            upcomingItems.add(
                UpcomingItem(
                    title = ct.title,
                    courseName = ct.courseName,
                    dueDate = ct.dueDate,
                    isClassroom = true
                )
            )
        }

        tasks.forEach { t ->
            upcomingItems.add(
                UpcomingItem(
                    title = t.title,
                    courseName = null,
                    dueDate = null,
                    isClassroom = false
                )
            )
        }

        val sortedUpcoming = upcomingItems
            .sortedWith(compareBy(nullsLast()) { it.dueDate })
            .take(5)

        val dueSoonCount = classroomTasks.count { ct ->
            ct.dueDate != null &&
                ct.dueDate.isAfter(now) &&
                ct.dueDate.isBefore(threeDaysFromNow) &&
                ct.submissionState != SubmissionState.TURNED_IN
        }

        val activeCourses = pendingClassroom
            .groupBy { it.courseName }
            .map { (name, courseTasks) -> CourseInfo(name, courseTasks.size) }

        return DashboardUiState.Success(
            pendingCount = tasks.size + pendingClassroom.size,
            completedCount = archivedTasks.size +
                classroomTasks.count { it.submissionState == SubmissionState.TURNED_IN },
            dueSoonCount = dueSoonCount,
            upcomingDeadlines = sortedUpcoming,
            highPriorityCount = tasks.count { it.priority.lowercase() == "high" },
            mediumPriorityCount = tasks.count { it.priority.lowercase() == "medium" },
            lowPriorityCount = tasks.count { it.priority.lowercase() == "low" },
            activeCourses = activeCourses,
            archivedTasks = archivedTasks
        )
    }
}
