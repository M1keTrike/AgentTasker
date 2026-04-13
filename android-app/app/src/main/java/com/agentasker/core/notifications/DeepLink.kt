package com.agentasker.core.notifications

import android.content.Intent

sealed class DeepLink {
    data object Dashboard : DeepLink()
    data object Tasks : DeepLink()
    data object Kanban : DeepLink()
    data object Classroom : DeepLink()

    companion object {
        const val EXTRA_SCREEN = "screen"
        const val EXTRA_TASK_ID = "taskId"

        fun fromIntent(intent: Intent?): DeepLink? {
            val screen = intent?.getStringExtra(EXTRA_SCREEN) ?: return null
            return when (screen.lowercase()) {
                "dashboard" -> Dashboard
                "tasks" -> Tasks
                "kanban" -> Kanban
                "classroom" -> Classroom
                else -> null
            }
        }
    }
}
