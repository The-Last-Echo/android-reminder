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

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(UNDO_CHANNEL_ID, context.getString(R.string.undo_channel_name), NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

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
        val canBypassDnd = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            settings.allowUrgentDndBypass && priority == PRIORITY_HIGH && style != NotificationStyle.SIMPLE &&
            notificationManager.isNotificationPolicyAccessGranted
        val channelId = channelId(style, priority, canBypassDnd)
        ensureReminderChannel(channelId, style, priority, canBypassDnd)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_REMINDER_ID, reminderId)
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
            .setCategory(if (style == NotificationStyle.FULL_SCREEN && priority == PRIORITY_HIGH) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
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

        if (style == NotificationStyle.FULL_SCREEN && priority == PRIORITY_HIGH && canUseFullScreenIntent()) {
            val fullScreenIntent = Intent(context, ReminderFullScreenActivity::class.java).apply {
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_REMINDER_TITLE, title)
                putExtra(EXTRA_REMINDER_NOTES, notes)
                putExtra(EXTRA_REMINDER_PHOTO_URI, photoUri)
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

    fun showUndoDeleteNotification(reminderId: Long, title: String) {
        val undoIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_UNDO_DELETE
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val undoPendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() xor UNDO_REQUEST_CODE_MASK,
            undoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, UNDO_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.reminder_moved_to_trash))
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(UNDO_TIMEOUT_MILLIS)
            .addAction(0, context.getString(R.string.undo), undoPendingIntent)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(undoNotificationId(reminderId), notification) }
    }

    fun dismissNotification(reminderId: Long) {
        notificationManager.cancel(reminderId.toInt())
    }

    fun dismissUndoDeleteNotification(reminderId: Long) {
        notificationManager.cancel(undoNotificationId(reminderId))
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

    private fun ensureReminderChannel(id: String, style: NotificationStyle, priority: Int, canBypassDnd: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val importance = channelImportance(style, priority)
        val styleName = when (style) {
            NotificationStyle.SIMPLE -> R.string.simple_style_name
            NotificationStyle.HEADS_UP -> R.string.heads_up_style_name
            NotificationStyle.FULL_SCREEN -> R.string.full_screen_style_name
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
            if (canBypassDnd) setBypassDnd(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun channelId(style: NotificationStyle, priority: Int, canBypassDnd: Boolean): String =
        "reminders_${style.name.lowercase()}_p$priority${if (canBypassDnd) "_dnd" else ""}"

    private fun channelImportance(style: NotificationStyle, priority: Int): Int = when {
        style == NotificationStyle.SIMPLE && priority >= PRIORITY_HIGH -> NotificationManager.IMPORTANCE_DEFAULT
        style == NotificationStyle.SIMPLE && priority == PRIORITY_MEDIUM -> NotificationManager.IMPORTANCE_LOW
        style == NotificationStyle.SIMPLE -> NotificationManager.IMPORTANCE_MIN
        priority >= PRIORITY_HIGH -> NotificationManager.IMPORTANCE_HIGH
        priority == PRIORITY_MEDIUM -> NotificationManager.IMPORTANCE_DEFAULT
        priority == PRIORITY_LOW -> NotificationManager.IMPORTANCE_LOW
        else -> NotificationManager.IMPORTANCE_MIN
    }

    private fun compatPriority(style: NotificationStyle, priority: Int): Int = when {
        style == NotificationStyle.SIMPLE && priority >= PRIORITY_HIGH -> NotificationCompat.PRIORITY_DEFAULT
        style == NotificationStyle.SIMPLE && priority == PRIORITY_MEDIUM -> NotificationCompat.PRIORITY_LOW
        style == NotificationStyle.SIMPLE -> NotificationCompat.PRIORITY_MIN
        priority >= PRIORITY_HIGH -> NotificationCompat.PRIORITY_MAX
        priority == PRIORITY_MEDIUM -> NotificationCompat.PRIORITY_DEFAULT
        priority == PRIORITY_LOW -> NotificationCompat.PRIORITY_LOW
        else -> NotificationCompat.PRIORITY_MIN
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

    private fun undoNotificationId(reminderId: Long) = reminderId.toInt() xor UNDO_NOTIFICATION_ID_MASK

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_REMINDER_TITLE = "reminder_title"
        const val EXTRA_REMINDER_NOTES = "reminder_notes"
        const val EXTRA_REMINDER_PHOTO_URI = "reminder_photo_uri"
        private const val UNDO_CHANNEL_ID = "reminder_undo_channel"
        private const val UNDO_NOTIFICATION_ID_MASK = 0x40000000
        private const val UNDO_REQUEST_CODE_MASK = 0x20000000
        private const val UNDO_TIMEOUT_MILLIS = 30_000L
        private const val PRIORITY_HIGH = 3
        private const val PRIORITY_MEDIUM = 2
        private const val PRIORITY_LOW = 1
    }
}
