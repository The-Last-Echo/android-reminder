package com.thelastecho.reminder.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.thelastecho.reminder.core.alarm.AndroidAlarmScheduler
import com.thelastecho.reminder.data.local.ReminderDatabase
import com.thelastecho.reminder.data.repository.ReminderRepositoryImpl
import com.thelastecho.reminder.domain.usecase.SnoozeReminderUseCase
import com.thelastecho.reminder.domain.usecase.ToggleReminderCompleteUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val notificationManager = ReminderNotificationManager(
            context,
            com.thelastecho.reminder.core.preferences.UserPreferencesRepository(context)
        )
        notificationManager.dismissNotification(reminderId)

        val pendingResult = goAsync()
        val database = ReminderDatabase.getInstance(context)
        val repository = ReminderRepositoryImpl(database.reminderDao(), database.categoryDao())
        val alarmScheduler = AndroidAlarmScheduler(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_COMPLETE -> {
                        val toggleUseCase = ToggleReminderCompleteUseCase(repository, alarmScheduler)
                        toggleUseCase(reminderId, isCompleted = true)
                    }
                    ACTION_SNOOZE -> {
                        val snoozeUseCase = SnoozeReminderUseCase(repository, alarmScheduler)
                        snoozeUseCase(reminderId) // Snoozes 10 minutes by default
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.thelastecho.reminder.ACTION_COMPLETE"
        const val ACTION_SNOOZE = "com.thelastecho.reminder.ACTION_SNOOZE"
        const val EXTRA_REMINDER_ID = "com.thelastecho.reminder.EXTRA_REMINDER_ID"
    }
}
