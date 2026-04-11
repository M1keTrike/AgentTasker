package com.agentasker.core.hardware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReminderBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var hapticFeedbackManager: HapticFeedbackManager

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Recordatorio"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "Tienes una tarea pendiente"

        // Vibrar ANTES de mostrar la notificación para que el usuario sienta
        // el aviso físico aunque tenga el teléfono en silencio.
        hapticFeedbackManager.notification()
        notificationHelper.showTaskReminder(taskId, title, body)
    }

    companion object {
        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
    }
}
