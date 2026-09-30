package com.thelastecho.reminder.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.thelastecho.reminder.ReminderApp
import com.thelastecho.reminder.R
import com.thelastecho.reminder.core.debug.ReminderDebugTrace
import com.thelastecho.reminder.data.attachments.AttachmentStore
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
        ensureReminderChannel(NotificationStyle.LIGHT, PRIORITY_HIGH)
        ensureReminderChannel(NotificationStyle.MEDIUM, PRIORITY_HIGH)
        ensureReminderChannel(NotificationStyle.STRONG, PRIORITY_HIGH)
    }

    fun showReminderNotification(
        reminderId: Long,
        title: String,
        notes: String,
        priorityLevel: Int,
        photoUri: String? = null,
        reminderStyle: String? = null,
        defaultStyle: NotificationStyle = NotificationStyle.LIGHT
    ) {
        val style = NotificationStyle.fromPersisted(reminderStyle) ?: defaultStyle

        val priority = priorityLevel.coerceIn(0, 3)

        ReminderDebugTrace.log(
            step = "notification.show.request",
            reminderId = reminderId,
            state = style.name,
            extra = mapOf("priority" to priority.toString(), "fullScreenAllowed" to canUseFullScreenIntent().toString())
        )

        if (style == NotificationStyle.NONE) {
            ReminderDebugTrace.log(
                step = "notification.show.result",
                reminderId = reminderId,
                state = "dismissed",
                extra = mapOf("style" to NotificationStyle.NONE.name)
            )
            AlarmSoundService.stopIfPlaying(context, reminderId)
            dismissNotification(reminderId)
            return
        }

        val launchAlarmScreenDirectly = style.shouldLaunchAlarmScreenDirectly(
            isAppForeground = isAppForeground(),
            canDrawOverlays = Settings.canDrawOverlays(context),
            isDeviceLocked = isDeviceLocked()
        )
        if (style.startsAlarmPlayback) {
            try {
                ReminderDebugTrace.log(
                    step = "notification.foreground_service.start",
                    reminderId = reminderId,
                    state = "pending"
                )
                AlarmSoundService.start(
                    context,
                    reminderId,
                    title,
                    notes,
                    photoUri,
                    priority,
                    allowFullScreenIntent = !launchAlarmScreenDirectly
                )
                if (launchAlarmScreenDirectly) {
                    launchAlarmScreen(reminderId, title, notes, photoUri)
                }
                ReminderDebugTrace.log(
                    step = "notification.foreground_service.result",
                    reminderId = reminderId,
                    state = "started"
                )
            } catch (exception: Exception) {
                ReminderDebugTrace.log(
                    step = "notification.foreground_service.result",
                    reminderId = reminderId,
                    state = "failed",
                    extra = mapOf("reason" to exception.javaClass.simpleName)
                )
                Log.e(TAG, "Could not start the alarm foreground service; posting an explicit notification fallback", exception)
                showStandardNotification(reminderId, title, notes, priority, photoUri, NotificationStyle.MEDIUM)
            }
            return
        }

        ReminderDebugTrace.log(
            step = "notification.standard.start",
            reminderId = reminderId,
            state = style.name
        )
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
        val launchFullScreenDirectly = style.shouldLaunchAlarmScreenDirectly(
            isAppForeground = isAppForeground(),
            canDrawOverlays = Settings.canDrawOverlays(context),
            isDeviceLocked = isDeviceLocked()
        )
        val fullScreenAllowed = style.requestsFullScreenIntent && !launchFullScreenDirectly && canUseFullScreenIntent()
        ensureReminderChannel(style, priority)
        val channelId = channelId(style)

        val openIntent = if (style != NotificationStyle.LIGHT) {
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

        val compatPriority = compatPriority(style)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(notes.ifBlank { null })
            .setStyle(if (notes.isNotBlank()) NotificationCompat.BigTextStyle().bigText(notes) else null)
            .setPriority(compatPriority)
            .setCategory(if (style == NotificationStyle.LIGHT) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_ALARM)
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

        if (style.requestsFullScreenIntent && fullScreenAllowed) {
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
        if (style.requestsFullScreenIntent && fullScreenAllowed && !canUseFullScreenIntent()) {
            ReminderDebugTrace.log(
                step = "notification.post.result",
                reminderId = reminderId,
                state = "revoked_after_check",
                extra = mapOf("fallback" to NotificationStyle.MEDIUM.name)
            )
            Log.w(TAG, "Full-screen intent access was revoked before reminder $reminderId was posted; retrying without full-screen access")
            showStandardNotification(reminderId, title, notes, priority, photoUri, NotificationStyle.MEDIUM)
            return
        }

        try {
            NotificationManagerCompat.from(context).notify(reminderId.toInt(), builder.build())
            ReminderDebugTrace.log(
                step = "notification.post.result",
                reminderId = reminderId,
                state = "posted",
                extra = mapOf("channel" to channelId(style), "fullScreenAllowed" to fullScreenAllowed.toString())
            )
        } catch (exception: SecurityException) {
            ReminderDebugTrace.log(
                step = "notification.post.result",
                reminderId = reminderId,
                state = "security_exception",
                extra = mapOf("reason" to "notification_permission")
            )
            Log.e(TAG, "Notification permission was revoked before posting reminder $reminderId", exception)
        }

        if (launchFullScreenDirectly) launchAlarmScreen(reminderId, title, notes, photoUri)
    }

    private fun isAppForeground(): Boolean {
        return (context.applicationContext as? ReminderApp)?.hasVisibleActivity == true
    }

    private fun isDeviceLocked(): Boolean =
        context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true

    private fun launchAlarmScreen(reminderId: Long, title: String, notes: String, photoUri: String?) {
        val intent = Intent(context, ReminderFullScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_REMINDER_TITLE, title)
            putExtra(EXTRA_REMINDER_NOTES, notes)
            putExtra(EXTRA_REMINDER_PHOTO_URI, photoUri)
        }
        try {
            context.startActivity(intent)
            ReminderDebugTrace.log(
                step = "notification.fullscreen.direct_launch",
                reminderId = reminderId,
                state = "started",
                extra = mapOf("reason" to "app_foreground")
            )
        } catch (exception: Exception) {
            ReminderDebugTrace.log(
                step = "notification.fullscreen.direct_launch",
                reminderId = reminderId,
                state = "failed",
                extra = mapOf("reason" to exception.javaClass.simpleName)
            )
            Log.w(TAG, "Could not directly open the alarm screen while the app was foregrounded", exception)
        }
    }

    fun dismissNotification(reminderId: Long) {
        notificationManager.cancel(reminderId.toInt())
    }

    private fun channelId(style: NotificationStyle): String = when (style) {
        NotificationStyle.LIGHT -> CHANNEL_ID_LIGHT
        NotificationStyle.MEDIUM -> CHANNEL_ID_MEDIUM
        NotificationStyle.STRONG -> CHANNEL_ID_STRONG
        NotificationStyle.NONE -> CHANNEL_ID_LIGHT
    }

    private fun ensureReminderChannel(style: NotificationStyle, priority: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val id = channelId(style)
        if (notificationManager.getNotificationChannel(id) != null) return
        val styleName = when (style) {
            NotificationStyle.LIGHT -> R.string.notification_light_name
            NotificationStyle.MEDIUM -> R.string.notification_medium_name
            NotificationStyle.STRONG -> R.string.notification_strong_name
            NotificationStyle.NONE -> R.string.notification_none_name
        }
        val channel = NotificationChannel(
            id,
            context.getString(R.string.reminder_channel_name, context.getString(styleName), context.getString(channelPriorityName(priority))),
            channelImportance(style)
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
            enableVibration(style == NotificationStyle.STRONG)
            if (style.usesNotificationSound) {
                setSound(
                    Settings.System.DEFAULT_NOTIFICATION_URI,
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            } else {
                setSound(null, null)
            }
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
        NotificationStyle.LIGHT -> NotificationManager.IMPORTANCE_HIGH
        NotificationStyle.MEDIUM, NotificationStyle.STRONG -> NotificationManager.IMPORTANCE_HIGH
        NotificationStyle.NONE -> NotificationManager.IMPORTANCE_NONE
    }

    private fun compatPriority(style: NotificationStyle): Int = when (style) {
        NotificationStyle.LIGHT -> NotificationCompat.PRIORITY_HIGH
        NotificationStyle.MEDIUM, NotificationStyle.STRONG -> NotificationCompat.PRIORITY_HIGH
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
        val attachmentStore = AttachmentStore(context)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        attachmentStore.openReference(rawUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val maxDimension = maxOf(bounds.outWidth, bounds.outHeight)
        val sampleSize = if (maxDimension > 1024) (maxDimension / 1024).coerceAtLeast(1) else 1
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        attachmentStore.openReference(rawUri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_REMINDER_TITLE = "reminder_title"
        const val EXTRA_REMINDER_NOTES = "reminder_notes"
        const val EXTRA_REMINDER_PHOTO_URI = "reminder_photo_uri"

        const val CHANNEL_ID_LIGHT = "reminders_light_v3"
        const val CHANNEL_ID_MEDIUM = "reminders_medium_v2"
        const val CHANNEL_ID_STRONG = "reminders_strong_v2"
        private const val TAG = "ReminderNotifications"

        private const val PRIORITY_HIGH = 3
        private const val PRIORITY_MEDIUM = 2
        private const val PRIORITY_LOW = 1
    }
}
