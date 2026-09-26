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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.thelastecho.reminder.R
import com.thelastecho.reminder.core.preferences.NotificationStyle
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.presentation.MainActivity
import com.thelastecho.reminder.presentation.ReminderFullScreenActivity

class ReminderNotificationManager(
    private val context: Context,
    val preferencesRepository: UserPreferencesRepository
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /** Create stable style channels once at app startup while preserving any user channel choices. */
    fun ensureReminderChannels() {
        ensureReminderChannel(NotificationStyle.SIMPLE, PRIORITY_HIGH)
        ensureReminderChannel(NotificationStyle.HEADS_UP, PRIORITY_HIGH)
        ensureReminderChannel(NotificationStyle.FULL_SCREEN, PRIORITY_HIGH)
    }

    fun showReminderNotification(
        reminderId: Long,
        title: String,
        notes: String,
        priorityLevel: Int,
        photoUri: String? = null,
        reminderStyle: String? = null,
        defaultStyle: NotificationStyle = NotificationStyle.HEADS_UP
    ) {
        val style = reminderStyle?.let { runCatching { NotificationStyle.valueOf(it) }.getOrNull() }
            ?: defaultStyle

        val priority = priorityLevel.coerceIn(0, 3)

        if (style == NotificationStyle.NONE) {
            dismissNotification(reminderId)
            return
        }

        if (style == NotificationStyle.FULL_SCREEN) {
            if (!canUseFullScreenIntent()) {
                Log.w(TAG, "Full-screen intent access unavailable; using the high-importance alarm notification fallback")
            }
            try {
                AlarmSoundService.start(context, reminderId, title, notes, photoUri, priority)
            } catch (exception: Exception) {
                Log.e(TAG, "Could not start the alarm foreground service; posting an explicit notification fallback", exception)
                showStandardNotification(reminderId, title, notes, priority, photoUri, style)
            }
            return
        }

        showStandardNotification(reminderId, title, notes, priority, photoUri, style)
    }

    fun showStandardNotification(
        reminderId: Long,
        title: String,
        notes: String,
        priority: Int,
        photoUri: String?,
        style: NotificationStyle
    ) {
        val fullScreenAllowed = canUseFullScreenIntent()
        val effectiveStyle = if (style == NotificationStyle.FULL_SCREEN && !fullScreenAllowed) {
            Log.w(TAG, "Full-screen intent access unavailable; explicitly falling back to the Heads-up channel")
            NotificationStyle.HEADS_UP
        } else {
            style
        }
        ensureReminderChannel(effectiveStyle, priority)
        val channelId = channelId(effectiveStyle)

        val openIntent = if (style == NotificationStyle.FULL_SCREEN) {
            Intent(context, ReminderFullScreenActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_REMINDER_TITLE, title)
                putExtra(EXTRA_REMINDER_NOTES, notes)
                putExtra(EXTRA_REMINDER_PHOTO_URI, photoUri)
            }
        } else {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_REMINDER_ID, reminderId)
            }
        }

        val openPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val compatPriority = compatPriority(effectiveStyle)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(notes.ifBlank { null })
            .setStyle(if (notes.isNotBlank()) NotificationCompat.BigTextStyle().bigText(notes) else null)
            .setPriority(compatPriority)
            .setCategory(if (style == NotificationStyle.FULL_SCREEN) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setColor(priorityColor(priority))
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, context.getString(R.string.action_complete), actionPendingIntent(reminderId, NotificationActionReceiver.ACTION_COMPLETE))
            .addAction(0, context.getString(R.string.action_snooze), actionPendingIntent(reminderId, NotificationActionReceiver.ACTION_SNOOZE))
            .addAction(0, context.getString(R.string.delete), actionPendingIntent(reminderId, NotificationActionReceiver.ACTION_DELETE))

        photoUri?.let { uri ->
            loadNotificationImage(uri)?.let { bitmap ->
                builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon(null as Bitmap?))
            }
        }

        if (style == NotificationStyle.FULL_SCREEN && fullScreenAllowed) {
            val fullScreenIntent = Intent(context, ReminderFullScreenActivity::class.java).apply {
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_REMINDER_TITLE, title)
                putExtra(EXTRA_REMINDER_NOTES, notes)
                putExtra(EXTRA_REMINDER_PHOTO_URI, photoUri)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                reminderId.toInt() + 10000,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        // Recheck immediately before posting: Android 14+ lets the user revoke FSI access at any time.
        if (style == NotificationStyle.FULL_SCREEN && fullScreenAllowed && !canUseFullScreenIntent()) {
            Log.w(TAG, "Full-screen intent access was revoked before reminder $reminderId was posted; retrying on the Heads-up channel")
            showStandardNotification(reminderId, title, notes, priority, photoUri, NotificationStyle.HEADS_UP)
            return
        }

        try {
            NotificationManagerCompat.from(context).notify(reminderId.toInt(), builder.build())
        } catch (exception: SecurityException) {
            Log.e(TAG, "Notification permission was revoked before posting reminder $reminderId", exception)
        }
    }

    fun dismissNotification(reminderId: Long) {
        notificationManager.cancel(reminderId.toInt())
    }

    private fun channelId(style: NotificationStyle): String = when (style) {
        NotificationStyle.SIMPLE -> CHANNEL_ID_SIMPLE
        NotificationStyle.HEADS_UP -> CHANNEL_ID_HEADS_UP
        NotificationStyle.FULL_SCREEN -> CHANNEL_ID_FULL_SCREEN
        NotificationStyle.NONE -> CHANNEL_ID_SIMPLE
    }

    private fun ensureReminderChannel(style: NotificationStyle, priority: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val id = channelId(style)
        if (notificationManager.getNotificationChannel(id) != null) return
        val styleName = when (style) {
            NotificationStyle.SIMPLE -> R.string.simple_style_name
            NotificationStyle.HEADS_UP -> R.string.heads_up_style_name
            NotificationStyle.FULL_SCREEN -> R.string.full_screen_style_name
            NotificationStyle.NONE -> R.string.notification_none_name
        }
        val channel = NotificationChannel(
            id,
            context.getString(R.string.reminder_channel_name, context.getString(styleName), context.getString(channelPriorityName(priority))),
            channelImportance(style)
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
            enableVibration(priority >= PRIORITY_MEDIUM && style != NotificationStyle.SIMPLE)
            setShowBadge(true)
        }
        try {
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Created notification channel id=$id style=$style importance=${channel.importance}")
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to create notification channel id=$id style=$style", exception)
        }
    }

    private fun channelImportance(style: NotificationStyle): Int = when (style) {
        NotificationStyle.SIMPLE -> NotificationManager.IMPORTANCE_LOW
        NotificationStyle.HEADS_UP, NotificationStyle.FULL_SCREEN -> NotificationManager.IMPORTANCE_HIGH
        NotificationStyle.NONE -> NotificationManager.IMPORTANCE_NONE
    }

    private fun compatPriority(style: NotificationStyle): Int = when (style) {
        NotificationStyle.SIMPLE -> NotificationCompat.PRIORITY_LOW
        NotificationStyle.HEADS_UP, NotificationStyle.FULL_SCREEN -> NotificationCompat.PRIORITY_HIGH
        NotificationStyle.NONE -> NotificationCompat.PRIORITY_MIN
    }

    private fun channelPriorityName(priority: Int): Int = when (priority) {
        PRIORITY_HIGH -> R.string.channel_priority_high
        PRIORITY_MEDIUM -> R.string.channel_priority_medium
        PRIORITY_LOW -> R.string.channel_priority_low
        else -> R.string.channel_priority_none
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

        const val CHANNEL_ID_SIMPLE = "reminders_simple"
        const val CHANNEL_ID_HEADS_UP = "reminders_heads_up"
        const val CHANNEL_ID_FULL_SCREEN = "reminders_full_screen"
        private const val TAG = "ReminderNotifications"

        private const val PRIORITY_HIGH = 3
        private const val PRIORITY_MEDIUM = 2
        private const val PRIORITY_LOW = 1
    }
}
