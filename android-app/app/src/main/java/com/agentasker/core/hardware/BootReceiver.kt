package com.agentasker.core.hardware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.agentasker.features.tasks.data.datasources.local.dao.TaskReminderDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskReminderDao: TaskReminderDao

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val activeReminders = taskReminderDao.getActiveReminders()
                activeReminders.forEach { reminder ->
                    reminderScheduler.scheduleReminder(
                        taskId = reminder.taskId,
                        title = "Recordatorio: ${reminder.title}",
                        body = reminder.description,
                        triggerAtMillis = reminder.reminderAt
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
