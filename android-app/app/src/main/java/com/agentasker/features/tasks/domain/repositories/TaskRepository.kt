package com.agentasker.features.tasks.domain.repositories

import com.agentasker.features.tasks.domain.entities.Subtask
import com.agentasker.features.tasks.domain.entities.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    /** Solo emite tasks archivadas. Se consume desde el Dashboard. */
    fun observeArchivedTasks(): Flow<List<Task>>
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
        dueDate: String? = null,
        isArchived: Boolean? = null
    ): Task
    /**
     * Marca la task como completada y archivada (atajo del botón
     * "Completar" que aparece cuando todas las subtasks están tachadas).
     */
    suspend fun completeAndArchive(id: String): Task
    suspend fun deleteTask(id: String)

    // ---------- Subtasks ----------

    fun observeSubtasks(taskId: String): Flow<List<Subtask>>
    suspend fun getSubtasks(taskId: String): List<Subtask>
    suspend fun createSubtasksBulk(taskId: String, titles: List<String>): List<Subtask>
    suspend fun createSubtask(taskId: String, title: String): Subtask
    suspend fun updateSubtask(subtaskId: String, title: String? = null, isCompleted: Boolean? = null): Subtask
    suspend fun deleteSubtask(subtaskId: String)

    /**
     * Borra todas las subtasks existentes de la task y las reemplaza por
     * las nuevas. Usado por el flujo de IA: si el usuario ya tenía
     * subtasks manuales y vuelve a tocar "Dividir con IA", las anteriores
     * se sobreescriben. Devuelve las nuevas subtasks creadas.
     */
    suspend fun replaceSubtasks(taskId: String, titles: List<String>): List<Subtask>

    /**
     * Upsert de una task importada de Classroom: usa (userId, externalId) como
     * clave lógica y actualiza sin duplicar. Se llama desde Classroom sync.
     */
    suspend fun upsertImportedTask(task: Task): Task
}
