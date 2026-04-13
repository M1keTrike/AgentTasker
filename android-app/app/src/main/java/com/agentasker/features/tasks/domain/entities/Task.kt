package com.agentasker.features.tasks.domain.entities

object TaskSource {
    const val LOCAL = "local"
    const val CLASSROOM = "classroom"
}

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val priority: String,
    val status: String = "pending",
    val dueDate: String? = null,
    val source: String = TaskSource.LOCAL,
    val externalId: String? = null,
    val courseName: String? = null,
    val externalLink: String? = null,
    val isArchived: Boolean = false,
    val subtasks: List<Subtask> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    val allSubtasksCompleted: Boolean
        get() = subtasks.isNotEmpty() && subtasks.all { it.isCompleted }
}
