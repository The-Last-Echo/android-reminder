package com.thelastecho.reminder.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.thelastecho.reminder.R
import com.thelastecho.reminder.core.preferences.AppThemeSettings
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
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


    private fun getSettings(): AppThemeSettings = runCatching {
        runBlocking { preferencesRepository.themeSettings.first() }
    }.getOrDefault(AppThemeSettings())

    fun showReminderNotification(
        reminderId: Long,
        title: String,
        notes: String,
        priorityLevel: Int,
        photoUri: String? = null,
        reminderStyle: String? = null
    ) {
        val settings = getSettings()
        val style = reminderStyle?.let { runCatching { NotificationStyle.valueOf(it) }.getOrNull() }
            ?: settings.notificationStyle
        val priority = priorityLevel.coerceIn(0, 3)
        if (style == NotificationStyle.NONE) {
            dismissNotification(reminderId)
            return
        }
        if (style == NotificationStyle.FULL_SCREEN) {
            try {
                AlarmSoundService.start(context, reminderId, title, notes, photoUri)
            } catch (_: RuntimeException) {
                // Android may reject foreground starts; preserve the permitted notification fallback.
                showStandardNotification(reminderId, title, notes, priority, photoUri, style)
            }
            return
        }
        showStandardNotification(reminderId, title, notes, priority, photoUri, style)
    }

    private fun showStandardNotification(reminderId: Long, title: String, notes: String, priority: Int, photoUri: String?, style: NotificationStyle) {
        val channelId = channelId(style, priority)
        ensureReminderChannel(channelId, style, priority)

        val openIntent = if (style == NotificationStyle.FULL_SCREEN) {
            Intent(context, ReminderFullScreenActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_REMINDER_TITLE, title)
                putExtra(EXTRA_REMINDER_NOTES, notes)
                putExtra(EXTRA_REMINDER_PHOTO_URI, photoUri)
                putExtra(AlarmSoundService.EXTRA_START_FROM_VISIBLE_ACTIVITY, true)
            }
        } else {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_REMINDER_ID, reminderId)
            }
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, reminderId.toInt(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(notes.ifBlank { null })
            .setStyle(if (notes.isNotBlank()) NotificationCompat.BigTextStyle().bigText(notes) else null)
            .setPriority(compatPriority(style, priority))
            .setCategory(if (style == NotificationStyle.FULL_SCREEN) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setColor(priorityColor(priority))
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, context.getString(R.string.action_complete), actionPendingIntent(reminderId, NotificationActionReceiver.ACTION_COMPLETE))
            .addAction(0, context.getString(R.string.action_snooze), actionPendingIntent(reminderId, NotificationActionReceiver.ACTION_SNOOZE))

        photoUri?.let { uri ->
            loadNotificationImage(uri)?.let { bitmap ->
                builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon(null as Bitmap?))
            }
        }

        if (style == NotificationStyle.FULL_SCREEN && canUseFullScreenIntent()) {
            val fullScreenIntent = Intent(context, ReminderFullScreenActivity::class.java).apply {
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_REMINDER_TITLE, title)
                putExtra(EXTRA_REMINDER_NOTES, notes)
                putExtra(EXTRA_REMINDER_PHOTO_URI, photoUri)
                putExtra(AlarmSoundService.EXTRA_START_FROM_VISIBLE_ACTIVITY, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context, reminderId.toInt() + 1, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        try {
            NotificationManagerCompat.from(context).notify(reminderId.toInt(), builder.build())
        } catch (_: SecurityException) {
            // The user may have revoked notification access while the alarm was pending.
        }
    }

    fun dismissNotification(reminderId: Long) {
        notificationManager.cancel(reminderId.toInt())
    }

    fun canUseFullScreenIntent(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
        notificationManager.canUseFullScreenIntent()

    private fun actionPendingIntent(reminderId: Long, action: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            ("$action$reminderId").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureReminderChannel(id: String, style: NotificationStyle, priority: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val importance = channelImportance(style, priority)
        val styleName = when (style) {
            NotificationStyle.SIMPLE -> R.string.simple_style_name
            NotificationStyle.HEADS_UP -> R.string.heads_up_style_name
            NotificationStyle.FULL_SCREEN -> R.string.full_screen_style_name
            NotificationStyle.NONE -> R.string.notification_none_name
        }
        val name = context.getString(
            R.string.reminder_channel_name,
            context.getString(styleName),
            context.getString(channelPriorityName(priority))
        )
        val channel = NotificationChannel(id, name, importance).apply {
            description = context.getString(R.string.reminder_channel_description)
            enableVibration(priority >= PRIORITY_MEDIUM && style != NotificationStyle.SIMPLE)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun channelId(style: NotificationStyle, priority: Int): String =
        "reminders_${style.name.lowercase()}"

    private fun channelImportance(style: NotificationStyle, priority: Int): Int = when {
        style == NotificationStyle.NONE -> NotificationManager.IMPORTANCE_NONE
        style == NotificationStyle.SIMPLE -> NotificationManager.IMPORTANCE_LOW
        style == NotificationStyle.HEADS_UP || style == NotificationStyle.FULL_SCREEN -> NotificationManager.IMPORTANCE_HIGH
        else -> NotificationManager.IMPORTANCE_DEFAULT
    }

    private fun compatPriority(style: NotificationStyle, priority: Int): Int = when {
        style == NotificationStyle.NONE -> NotificationCompat.PRIORITY_MIN
        style == NotificationStyle.SIMPLE -> NotificationCompat.PRIORITY_LOW
        style == NotificationStyle.HEADS_UP || style == NotificationStyle.FULL_SCREEN -> NotificationCompat.PRIORITY_HIGH
        else -> NotificationCompat.PRIORITY_DEFAULT
    }

    private fun channelPriorityName(priority: Int): Int = when (priority) {
        PRIORITY_HIGH -> R.string.channel_priority_high
        PRIORITY_MEDIUM -> R.string.channel_priority_medium
        PRIORITY_LOW -> R.string.channel_priority_low
        else -> R.string.channel_priority_none
    }

    private fun priorityColor(priority: Int): Int = when (priority) {
        PRIORITY_HIGH -> 0xFFB00020.toInt()
        PRIORITY_MEDIUM -> 0xFFFF9800.toInt()
        PRIORITY_LOW -> 0xFF2196F3.toInt()
        else -> 0xFF6750A4.toInt()
    }

    private fun loadNotificationImage(rawUri: String): Bitmap? = runCatching {
        val uri = Uri.parse(rawUri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val maxDimension = maxOf(bounds.outWidth, bounds.outHeight)
        val sampleSize = if (maxDimension > 1024) (maxDimension / 1024).coerceAtLeast(1) else 1
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_REMINDER_TITLE = "reminder_title"
        const val EXTRA_REMINDER_NOTES = "reminder_notes"
        const val EXTRA_REMINDER_PHOTO_URI = "reminder_photo_uri"
        private const val PRIORITY_HIGH = 3
        private const val PRIORITY_MEDIUM = 2
        private const val PRIORITY_LOW = 1
    }
}
