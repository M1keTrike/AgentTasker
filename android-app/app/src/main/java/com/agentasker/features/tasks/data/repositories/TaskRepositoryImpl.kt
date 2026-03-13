package com.agentasker.features.tasks.data.repositories

import com.agentasker.core.network.AgentTaskerApi
import com.agentasker.core.network.NetworkMonitor
import com.agentasker.features.tasks.data.datasources.local.dao.TaskDao
import com.agentasker.features.tasks.data.datasources.local.entities.TaskEntity
import com.agentasker.features.tasks.data.datasources.local.mapper.toDomain
import com.agentasker.features.tasks.data.datasources.local.mapper.toEntities
import com.agentasker.features.tasks.data.datasources.local.mapper.toEntity
import com.agentasker.features.tasks.data.datasources.remote.model.CreateTaskRequest
import com.agentasker.features.tasks.data.datasources.remote.model.UpdateTaskRequest
import com.agentasker.features.tasks.data.workers.TaskSyncScheduler
import com.agentasker.features.tasks.domain.entities.Task
import com.agentasker.features.tasks.domain.repositories.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val api: AgentTaskerApi,
    private val taskDao: TaskDao,
    private val networkMonitor: NetworkMonitor,
    private val syncScheduler: TaskSyncScheduler
) : TaskRepository {

    private suspend fun isOnline(): Boolean = networkMonitor.isOnline.first()

    override fun observeTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities -> entities.toDomain() }
    }

    override suspend fun refreshTasks() {
        if (!isOnline()) return
        try {
            val remoteTasks = api.getTasks()
            taskDao.upsertTasks(remoteTasks.toEntities(isSynced = true))
        } catch (_: Exception) {
            // No network — UI still shows cached data via observeTasks()
        }
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
                return entity.toDomain()
            } catch (_: Exception) { }
        }
        return taskDao.getTaskById(id).first()?.toDomain()
            ?: throw Exception("Tarea no encontrada")
    }

    override suspend fun createTask(title: String, description: String, priority: String, status: String, dueDate: String?): Task {
        val request = CreateTaskRequest(
            title = title,
            description = description,
            priority = priority,
            status = status,
            dueDate = dueDate
        )
        if (isOnline()) {
            try {
                val response = api.createTask(request)
                val entity = response.toEntity(isSynced = true)
                taskDao.upsertTask(entity)
                return entity.toDomain()
            } catch (_: Exception) { }
        }
        val localEntity = TaskEntity(
            id = "local_${System.currentTimeMillis()}",
            title = title,
            description = description,
            priority = priority,
            status = status,
            dueDate = dueDate,
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
        dueDate: String?
    ): Task {
        val request = UpdateTaskRequest(
            title = title,
            description = description,
            priority = priority,
            status = status,
            dueDate = dueDate
        )
        if (isOnline()) {
            try {
                val response = api.updateTask(id.toInt(), request)
                val entity = response.toEntity(isSynced = true)
                taskDao.upsertTask(entity)
                return entity.toDomain()
            } catch (_: Exception) { }
        }
        val current = taskDao.getTaskById(id).first()
            ?: throw Exception("Tarea no encontrada")
        // If task was pending create, keep "create" as pendingAction
        val action = if (current.pendingAction == "create") "create" else "update"
        val updated = current.copy(
            title = title ?: current.title,
            description = description ?: current.description,
            priority = priority ?: current.priority,
            status = status ?: current.status,
            dueDate = dueDate ?: current.dueDate,
            isSynced = false,
            pendingAction = action
        )
        taskDao.upsertTask(updated)
        syncScheduler.scheduleSyncOnConnectivity()
        return updated.toDomain()
    }

    override suspend fun deleteTask(id: String) {
        val existing = taskDao.getTaskByIdSync(id)

        if (existing?.pendingAction == "create") {
            // Task never reached the server, just remove locally
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

        // Offline: mark for deletion, will be synced later
        if (existing != null) {
            taskDao.upsertTask(existing.copy(isSynced = false, pendingAction = "delete"))
            syncScheduler.scheduleSyncOnConnectivity()
        } else {
            taskDao.deleteTaskById(id)
        }
    }
}
