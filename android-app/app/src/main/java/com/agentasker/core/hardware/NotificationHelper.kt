package com.agentasker.core.hardware

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
        }
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
    }
}
