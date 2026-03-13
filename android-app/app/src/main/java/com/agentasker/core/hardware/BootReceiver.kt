package com.agentasker.core.hardware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.agentasker.features.tasks.data.datasources.local.dao.TaskDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskDao: TaskDao

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasksWithReminders = taskDao.getTasksWithReminders()
                tasksWithReminders.forEach { task ->
                    task.reminderAt?.let { reminderAt ->
                        reminderScheduler.scheduleReminder(
                            taskId = task.id,
                            title = "Recordatorio: ${task.title}",
                            body = task.description,
                            triggerAtMillis = reminderAt
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
