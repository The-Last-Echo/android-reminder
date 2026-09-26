package com.thelastecho.reminder.core.notification

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Read-only snapshot of the effective notification access and channel settings on this device. */
data class NotificationDiagnosticsSnapshot(
    val postNotificationsPermission: AccessState,
    val appNotificationsEnabled: Boolean,
    val fullScreenIntentAccess: AccessState,
    val channels: List<ChannelState>
) {
    enum class AccessState { ALLOWED, BLOCKED, NOT_REQUIRED, NOT_AVAILABLE }

    data class ChannelState(
        val id: String,
        val name: String,
        val importance: Int?,
        val hasSound: Boolean?,
        val soundUri: String?,
        val vibrates: Boolean?
    )
}

object NotificationDiagnostics {
    fun read(context: Context): NotificationDiagnosticsSnapshot {
        val appContext = context.applicationContext
        val notificationManager = appContext.getSystemService(NotificationManager::class.java)
        val postPermission = when {
            Build.VERSION.SDK_INT < 33 -> NotificationDiagnosticsSnapshot.AccessState.NOT_REQUIRED
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> NotificationDiagnosticsSnapshot.AccessState.ALLOWED
            else -> NotificationDiagnosticsSnapshot.AccessState.BLOCKED
        }
        val fullScreenAccess = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> NotificationDiagnosticsSnapshot.AccessState.NOT_REQUIRED
            notificationManager.canUseFullScreenIntent() -> NotificationDiagnosticsSnapshot.AccessState.ALLOWED
            else -> NotificationDiagnosticsSnapshot.AccessState.BLOCKED
        }
        val channels = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            buildList {
                add(ReminderNotificationManager.CHANNEL_ID_SIMPLE)
                add(ReminderNotificationManager.CHANNEL_ID_HEADS_UP)
                // This is the channel used by the visible Full-Screen alarm notification.
                add(ReminderNotificationManager.CHANNEL_ID_FULL_SCREEN)
            }.map { id ->
                val channel = notificationManager.getNotificationChannel(id)
                NotificationDiagnosticsSnapshot.ChannelState(
                    id = id,
                    name = channel?.name?.toString() ?: id,
                    importance = channel?.importance,
                    hasSound = channel?.sound != null,
                    soundUri = channel?.sound?.toString(),
                    vibrates = channel?.shouldVibrate()
                )
            }
        } else {
            emptyList()
        }
        return NotificationDiagnosticsSnapshot(
            postNotificationsPermission = postPermission,
            appNotificationsEnabled = NotificationManagerCompat.from(appContext).areNotificationsEnabled(),
            fullScreenIntentAccess = fullScreenAccess,
            channels = channels
        )
    }
}
