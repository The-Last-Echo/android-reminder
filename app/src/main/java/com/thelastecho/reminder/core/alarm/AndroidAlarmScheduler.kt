package com.thelastecho.reminder.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.thelastecho.reminder.core.debug.ReminderDebugTrace
import com.thelastecho.reminder.domain.model.Reminder

class AndroidAlarmScheduler(
    private val context: Context
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(reminder: Reminder) {
        val dueTime = reminder.dueDateTimeEpochMillis ?: return
        if (dueTime <= System.currentTimeMillis() || reminder.isCompleted) return

        ReminderDebugTrace.log(
            step = "alarm.schedule.request",
            reminderId = reminder.id,
            state = "pending",
            extra = mapOf(
                "dueTime" to dueTime.toString(),
                "exactAlarmAccess" to canScheduleExactAlarms().toString()
            )
        )

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_REMINDER_TITLE, reminder.title)
            putExtra(EXTRA_REMINDER_NOTES, reminder.notes)
            putExtra(EXTRA_REMINDER_PRIORITY, reminder.priority.level)
            putExtra(EXTRA_REMINDER_PHOTO_URI, reminder.imagePath ?: reminder.legacyImageUri)
            putExtra(EXTRA_REMINDER_NOTIFICATION_STYLE, reminder.notificationStyle)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    dueTime,
                    pendingIntent
                )
                ReminderDebugTrace.log(
                    step = "alarm.schedule.result",
                    reminderId = reminder.id,
                    state = "setExactAndAllowWhileIdle",
                    extra = mapOf("dueTime" to dueTime.toString())
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    dueTime,
                    pendingIntent
                )
                ReminderDebugTrace.log(
                    step = "alarm.schedule.result",
                    reminderId = reminder.id,
                    state = "setAndAllowWhileIdle",
                    extra = mapOf("dueTime" to dueTime.toString())
                )
            }
        } catch (e: SecurityException) {
            // In case exact alarm permission was revoked, fallback safely
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                dueTime,
                pendingIntent
            )
            ReminderDebugTrace.log(
                step = "alarm.schedule.result",
                reminderId = reminder.id,
                state = "fallback.setAndAllowWhileIdle",
                extra = mapOf("dueTime" to dueTime.toString(), "reason" to "security_exception")
            )
        }
    }

    override fun cancel(reminderId: Long) {
        ReminderDebugTrace.log(
            step = "alarm.cancel.request",
            reminderId = reminderId,
            state = "pending"
        )
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        ReminderDebugTrace.log(
            step = "alarm.cancel.result",
            reminderId = reminderId,
            state = "cancelled"
        )
    }

    override fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
        const val EXTRA_REMINDER_NOTES = "extra_reminder_notes"
        const val EXTRA_REMINDER_PRIORITY = "extra_reminder_priority"
        const val EXTRA_REMINDER_PHOTO_URI = "extra_reminder_photo_uri"
        const val EXTRA_REMINDER_NOTIFICATION_STYLE = "extra_reminder_notification_style"
    }
}
