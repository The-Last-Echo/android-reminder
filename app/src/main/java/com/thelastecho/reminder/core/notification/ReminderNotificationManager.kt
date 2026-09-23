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
import com.thelastecho.reminder.presentation.ReminderFullScreenActivity
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
            val simpleChannel = NotificationChannel(SIMPLE_CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = descriptionText
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(simpleChannel)
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
        priorityLevel: Int,
        photoUri: String? = null,
        reminderStyle: String? = null
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

        val notificationStyle = reminderStyle?.let { runCatching { NotificationStyle.valueOf(it) }.getOrNull() } ?: getNotificationStyle()

        val channelId = if (notificationStyle == NotificationStyle.SIMPLE) SIMPLE_CHANNEL_ID else CHANNEL_ID
        val builder = NotificationCompat.Builder(context, channelId)
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

        photoUri?.let { rawUri ->
            runCatching {
                context.contentResolver.openInputStream(android.net.Uri.parse(rawUri))?.use { stream -> android.graphics.BitmapFactory.decodeStream(stream) }
            }.getOrNull()?.let { bitmap ->
                builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon(null as android.graphics.Bitmap?))
            }
        }

        // Apply notification style
        when (notificationStyle) {
            NotificationStyle.SIMPLE -> {
                builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Simple notifications don't interrupt
                }
            }
            NotificationStyle.FULL_SCREEN -> {
                val fullScreenIntent = Intent(context, ReminderFullScreenActivity::class.java).apply {
                    putExtra("reminder_id", reminderId)
                    putExtra("reminder_title", title)
                    putExtra("reminder_notes", notes)
                    putExtra("reminder_photo_uri", photoUri)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val fullScreenPendingIntent = PendingIntent.getActivity(context, reminderId.toInt() + 1, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                builder.setFullScreenIntent(fullScreenPendingIntent, true)
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
        const val SIMPLE_CHANNEL_ID = "reminder_notifications_simple_channel"
    }
}
