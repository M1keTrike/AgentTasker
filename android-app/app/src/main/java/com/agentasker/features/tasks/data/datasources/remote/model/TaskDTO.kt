package com.agentasker.features.tasks.data.datasources.remote.model

import com.google.gson.annotations.SerializedName

typealias TaskResponse = List<TaskDTO>

data class TaskDTO(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("priority")
    val priority: String,
    @SerializedName("status")
    val status: String = "pending",
    @SerializedName("dueDate")
    val dueDate: String? = null,
    @SerializedName("source")
    val source: String? = null,
    @SerializedName("externalId")
    val externalId: String? = null,
    @SerializedName("courseName")
    val courseName: String? = null,
    @SerializedName("externalLink")
    val externalLink: String? = null,
    @SerializedName("subtasks")
    val subtasks: List<SubtaskDTO>? = null,
    @SerializedName("createdAt")
    val createdAt: String?,
    @SerializedName("updatedAt")
    val updatedAt: String?
)

data class CreateTaskRequest(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("priority")
    val priority: String = "medium",
    @SerializedName("status")
    val status: String = "pending",
    @SerializedName("dueDate")
    val dueDate: String? = null,
    @SerializedName("source")
    val source: String? = null,
    @SerializedName("externalId")
    val externalId: String? = null,
    @SerializedName("courseName")
    val courseName: String? = null,
    @SerializedName("externalLink")
    val externalLink: String? = null
)

data class UpdateTaskRequest(
    @SerializedName("title")
    val title: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("priority")
    val priority: String?,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("dueDate")
    val dueDate: String? = null
)

data class SubtaskDTO(
    @SerializedName("id")
    val id: Int,
    @SerializedName("taskId")
    val taskId: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("isCompleted")
    val isCompleted: Boolean = false,
    @SerializedName("position")
    val position: Int = 0,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

data class CreateSubtaskRequest(
    @SerializedName("title")
    val title: String,
    @SerializedName("isCompleted")
    val isCompleted: Boolean? = null,
    @SerializedName("position")
    val position: Int? = null
)

data class UpdateSubtaskRequest(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("isCompleted")
    val isCompleted: Boolean? = null,
    @SerializedName("position")
    val position: Int? = null
)

data class BulkCreateSubtasksRequest(
    @SerializedName("subtasks")
    val subtasks: List<String>
)
