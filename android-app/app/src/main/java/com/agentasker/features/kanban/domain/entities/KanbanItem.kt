package com.agentasker.features.kanban.domain.entities

import com.agentasker.features.classroom.domain.entities.ClassroomTask
import com.agentasker.features.tasks.domain.entities.Task

sealed class KanbanItem {
    abstract val id: String
    abstract val title: String
    abstract val status: String
    abstract val type: KanbanItemType

    data class TaskItem(val task: Task) : KanbanItem() {
        override val id: String get() = task.id
        override val title: String get() = task.title
        override val status: String get() = task.status
        override val type: KanbanItemType get() = KanbanItemType.TASK
    }

    data class ClassroomItem(val classroomTask: ClassroomTask) : KanbanItem() {
        override val id: String get() = classroomTask.id
        override val title: String get() = classroomTask.title
        override val status: String get() = classroomTask.submissionState.toKanbanStatus()
        override val type: KanbanItemType get() = KanbanItemType.CLASSROOM
    }
}

enum class KanbanItemType {
    TASK, CLASSROOM
}

private fun com.agentasker.features.classroom.domain.entities.SubmissionState.toKanbanStatus(): String {
    return when (this) {
        com.agentasker.features.classroom.domain.entities.SubmissionState.NEW,
        com.agentasker.features.classroom.domain.entities.SubmissionState.CREATED -> "pending"
        com.agentasker.features.classroom.domain.entities.SubmissionState.RECLAIMED_BY_STUDENT -> "in_progress"
        com.agentasker.features.classroom.domain.entities.SubmissionState.TURNED_IN,
        com.agentasker.features.classroom.domain.entities.SubmissionState.RETURNED -> "completed"
    }
}
