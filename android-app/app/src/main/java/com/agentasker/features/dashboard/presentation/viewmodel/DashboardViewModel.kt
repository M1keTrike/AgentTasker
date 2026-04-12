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

/**
 * ViewModel del Dashboard. Ahora se alimenta SOLO de la tabla local `tasks`
 * (Room), que ya incluye las tasks de Classroom sincronizadas. Ya NO llama
 * a `getClassroomTasksUseCase()` (API de Google) — eso eliminó:
 *  - Llamadas extras a Google (evitando 429)
 *  - Cursos no-sincronizados apareciendo en las estadísticas
 *  - Conteos duplicados (tasks locales + tasks remotas de Classroom)
 */
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

        // --- Próximas entregas: de TODAS las tasks locales con dueDate ---
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

        // --- Por vencer: tasks con dueDate en los próximos 3 días ---
        val dueSoonCount = tasks.count { task ->
            val dl = task.dueDate?.toLocalDateTimeOrNull() ?: return@count false
            dl.isAfter(now) && dl.isBefore(threeDaysFromNow)
        }

        // --- Cursos activos: solo de tasks de Classroom YA sincronizadas ---
        val classroomTasks = tasks.filter { it.source == TaskSource.CLASSROOM }
        val activeCourses = classroomTasks
            .mapNotNull { it.courseName }
            .groupingBy { it }
            .eachCount()
            .map { (name, count) -> CourseInfo(name, count) }
            .sortedByDescending { it.pendingCount }

        return DashboardUiState.Success(
            // Pendientes = tasks activas (no archivadas, Room ya filtra eso)
            pendingCount = tasks.size,
            // Completadas = solo las archivadas (el flujo explícito del usuario)
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

/**
 * Parsea un string ISO 8601 (como "2026-04-12T16:50:00.000Z" o
 * "2026-05-24T04:59:00") a [LocalDateTime] en la zona horaria local.
 * Retorna null si el formato no es válido.
 */
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
