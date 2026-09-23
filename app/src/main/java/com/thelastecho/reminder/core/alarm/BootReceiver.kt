package com.thelastecho.reminder.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.thelastecho.reminder.data.local.ReminderDatabase
import com.thelastecho.reminder.data.repository.ReminderRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            val database = ReminderDatabase.getInstance(context)
            val repository = ReminderRepositoryImpl(database.reminderDao(), database.categoryDao())
            val alarmScheduler = AndroidAlarmScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val activeReminders = repository.getActiveScheduledReminders()
                    activeReminders.forEach { reminder ->
                        alarmScheduler.schedule(reminder)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
