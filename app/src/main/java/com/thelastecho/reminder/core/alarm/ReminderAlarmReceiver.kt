package com.thelastecho.reminder.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.thelastecho.reminder.core.debug.ReminderDebugTrace
import com.thelastecho.reminder.core.notification.ReminderNotificationManager
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) {
            ReminderDebugTrace.log(step = "alarm.receiver.missing_id", state = "discarded")
            return
        }

        ReminderDebugTrace.log(
            step = "alarm.receiver.delivered",
            reminderId = reminderId,
            state = "received",
            extra = mapOf("action" to intent.action.orEmpty())
        )

        val title = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_REMINDER_TITLE) ?: context.getString(com.thelastecho.reminder.R.string.app_name)
        val notes = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_REMINDER_NOTES) ?: ""
        val priority = intent.getIntExtra(AndroidAlarmScheduler.EXTRA_REMINDER_PRIORITY, 0)
        val photoUri = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_REMINDER_PHOTO_URI)
        val style = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_REMINDER_NOTIFICATION_STYLE)

        val pendingResult = goAsync()
        val resultFinished = AtomicBoolean(false)
        fun finishPendingResult() {
            if (resultFinished.compareAndSet(false, true)) pendingResult.finish()
        }

        try {
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val userPreferences = UserPreferencesRepository(appContext)
                    val defaultStyle = userPreferences.themeSettings.first().notificationStyle
                    ReminderDebugTrace.log(
                        step = "alarm.receiver.notification.start",
                        reminderId = reminderId,
                        state = "pending",
                        extra = mapOf("defaultStyle" to defaultStyle.name)
                    )
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
                    ReminderDebugTrace.log(
                        step = "alarm.receiver.notification.complete",
                        reminderId = reminderId,
                        state = "posted"
                    )
                } finally {
                    finishPendingResult()
                }
            }
        } catch (failure: Throwable) {
            finishPendingResult()
            throw failure
        }
    }
}
