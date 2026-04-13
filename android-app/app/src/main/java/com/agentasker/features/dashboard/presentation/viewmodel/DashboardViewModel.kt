package com.agentasker.features.dashboard.presentation.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentasker.features.tasks.domain.entities.Task
import com.agentasker.features.tasks.domain.entities.TaskSource
import com.agentasker.features.tasks.domain.repositories.TaskRepository
import com.agentasker.features.tasks.domain.usecases.GetTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
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

            combine(
                getTasksUseCase(),
                taskRepository.observeArchivedTasks()
            ) { tasks, archived ->
                tasks to archived
            }.collect { (tasks, archived) ->
                _uiState.value = buildSuccessState(tasks, archived)
            }
        }
    }

    fun deleteArchivedPermanently(taskId: String) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(taskId)
            } catch (_: Exception) { }
        }
    }

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
        archivedTasks: List<Task>
    ): DashboardUiState.Success {
        val now = LocalDateTime.now()
        val threeDaysFromNow = now.plusDays(3)

        val upcomingItems = tasks.mapNotNull { task ->
            val dueLocal = task.dueDate?.toLocalDateTimeOrNull() ?: return@mapNotNull null
            UpcomingItem(
                title = task.title,
                courseName = task.courseName,
                dueDate = dueLocal,
                isClassroom = task.source == TaskSource.CLASSROOM
            )
        }
            .sortedBy { it.dueDate }
            .take(5)

        val dueSoonCount = tasks.count { task ->
            val dl = task.dueDate?.toLocalDateTimeOrNull() ?: return@count false
            dl.isAfter(now) && dl.isBefore(threeDaysFromNow)
        }

        val classroomTasks = tasks.filter { it.source == TaskSource.CLASSROOM }
        val activeCourses = classroomTasks
            .mapNotNull { it.courseName }
            .groupingBy { it }
            .eachCount()
            .map { (name, count) -> CourseInfo(name, count) }
            .sortedByDescending { it.pendingCount }

        return DashboardUiState.Success(
            pendingCount = tasks.size,
            completedCount = archivedTasks.size,
            dueSoonCount = dueSoonCount,
            upcomingDeadlines = upcomingItems,
            highPriorityCount = tasks.count { it.priority.lowercase() == "high" },
            mediumPriorityCount = tasks.count { it.priority.lowercase() == "medium" },
            lowPriorityCount = tasks.count { it.priority.lowercase() == "low" },
            activeCourses = activeCourses,
            archivedTasks = archivedTasks
        )
    }
}

@SuppressLint("NewApi")
private fun String.toLocalDateTimeOrNull(): LocalDateTime? = try {
    val instant = Instant.parse(this)
    LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
} catch (_: Exception) {
    try {
        LocalDateTime.parse(this)
    } catch (_: Exception) {
        null
    }
}
