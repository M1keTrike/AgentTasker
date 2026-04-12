package com.agentasker.features.tasks.data.repositories

import android.util.Log
import com.agentasker.core.network.AgentTaskerApi
import com.agentasker.core.network.NetworkMonitor
import com.agentasker.features.tasks.data.datasources.local.dao.SubtaskDao
import com.agentasker.features.tasks.data.datasources.local.dao.TaskDao
import com.agentasker.features.tasks.data.datasources.local.entities.SubtaskEntity
import com.agentasker.features.tasks.data.datasources.local.entities.TaskEntity
import com.agentasker.features.tasks.data.datasources.local.mapper.subtasksToDomain
import com.agentasker.features.tasks.data.datasources.local.mapper.toDomain
import com.agentasker.features.tasks.data.datasources.local.mapper.toEntities
import com.agentasker.features.tasks.data.datasources.local.mapper.toEntity
import com.agentasker.features.tasks.data.datasources.remote.model.BulkCreateSubtasksRequest
import com.agentasker.features.tasks.data.datasources.remote.model.CreateSubtaskRequest
import com.agentasker.features.tasks.data.datasources.remote.model.CreateTaskRequest
import com.agentasker.features.tasks.data.datasources.remote.model.UpdateSubtaskRequest
import com.agentasker.features.tasks.data.datasources.remote.model.UpdateTaskRequest
import com.agentasker.features.tasks.data.workers.TaskSyncScheduler
import com.agentasker.features.tasks.domain.entities.Subtask
import com.agentasker.features.tasks.domain.entities.Task
import com.agentasker.features.tasks.domain.entities.TaskSource
import com.agentasker.features.tasks.domain.repositories.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject

private const val TAG = "TaskRepository"

