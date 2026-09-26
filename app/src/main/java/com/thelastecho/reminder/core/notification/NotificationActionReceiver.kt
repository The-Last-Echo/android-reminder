package com.thelastecho.reminder.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.thelastecho.reminder.core.alarm.AndroidAlarmScheduler
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.data.local.ReminderDatabase
import com.thelastecho.reminder.data.repository.ReminderRepositoryImpl
import com.thelastecho.reminder.domain.usecase.DeleteReminderUseCase
import com.thelastecho.reminder.domain.usecase.SnoozeReminderUseCase
import com.thelastecho.reminder.domain.usecase.ToggleReminderCompleteUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import java.util.concurrent.atomic.AtomicBoolean

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return
        val action = intent.action
        val pendingResult = goAsync()
        val resultFinished = AtomicBoolean(false)
        fun finishPendingResult() {
            if (resultFinished.compareAndSet(false, true)) pendingResult.finish()
        }

        try {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val appContext = context.applicationContext
                    val notificationManager = ReminderNotificationManager(
                        appContext,
                        UserPreferencesRepository(appContext)
                    )
                    notificationManager.dismissNotification(reminderId)
                    AlarmSoundService.stopIfPlaying(appContext, reminderId)

                    val database = ReminderDatabase.getInstance(appContext)
                    val repository = ReminderRepositoryImpl(database.reminderDao(), database.categoryDao())
                    val alarmScheduler = AndroidAlarmScheduler(appContext)
                    when (action) {
                        ACTION_COMPLETE -> ToggleReminderCompleteUseCase(repository, alarmScheduler)(reminderId, isCompleted = true)
                        ACTION_SNOOZE -> SnoozeReminderUseCase(repository, alarmScheduler)(reminderId)
                        ACTION_DELETE -> DeleteReminderUseCase(repository, alarmScheduler)(reminderId)
                    }
                } finally {
                    finishPendingResult()
                }
            }
        } catch (failure: Throwable) {
            finishPendingResult()
            throw failure
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.thelastecho.reminder.ACTION_COMPLETE"
        const val ACTION_SNOOZE = "com.thelastecho.reminder.ACTION_SNOOZE"
        const val ACTION_DELETE = "com.thelastecho.reminder.ACTION_DELETE"
        const val EXTRA_REMINDER_ID = "com.thelastecho.reminder.EXTRA_REMINDER_ID"
    }
}
