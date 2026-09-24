package com.thelastecho.reminder.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.thelastecho.reminder.core.notification.ReminderNotificationManager

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val title = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_REMINDER_TITLE) ?: context.getString(com.thelastecho.reminder.R.string.app_name)
        val notes = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_REMINDER_NOTES) ?: ""
        val priority = intent.getIntExtra(AndroidAlarmScheduler.EXTRA_REMINDER_PRIORITY, 0)
        val photoUri = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_REMINDER_PHOTO_URI)
        val style = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_REMINDER_NOTIFICATION_STYLE)

        val notificationManager = ReminderNotificationManager(
            context,
            com.thelastecho.reminder.core.preferences.UserPreferencesRepository(context)
        )
        notificationManager.showReminderNotification(
            reminderId = reminderId,
            title = title,
            notes = notes,
            priorityLevel = priority,
            photoUri = photoUri,
            reminderStyle = style
        )
    }
}
