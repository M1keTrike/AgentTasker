package com.agentasker.features.tasks.domain.repositories

import com.agentasker.features.tasks.domain.entities.Subtask
import com.agentasker.features.tasks.domain.entities.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    suspend fun refreshTasks()
    suspend fun getTasks(): List<Task>
    suspend fun getTaskById(id: String): Task
    suspend fun createTask(
        title: String,
        description: String,
        priority: String,
        status: String = "pending",
        dueDate: String? = null,
        source: String? = null,
        externalId: String? = null,
        courseName: String? = null,
        externalLink: String? = null
    ): Task
    suspend fun updateTask(
        id: String,
        title: String?,
        description: String?,
        priority: String?,
        status: String? = null,
        dueDate: String? = null
    ): Task
    suspend fun deleteTask(id: String)

    // ---------- Subtasks ----------

    fun observeSubtasks(taskId: String): Flow<List<Subtask>>
    suspend fun getSubtasks(taskId: String): List<Subtask>
    suspend fun createSubtasksBulk(taskId: String, titles: List<String>): List<Subtask>
    suspend fun createSubtask(taskId: String, title: String): Subtask
    suspend fun updateSubtask(subtaskId: String, title: String? = null, isCompleted: Boolean? = null): Subtask
    suspend fun deleteSubtask(subtaskId: String)

    /**
     * Upsert de una task importada de Classroom: usa (userId, externalId) como
     * clave lógica y actualiza sin duplicar. Se llama desde Classroom sync.
     */
    suspend fun upsertImportedTask(task: Task): Task
}
