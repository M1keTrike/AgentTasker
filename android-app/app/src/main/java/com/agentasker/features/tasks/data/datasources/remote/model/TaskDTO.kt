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
    val priority: String = "medium"
)

data class UpdateTaskRequest(
    @SerializedName("title")
    val title: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("priority")
    val priority: String?
)

data class SubtaskDTO(
    @SerializedName("id")
    val id: String?,
    @SerializedName("title")
    val title: String,
    @SerializedName("isCompleted")
    val isCompleted: Boolean?
)

