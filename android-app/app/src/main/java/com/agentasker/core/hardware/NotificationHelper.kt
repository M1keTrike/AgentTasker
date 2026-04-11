package com.agentasker.core.hardware

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.agentasker.MainActivity
import com.agentasker.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
        as NotificationManager

    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val taskChannel = NotificationChannel(
                TASK_REMINDERS_CHANNEL,
                "Recordatorios de tareas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de recordatorios de tareas programadas"
            }
            notificationManager.createNotificationChannel(taskChannel)

            val classroomChannel = NotificationChannel(
                CLASSROOM_CHANNEL,
                "Tareas de Classroom",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de tareas de Google Classroom"
            }
            notificationManager.createNotificationChannel(classroomChannel)

            val pushChannel = NotificationChannel(
                PUSH_CHANNEL,
                "Notificaciones push",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Mensajes push enviados desde el servidor (FCM)"
            }
            notificationManager.createNotificationChannel(pushChannel)

            // Canal LOW para progreso de workers de IA — sin sonido ni
            // vibración para no molestar mientras corre en background.
            val aiChannel = NotificationChannel(
                AI_PROGRESS_CHANNEL,
                "Procesos de IA",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progreso de análisis y generación de tareas con IA"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(aiChannel)
        }
    }

    /**
     * Construye una notificación "en curso" usada como foreground info
     * desde los workers de IA. El ID debe ser estable por worker para
     * que WorkManager pueda reemplazarla al actualizar progreso.
     */
    fun buildAiProgressNotification(title: String, body: String): Notification {
        return NotificationCompat.Builder(context, AI_PROGRESS_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(0, 0, true)
            .build()
    }

    /**
     * Muestra una notificación final (éxito o fallo) cuando un worker
     * de IA termina. Al tocarla abre la app con deep link a `screen`.
     */
    fun showAiResultNotification(
        title: String,
        body: String,
        success: Boolean,
        screen: String = "tasks",
        taskId: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("screen", screen)
            if (taskId != null) putExtra("taskId", taskId)
        }
        val notificationId = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, AI_PROGRESS_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(
                if (success) NotificationCompat.PRIORITY_DEFAULT
                else NotificationCompat.PRIORITY_HIGH
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showPushNotification(title: String, body: String, data: Map<String, String> = emptyMap()) {
        // FLAG_ACTIVITY_SINGLE_TOP en vez de CLEAR_TASK para que, si la Activity
        // ya está en el top del stack, Android invoque onNewIntent() con los
        // extras en lugar de recrearla desde cero — así el deep link se procesa
        // sin perder el estado de navegación actual.
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data.forEach { (k, v) -> putExtra(k, v) }
        }
        val notificationId = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, PUSH_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showTaskReminder(taskId: String, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, taskId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, TASK_REMINDERS_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }

    fun showClassroomReminder(taskId: String, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, taskId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CLASSROOM_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }

    companion object {
        const val TASK_REMINDERS_CHANNEL = "task_reminders"
        const val CLASSROOM_CHANNEL = "classroom_tasks"
        const val PUSH_CHANNEL = "push_notifications"
        const val AI_PROGRESS_CHANNEL = "ai_progress"

        // IDs estables para notifications en curso (foreground workers).
        const val AI_SUBTASK_WORKER_NOTIFICATION_ID = 2001
        const val AI_IMAGE_WORKER_NOTIFICATION_ID = 2002
    }
}
