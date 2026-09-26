package com.thelastecho.reminder.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.thelastecho.reminder.core.notification.ReminderNotificationManager
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val title = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_REMINDER_TITLE) ?: context.getString(com.thelastecho.reminder.R.string.app_name)
        val notes = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_REMINDER_NOTES) ?: ""
        val priority = intent.getIntExtra(AndroidAlarmScheduler.EXTRA_REMINDER_PRIORITY, 0)
        val photoUri = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_REMINDER_PHOTO_URI)
        val style = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_REMINDER_NOTIFICATION_STYLE)

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userPreferences = UserPreferencesRepository(appContext)
                val defaultStyle = userPreferences.themeSettings.first().notificationStyle
                val notificationManager = ReminderNotificationManager(appContext, userPreferences)
                notificationManager.showReminderNotification(
                    reminderId = reminderId,
                    title = title,
                    notes = notes,
                    priorityLevel = priority,
                    photoUri = photoUri,
                    reminderStyle = style,
                    defaultStyle = defaultStyle
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
