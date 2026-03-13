package com.agentasker.features.kanban.data.datasources.remote.model

import com.google.gson.annotations.SerializedName

data class KanbanColumnDTO(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("statusKey")
    val statusKey: String,
    @SerializedName("position")
    val position: Int,
    @SerializedName("color")
    val color: String?,
    @SerializedName("createdAt")
    val createdAt: String?,
    @SerializedName("updatedAt")
    val updatedAt: String?
)

data class CreateKanbanColumnRequest(
    @SerializedName("title")
    val title: String,
    @SerializedName("statusKey")
    val statusKey: String,
    @SerializedName("position")
    val position: Int? = null,
    @SerializedName("color")
    val color: String? = null
)

data class UpdateKanbanColumnRequest(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("statusKey")
    val statusKey: String? = null,
    @SerializedName("position")
    val position: Int? = null,
    @SerializedName("color")
    val color: String? = null
)

data class ReorderItem(
    @SerializedName("id")
    val id: Int,
    @SerializedName("position")
    val position: Int
)

data class ReorderKanbanColumnsRequest(
    @SerializedName("columns")
    val columns: List<ReorderItem>
)
