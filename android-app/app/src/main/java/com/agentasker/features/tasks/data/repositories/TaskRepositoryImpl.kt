package com.agentasker.features.tasks.data.repositories

import com.agentasker.core.network.AgentTaskerApi
import com.agentasker.features.tasks.data.datasources.local.dao.TaskDao
import com.agentasker.features.tasks.data.datasources.local.entities.TaskEntity
import com.agentasker.features.tasks.data.datasources.local.mapper.toDomain
import com.agentasker.features.tasks.data.datasources.local.mapper.toEntities
import com.agentasker.features.tasks.data.datasources.local.mapper.toEntity
import com.agentasker.features.tasks.data.datasources.remote.model.CreateTaskRequest
import com.agentasker.features.tasks.data.datasources.remote.model.UpdateTaskRequest
import com.agentasker.features.tasks.domain.entities.Task
import com.agentasker.features.tasks.domain.repositories.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val api: AgentTaskerApi,
    private val taskDao: TaskDao
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities -> entities.toDomain() }
    }

    override suspend fun refreshTasks() {
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
        val response = api.getTaskById(id.toInt())
        val entity = response.toEntity(isSynced = true)
        taskDao.upsertTask(entity)
        return entity.toDomain()
    }

    override suspend fun createTask(title: String, description: String, priority: String): Task {
        val request = CreateTaskRequest(
            title = title,
            description = description,
            priority = priority
        )
        return try {
            val response = api.createTask(request)
            val entity = response.toEntity(isSynced = true)
            taskDao.upsertTask(entity)
            entity.toDomain()
        } catch (e: Exception) {
            val localEntity = TaskEntity(
                id = "local_${System.currentTimeMillis()}",
                title = title,
                description = description,
                priority = priority,
                isSynced = false
            )
            taskDao.upsertTask(localEntity)
            localEntity.toDomain()
        }
    }

    override suspend fun updateTask(
        id: String,
        title: String?,
        description: String?,
        priority: String?
    ): Task {
        val request = UpdateTaskRequest(
            title = title,
            description = description,
            priority = priority
        )
        return try {
            val response = api.updateTask(id.toInt(), request)
            val entity = response.toEntity(isSynced = true)
            taskDao.upsertTask(entity)
            entity.toDomain()
        } catch (e: Exception) {
            val current = taskDao.getTaskById(id).first()
                ?: throw e
            val updated = current.copy(
                title = title ?: current.title,
                description = description ?: current.description,
                priority = priority ?: current.priority,
                isSynced = false
            )
            taskDao.upsertTask(updated)
            updated.toDomain()
        }
    }

    override suspend fun deleteTask(id: String) {
        taskDao.deleteTaskById(id)
        try {
            api.deleteTask(id.toInt())
        } catch (_: Exception) {
            // Task already removed from local cache for instant UI feedback
        }
    }
}
