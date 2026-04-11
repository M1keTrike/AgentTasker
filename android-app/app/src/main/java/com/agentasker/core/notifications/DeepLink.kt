package com.agentasker.core.notifications

import android.content.Intent

/**
 * Destinos profundos (deep links) que la app reconoce cuando llega desde
 * una notificación FCM.
 *
 * El payload FCM debe incluir un campo `screen` en `data` con uno de los
 * valores del enum, p.ej.:
 *
 * ```json
 * "data": { "screen": "kanban", "taskId": "42" }
 * ```
 */
sealed class DeepLink {
    data object Dashboard : DeepLink()
    data object Tasks : DeepLink()
    data object Kanban : DeepLink()
    data object Classroom : DeepLink()

    companion object {
        const val EXTRA_SCREEN = "screen"
        const val EXTRA_TASK_ID = "taskId"

        /**
         * Intenta extraer un [DeepLink] de los extras de un [Intent].
         * Retorna `null` si el intent no contiene un destino reconocido.
         */
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