class TaskRepositoryImpl @Inject constructor(
    private val api: AgentTaskerApi,
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val networkMonitor: NetworkMonitor,
    private val syncScheduler: TaskSyncScheduler
) : TaskRepository {

    private suspend fun isOnline(): Boolean = networkMonitor.isOnline.first()

    override fun observeTasks(): Flow<List<Task>> {
        // Combinamos tasks con TODAS las subtasks y juntamos en memoria.
        // Así el Flow re-emite cuando cambia cualquiera de las dos tablas,
        // y los toggles de subtasks se reflejan inmediatamente en la UI.
        // Si el número crece mucho, migrar a @Transaction + @Relation.
        return combine(
            taskDao.getAllTasks(),
            subtaskDao.observeAll()
        ) { taskEntities, allSubtasks ->
            val subsByTask = allSubtasks.groupBy { it.taskId }
            taskEntities.map { entity ->
                val subs = (subsByTask[entity.id] ?: emptyList())
                    .sortedBy { it.position }
                    .map { it.toDomain() }
                entity.toDomain(subs)
            }
        }
    }

    override fun observeArchivedTasks(): Flow<List<Task>> {
        return combine(
            taskDao.getArchivedTasks(),
            subtaskDao.observeAll()
        ) { taskEntities, allSubtasks ->
            val subsByTask = allSubtasks.groupBy { it.taskId }
            taskEntities.map { entity ->
                val subs = (subsByTask[entity.id] ?: emptyList())
                    .sortedBy { it.position }
                    .map { it.toDomain() }
                entity.toDomain(subs)
            }
        }
    }

    override suspend fun refreshTasks() {
        if (!isOnline()) return
        try {
            val remoteTasks = api.getTasks()
            taskDao.upsertTasks(remoteTasks.toEntities(isSynced = true))
            // Sincronizamos subtasks embebidas en la respuesta.
            remoteTasks.forEach { dto ->
                dto.subtasks?.let { subs ->
                    val entities = subs.map { sub ->
                        SubtaskEntity(
                            id = sub.id.toString(),
                            taskId = sub.taskId.toString(),
                            title = sub.title,
                            isCompleted = sub.isCompleted,
                            position = sub.position,
                            createdAt = sub.createdAt,
                            updatedAt = sub.updatedAt,
                            isSynced = true
                        )
                    }
                    if (entities.isNotEmpty()) subtaskDao.upsertAll(entities)
                }
            }
        } catch (_: Exception) { }
    }

    override suspend fun getTasks(): List<Task> {
        refreshTasks()
        return taskDao.getAllTasks().first().toDomain()
    }

    override suspend fun getTaskById(id: String): Task {
        if (isOnline()) {
            try {
                val response = api.getTaskById(id.toInt())
                val entity = response.toEntity(isSynced = true)
                taskDao.upsertTask(entity)
                val subs = response.subtasks.orEmpty().map { sub ->
                    SubtaskEntity(
                        id = sub.id.toString(),
                        taskId = id,
                        title = sub.title,
                        isCompleted = sub.isCompleted,
                        position = sub.position,
                        createdAt = sub.createdAt,
                        updatedAt = sub.updatedAt,
                        isSynced = true
                    )
                }
                if (subs.isNotEmpty()) subtaskDao.upsertAll(subs)
                return entity.toDomain(subs.map { it.toDomain() })
            } catch (_: Exception) { }
        }
        val cached = taskDao.getTaskById(id).first()
            ?: throw Exception("Tarea no encontrada")
        val subs = subtaskDao.getByTaskId(id).map { it.toDomain() }
        return cached.toDomain(subs)
    }

    override suspend fun createTask(
        title: String,
        description: String,
        priority: String,
        status: String,
        dueDate: String?,
        source: String?,
        externalId: String?,
        courseName: String?,
        externalLink: String?
    ): Task {
        val request = CreateTaskRequest(
            title = title,
            description = description,
            priority = priority,
            status = status,
            dueDate = dueDate,
            source = source,
            externalId = externalId,
            courseName = courseName,
            externalLink = externalLink
        )
        Log.d(TAG, "createTask online=${isOnline()} request=$request")
        if (isOnline()) {
            try {
                val response = api.createTask(request)
                val entity = response.toEntity(isSynced = true)
                taskDao.upsertTask(entity)
                Log.d(TAG, "createTask OK remote id=${response.id}")
                return entity.toDomain()
            } catch (e: HttpException) {
                val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                Log.w(TAG, "createTask HTTP ${e.code()}: $body — fallback a local")
            } catch (e: Exception) {
                Log.w(TAG, "createTask exception: ${e.message} — fallback a local", e)
            }
        }
        val localEntity = TaskEntity(
            id = "local_${System.currentTimeMillis()}",
            title = title,
            description = description,
            priority = priority,
            status = status,
            dueDate = dueDate,
            source = source ?: TaskSource.LOCAL,
            externalId = externalId,
            courseName = courseName,
            externalLink = externalLink,
            isSynced = false,
            pendingAction = "create"
        )
        taskDao.upsertTask(localEntity)
        syncScheduler.scheduleSyncOnConnectivity()
        return localEntity.toDomain()
    }

    override suspend fun updateTask(
        id: String,
        title: String?,
        description: String?,
        priority: String?,
        status: String?,
        dueDate: String?,
        isArchived: Boolean?
    ): Task {
        val request = UpdateTaskRequest(
            title = title,
            description = description,
            priority = priority,
            status = status,
            dueDate = dueDate,
            isArchived = isArchived
        )
        if (isOnline() && id.toIntOrNull() != null) {
            try {
                val response = api.updateTask(id.toInt(), request)
                val entity = response.toEntity(isSynced = true)
                taskDao.upsertTask(entity)
                return entity.toDomain()
            } catch (_: Exception) { }
        }
        val current = taskDao.getTaskById(id).first()
            ?: throw Exception("Tarea no encontrada")
        val action = if (current.pendingAction == "create") "create" else "update"
        val updated = current.copy(
            title = title ?: current.title,
            description = description ?: current.description,
            priority = priority ?: current.priority,
            status = status ?: current.status,
            dueDate = dueDate ?: current.dueDate,
            isArchived = isArchived ?: current.isArchived,
            isSynced = false,
            pendingAction = action
        )
        taskDao.upsertTask(updated)
        syncScheduler.scheduleSyncOnConnectivity()
        return updated.toDomain()
    }

    override suspend fun completeAndArchive(id: String): Task {
        return updateTask(
            id = id,
            title = null,
            description = null,
            priority = null,
            status = "completed",
            dueDate = null,
            isArchived = true
        )
    }

    override suspend fun deleteTask(id: String) {
        val existing = taskDao.getTaskByIdSync(id)

        if (existing?.pendingAction == "create") {
            taskDao.deleteTaskById(id)
            return
        }

        if (isOnline()) {
            try {
                api.deleteTask(id.toInt())
                taskDao.deleteTaskById(id)
                return
            } catch (_: Exception) { }
        }

        if (existing != null) {
            taskDao.upsertTask(existing.copy(isSynced = false, pendingAction = "delete"))
            syncScheduler.scheduleSyncOnConnectivity()
        } else {
            taskDao.deleteTaskById(id)
        }
    }

    // ---------- Subtasks ----------

    override fun observeSubtasks(taskId: String): Flow<List<Subtask>> {
        return subtaskDao.observeByTaskId(taskId).map { it.subtasksToDomain() }
    }

    override suspend fun getSubtasks(taskId: String): List<Subtask> {
        if (isOnline()) {
            try {
                val response = api.getSubtasks(taskId.toInt())
                val entities = response.map { dto ->
                    SubtaskEntity(
                        id = dto.id.toString(),
                        taskId = dto.taskId.toString(),
                        title = dto.title,
                        isCompleted = dto.isCompleted,
                        position = dto.position,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt,
                        isSynced = true
                    )
                }
                subtaskDao.upsertAll(entities)
                return entities.map { it.toDomain() }
            } catch (_: Exception) { }
        }
        return subtaskDao.getByTaskId(taskId).map { it.toDomain() }
    }

    override suspend fun createSubtasksBulk(taskId: String, titles: List<String>): List<Subtask> {
        if (titles.isEmpty()) return emptyList()

        if (isOnline() && taskId.toIntOrNull() != null) {
            try {
                val response = api.createSubtasksBulk(
                    taskId.toInt(),
                    BulkCreateSubtasksRequest(subtasks = titles)
                )
                val entities = response.map { dto ->
                    SubtaskEntity(
                        id = dto.id.toString(),
                        taskId = taskId,
                        title = dto.title,
                        isCompleted = dto.isCompleted,
                        position = dto.position,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt,
                        isSynced = true
                    )
                }
                subtaskDao.upsertAll(entities)
                return entities.map { it.toDomain() }
            } catch (e: Exception) {
                Log.w(TAG, "createSubtasksBulk online falló: ${e.message}", e)
            }
        }

        // Offline / task local: creamos filas locales con pendingAction="create".
        val existingCount = subtaskDao.countByTaskId(taskId)
        val now = System.currentTimeMillis()
        val entities = titles.mapIndexed { idx, title ->
            SubtaskEntity(
                id = "local_sub_${now}_$idx",
                taskId = taskId,
                title = title,
                position = existingCount + idx,
                isSynced = false,
                pendingAction = "create"
            )
        }
        subtaskDao.upsertAll(entities)
        syncScheduler.scheduleSyncOnConnectivity()
        return entities.map { it.toDomain() }
    }

    override suspend fun createSubtask(taskId: String, title: String): Subtask {
        return createSubtasksBulk(taskId, listOf(title)).first()
    }

    override suspend fun updateSubtask(subtaskId: String, title: String?, isCompleted: Boolean?): Subtask {
        val current = subtaskDao.getByIdSync(subtaskId)
            ?: throw Exception("Subtarea no encontrada")

        if (isOnline() && subtaskId.toIntOrNull() != null) {
            try {
                val response = api.updateSubtask(
                    subtaskId.toInt(),
                    UpdateSubtaskRequest(title = title, isCompleted = isCompleted)
                )
                val synced = SubtaskEntity(
                    id = response.id.toString(),
                    taskId = response.taskId.toString(),
                    title = response.title,
                    isCompleted = response.isCompleted,
                    position = response.position,
                    createdAt = response.createdAt,
                    updatedAt = response.updatedAt,
                    isSynced = true
                )
                subtaskDao.upsert(synced)
                return synced.toDomain()
            } catch (_: Exception) { }
        }

        val action = if (current.pendingAction == "create") "create" else "update"
        val updated = current.copy(
            title = title ?: current.title,
            isCompleted = isCompleted ?: current.isCompleted,
            isSynced = false,
            pendingAction = action
        )
        subtaskDao.upsert(updated)
        syncScheduler.scheduleSyncOnConnectivity()
        return updated.toDomain()
    }

    override suspend fun deleteSubtask(subtaskId: String) {
        val existing = subtaskDao.getByIdSync(subtaskId) ?: return

        if (existing.pendingAction == "create") {
            subtaskDao.deleteById(subtaskId)
            return
        }

        if (isOnline() && subtaskId.toIntOrNull() != null) {
            try {
                api.deleteSubtask(subtaskId.toInt())
                subtaskDao.deleteById(subtaskId)
                return
            } catch (_: Exception) { }
        }

        subtaskDao.upsert(existing.copy(isSynced = false, pendingAction = "delete"))
        syncScheduler.scheduleSyncOnConnectivity()
    }

    override suspend fun replaceSubtasks(taskId: String, titles: List<String>): List<Subtask> {
        // Fase 1: borrar las existentes. Iteramos una a una para respetar el
        // flujo offline-first (cada deleteSubtask maneja online/pending).
        val existing = subtaskDao.getByTaskId(taskId)
        existing.forEach { sub ->
            try { deleteSubtask(sub.id) } catch (e: Exception) {
                Log.w(TAG, "replaceSubtasks: fallo borrando sub ${sub.id}: ${e.message}")
            }
        }
        // Fase 2: crear las nuevas (ya limpia la lista).
        return createSubtasksBulk(taskId, titles)
    }

    // ---------- Classroom import ----------

    override suspend fun upsertImportedTask(task: Task): Task {
        // Buscamos por externalId usando una query directa que NO filtra
        // por isArchived — si ya existe (incluso archivada), queremos
        // preservar los flags locales en vez de re-importarla como nueva.
        val existing = task.externalId?.let { taskDao.findByExternalId(it) }

        // Si el usuario borró manualmente la Classroom task (quedó con
        // pendingAction="delete"), respetamos esa decisión y no la
        // re-importamos. Caso contrario sería confuso: el usuario la
        // borra, toca sync, y la task reaparece como zombie.
        if (existing?.pendingAction == "delete") {
            return existing.toDomain()
        }

        val entity = if (existing != null) {
            // Actualizamos campos "de Classroom" (título, descripción,
            // fechas, etc.) pero preservamos el estado local que el
            // usuario pudo haber cambiado: isArchived, status (si la
            // tenía como completed/in_progress), y el ID local.
            task.toEntity(isSynced = true).copy(
                id = existing.id,
                isArchived = existing.isArchived,
                // Si el usuario ya marcó la task como completed o
                // in_progress, respetamos su decisión. Solo pisamos si
                // sigue en pending.
                status = if (existing.status == "pending") task.status else existing.status
            )
        } else {
            task.toEntity(isSynced = true)
        }
        taskDao.upsertTask(entity)
        return entity.toDomain()
    }
}
