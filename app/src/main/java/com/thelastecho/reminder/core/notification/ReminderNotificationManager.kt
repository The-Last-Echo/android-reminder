package com.thelastecho.reminder.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.thelastecho.reminder.R
import com.thelastecho.reminder.core.preferences.NotificationStyle
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.presentation.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ReminderNotificationManager(
    private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.channel_reminders_name)
            val descriptionText = context.getString(R.string.channel_reminders_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getNotificationStyle(): NotificationStyle {
        return try {
            runBlocking {
                preferencesRepository.themeSettings.first().notificationStyle
            }
        } catch (e: Exception) {
            NotificationStyle.HEADS_UP
        }
    }

    fun showReminderNotification(
        reminderId: Long,
        title: String,
        notes: String,
        priorityLevel: Int
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("reminder_id", reminderId)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Complete action button
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_COMPLETE
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            ("complete_$reminderId").hashCode(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action button (10 minutes)
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            ("snooze_$reminderId").hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationColor = when (priorityLevel) {
            3 -> 0xFFB00020.toInt() // High
            2 -> 0xFFFF9800.toInt() // Medium
            1 -> 0xFF2196F3.toInt() // Low
            else -> 0xFF6750A4.toInt()
        }

        val notificationStyle = getNotificationStyle()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(notes.ifBlank { null })
            .setStyle(if (notes.isNotBlank()) NotificationCompat.BigTextStyle().bigText(notes) else null)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(notificationColor)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .addAction(0, context.getString(R.string.action_complete), completePendingIntent)
            .addAction(0, context.getString(R.string.action_snooze), snoozePendingIntent)

        // Apply notification style
        when (notificationStyle) {
            NotificationStyle.SIMPLE -> {
                builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Simple notifications don't interrupt
                }
            }
            NotificationStyle.FULL_SCREEN -> {
                builder.setFullScreenIntent(tapPendingIntent, true)
                builder.setPriority(NotificationCompat.PRIORITY_MAX)
            }
            NotificationStyle.HEADS_UP -> {
                builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Heads-up is handled by channel importance
                }
            }
        }

        try {
            NotificationManagerCompat.from(context).notify(reminderId.toInt(), builder.build())
        } catch (e: SecurityException) {
            // In case notification permission was revoked at runtime
        }
    }

    fun dismissNotification(reminderId: Long) {
        notificationManager.cancel(reminderId.toInt())
    }

    companion object {
        const val CHANNEL_ID = "reminder_notifications_channel"
    }
}
