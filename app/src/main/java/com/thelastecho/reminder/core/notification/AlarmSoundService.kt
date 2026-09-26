package com.thelastecho.reminder.core.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.thelastecho.reminder.R
import com.thelastecho.reminder.core.preferences.NotificationStyle
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.presentation.ReminderFullScreenActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Plays the alarm tone while its actionable alarm notification keeps the service in the foreground. */
class AlarmSoundService : Service() {
    private var player: MediaPlayer? = null
    private var reminderId: Long = -1L
    private var lastNotificationContent: NotificationContent? = null
    private var playbackGeneration = 0
    private data class NotificationContent(val title: String, val notes: String, val photoUri: String?)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val preparationTimeout = Runnable { stopPlayback(showFallback = true) }
    private val playbackWatchdog = object : Runnable {
        override fun run() {
            val isPlaying = runCatching { player?.isPlaying == true }.getOrDefault(false)
            if (isPlaying) mainHandler.postDelayed(this, PLAYBACK_WATCHDOG_MS) else stopPlayback(showFallback = true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            val requestedReminderId = intent.getLongExtra(ReminderNotificationManager.EXTRA_REMINDER_ID, -1L)
            if (reminderId == requestedReminderId) {
                stopPlayback()
            } else {
                // A stale alarm card may outlive the currently playing reminder; dismiss only that card.
                if (requestedReminderId >= 0L) {
                    getSystemService(NotificationManager::class.java).cancel(requestedReminderId.toInt())
                }
                if (reminderId < 0L) stopSelf(startId)
            }
            return START_NOT_STICKY
        }
        val command = intent ?: run { stopSelf(startId); return START_NOT_STICKY }
        val newReminderId = command.getLongExtra(ReminderNotificationManager.EXTRA_REMINDER_ID, -1L)
        if (newReminderId < 0L) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (reminderId >= 0L && reminderId != newReminderId) {
            detachPreviousAlarmNotification()
        } else if (reminderId == newReminderId) {
            invalidatePlaybackRequest()
            releasePlayer()
        }
        reminderId = newReminderId
        activeReminderId = reminderId
        val title = command.getStringExtra(ReminderNotificationManager.EXTRA_REMINDER_TITLE).orEmpty()
        val notes = command.getStringExtra(ReminderNotificationManager.EXTRA_REMINDER_NOTES).orEmpty()
        val photoUri = command.getStringExtra(ReminderNotificationManager.EXTRA_REMINDER_PHOTO_URI)
        val priority = command.getIntExtra(EXTRA_REMINDER_PRIORITY, 3)
        lastNotificationContent = NotificationContent(title, notes, photoUri)
        try {
            postAlarmNotification(reminderId, title, notes, photoUri, foreground = true)
        } catch (_: Exception) {
            // If FGS start fails, post a standard Full-Screen notification fallback.
            stopForeground(STOP_FOREGROUND_REMOVE)
            val notificationManager = ReminderNotificationManager(
                applicationContext,
                UserPreferencesRepository(applicationContext)
            )
            notificationManager.showStandardNotification(
                reminderId = reminderId,
                title = title,
                notes = notes,
                priority = priority,
                photoUri = photoUri,
                style = NotificationStyle.FULL_SCREEN
            )
            stopSelf(startId)
            return START_NOT_STICKY
        }
        startPlayback()
        // Decode photos off the service thread so a large image cannot delay alarm audio.
        photoUri?.let { raw ->
            val currentReminderId = reminderId
            Thread({
                val bitmap = decodeNotificationPhoto(raw) ?: return@Thread
                mainHandler.post {
                    if (reminderId == currentReminderId && player != null) {
                        postAlarmNotification(currentReminderId, title, notes, raw, bitmap, foreground = true)
                    }
                }
            }, "reminder-notification-photo").apply { isDaemon = true }.start()
        }
        return START_NOT_STICKY
    }

    private fun postAlarmNotification(
        id: Long,
        title: String,
        notes: String,
        photoUri: String?,
        photo: android.graphics.Bitmap? = null,
        foreground: Boolean = false,
        allowFullScreenIntent: Boolean = true
    ) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val fullScreenAllowed = allowFullScreenIntent && (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                notificationManager.canUseFullScreenIntent()
            )
        if (!fullScreenAllowed) {
            Log.w(TAG, "Full-screen intent access unavailable at post time for reminder $id; posting the high-importance alarm fallback")
        }
        var notification = buildNotification(
            id = id,
            title = title,
            notes = notes,
            photoUri = photoUri,
            photo = photo,
            foreground = foreground,
            fullScreenAllowed = fullScreenAllowed
        )
        // Recheck immediately before posting; users can revoke full-screen access at any time.
        val accessAtPostTime = allowFullScreenIntent && (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                notificationManager.canUseFullScreenIntent()
            )
        if (accessAtPostTime != fullScreenAllowed) {
            notification = buildNotification(
                id = id,
                title = title,
                notes = notes,
                photoUri = photoUri,
                photo = photo,
                foreground = foreground,
                fullScreenAllowed = accessAtPostTime
            )
        }
        if (!accessAtPostTime) {
            Log.w(TAG, "Full-screen intent access unavailable at post time for reminder $id; posting the high-importance alarm fallback")
        }
        if (foreground) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(this, id.toInt(), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(id.toInt(), notification)
            }
        } else {
            notificationManager.notify(id.toInt(), notification)
        }
    }

    private fun buildNotification(
        id: Long,
        title: String,
        notes: String,
        photoUri: String?,
        photo: android.graphics.Bitmap? = null,
        foreground: Boolean = false,
        fullScreenAllowed: Boolean
    ): Notification {
        val open = Intent(this, ReminderFullScreenActivity::class.java).apply {
            putExtra(ReminderNotificationManager.EXTRA_REMINDER_ID, id)
            putExtra(ReminderNotificationManager.EXTRA_REMINDER_TITLE, title)
            putExtra(ReminderNotificationManager.EXTRA_REMINDER_NOTES, notes)
            putExtra(ReminderNotificationManager.EXTRA_REMINDER_PHOTO_URI, photoUri)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openIntent = PendingIntent.getActivity(this, id.toInt(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = Intent(this, AlarmSoundService::class.java).setAction(ACTION_STOP).apply {
            putExtra(ReminderNotificationManager.EXTRA_REMINDER_ID, id)
        }
        val stopIntent = PendingIntent.getService(this, id.toInt(), stop, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val complete = actionPendingIntent(id, NotificationActionReceiver.ACTION_COMPLETE)
        val snooze = actionPendingIntent(id, NotificationActionReceiver.ACTION_SNOOZE)
        val notification = NotificationCompat.Builder(this, ReminderNotificationManager.CHANNEL_ID_FULL_SCREEN)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(notes.ifBlank { getString(R.string.app_name) })
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(foreground)
            .setAutoCancel(!foreground)
            .setContentIntent(openIntent)
            .setDeleteIntent(stopIntent)
            .addAction(0, getString(R.string.action_complete), complete)
            .addAction(0, getString(R.string.action_snooze), snooze)
            .addAction(0, getString(R.string.delete), actionPendingIntent(id, NotificationActionReceiver.ACTION_DELETE))
            .addAction(0, getString(R.string.dismiss), stopIntent)
        photo?.let { notification.setStyle(NotificationCompat.BigPictureStyle().bigPicture(it)) }
        if (fullScreenAllowed) {
            notification.setFullScreenIntent(openIntent, true)
        }
        return notification.build()
    }

    private fun decodeNotificationPhoto(rawUri: String): android.graphics.Bitmap? = runCatching {
        val uri = Uri.parse(rawUri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val maxDimension = maxOf(bounds.outWidth, bounds.outHeight)
        val sample = if (maxDimension > 1024) (maxDimension / 1024).coerceAtLeast(1) else 1
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()

    private fun actionPendingIntent(id: Long, action: String): PendingIntent {
        val intent = Intent(this, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_ID, id)
        }
        return PendingIntent.getBroadcast(this, ("$action$id").hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun startPlayback() {
        mainHandler.removeCallbacks(preparationTimeout)
        mainHandler.removeCallbacks(playbackWatchdog)
        releasePlayer()
        val generation = ++playbackGeneration
        val playbackReminderId = reminderId

        CoroutineScope(Dispatchers.IO).launch {
            val saved = runCatching {
                UserPreferencesRepository(applicationContext).themeSettings.first().alarmSoundUri
            }.getOrNull()
            val uri = saved?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            mainHandler.post {
                if (generation != playbackGeneration || playbackReminderId != reminderId) return@post
                runCatching {
                    player = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        setDataSource(this@AlarmSoundService, uri)
                        isLooping = true
                        setOnErrorListener { _, _, _ -> stopPlayback(showFallback = true); true }
                        setOnPreparedListener { mediaPlayer ->
                            mainHandler.removeCallbacks(preparationTimeout)
                            runCatching {
                                mediaPlayer.start()
                                mainHandler.postDelayed(playbackWatchdog, PLAYBACK_WATCHDOG_MS)
                            }.onFailure { stopPlayback(showFallback = true) }
                        }
                        prepareAsync()
                    }
                    mainHandler.postDelayed(preparationTimeout, PREPARE_TIMEOUT_MS)
                }.onFailure { stopPlayback(showFallback = true) }
            }
        }
    }

    private fun releasePlayer() {
        runCatching { player?.stop() }
        player?.release()
        player = null
    }

    private fun invalidatePlaybackRequest() {
        playbackGeneration++
        mainHandler.removeCallbacks(preparationTimeout)
        mainHandler.removeCallbacks(playbackWatchdog)
    }

    private fun detachPreviousAlarmNotification() {
        val previousId = reminderId
        val previousContent = lastNotificationContent
        invalidatePlaybackRequest()
        releasePlayer()
        stopForeground(STOP_FOREGROUND_DETACH)
        if (previousId >= 0L && previousContent != null) {
            postAlarmNotification(
                previousId,
                previousContent.title,
                previousContent.notes,
                previousContent.photoUri,
                foreground = false,
                allowFullScreenIntent = false
            )
        }
    }

    private fun stopPlayback(showFallback: Boolean = false) {
        invalidatePlaybackRequest()
        releasePlayer()
        if (reminderId >= 0L) {
            if (showFallback) {
                val extras = lastNotificationContent
                stopForeground(STOP_FOREGROUND_REMOVE)
                if (extras != null) {
                    postAlarmNotification(
                        reminderId,
                        extras.title,
                        extras.notes,
                        extras.photoUri,
                        foreground = false,
                        allowFullScreenIntent = false
                    )
                } else {
                    getSystemService(NotificationManager::class.java).cancel(reminderId.toInt())
                }
            } else {
                getSystemService(NotificationManager::class.java).cancel(reminderId.toInt())
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }
        if (activeReminderId == reminderId) activeReminderId = -1L
        stopSelf()
    }

    override fun onDestroy() {
        invalidatePlaybackRequest()
        runCatching { player?.stop() }
        player?.release()
        player = null
        if (activeReminderId == reminderId) activeReminderId = -1L
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.thelastecho.reminder.STOP_ALARM_SOUND"
        const val EXTRA_START_FROM_VISIBLE_ACTIVITY = "start_alarm_sound_from_visible_activity"
        const val EXTRA_REMINDER_PRIORITY = "extra_reminder_priority"
        private const val TAG = "AlarmSoundService"
        private const val PREPARE_TIMEOUT_MS = 15_000L
        private const val PLAYBACK_WATCHDOG_MS = 30_000L
        @Volatile private var activeReminderId: Long = -1L

        fun stopIfPlaying(context: Context, reminderId: Long) {
            if (activeReminderId == reminderId) context.stopService(Intent(context, AlarmSoundService::class.java))
        }

        fun start(context: Context, reminderId: Long, title: String, notes: String, photoUri: String?, priority: Int = 3) {
            val intent = Intent(context, AlarmSoundService::class.java).apply {
                putExtra(ReminderNotificationManager.EXTRA_REMINDER_ID, reminderId)
                putExtra(ReminderNotificationManager.EXTRA_REMINDER_TITLE, title)
                putExtra(ReminderNotificationManager.EXTRA_REMINDER_NOTES, notes)
                putExtra(ReminderNotificationManager.EXTRA_REMINDER_PHOTO_URI, photoUri)
                putExtra(EXTRA_REMINDER_PRIORITY, priority)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        }
    }
}
