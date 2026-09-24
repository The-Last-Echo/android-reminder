package com.thelastecho.reminder.core.notification

import android.app.Notification
import android.app.NotificationChannel
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
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.thelastecho.reminder.R
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.presentation.ReminderFullScreenActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Plays the user's alarm tone for a highest-mode reminder. Its ongoing notification reuses
 * the reminder's notification ID, so it replaces rather than duplicates the reminder card. */
class AlarmSoundService : Service() {
    private var player: MediaPlayer? = null
    private var reminderId: Long = -1L
    private var lastNotificationContent: NotificationContent? = null
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
            stopPlayback()
            return START_NOT_STICKY
        }
        reminderId = intent?.getLongExtra(ReminderNotificationManager.EXTRA_REMINDER_ID, -1L) ?: -1L
        if (reminderId < 0L) {
            stopSelf()
            return START_NOT_STICKY
        }
        val command = intent ?: run { stopSelf(); return START_NOT_STICKY }
        val title = command.getStringExtra(ReminderNotificationManager.EXTRA_REMINDER_TITLE).orEmpty()
        val notes = command.getStringExtra(ReminderNotificationManager.EXTRA_REMINDER_NOTES).orEmpty()
        val photoUri = command.getStringExtra(ReminderNotificationManager.EXTRA_REMINDER_PHOTO_URI)
        lastNotificationContent = NotificationContent(title, notes, photoUri)
        createChannel()
        val notification = buildNotification(reminderId, title, notes, photoUri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(this, reminderId.toInt(), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(reminderId.toInt(), notification)
        }
        startPlayback()
        // Decode photos off the service thread so a large image cannot delay alarm audio.
        photoUri?.let { raw ->
            val currentReminderId = reminderId
            Thread({
                val bitmap = decodeNotificationPhoto(raw) ?: return@Thread
                mainHandler.post {
                    if (reminderId == currentReminderId && player != null) {
                        val withPhoto = buildNotification(currentReminderId, title, notes, raw, bitmap)
                        getSystemService(NotificationManager::class.java).notify(currentReminderId.toInt(), withPhoto)
                    }
                }
            }, "reminder-notification-photo").apply { isDaemon = true }.start()
        }
        return START_NOT_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.full_screen_style_name), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = getString(R.string.alarm_sound_channel_description)
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
    }

    private fun buildNotification(
        id: Long,
        title: String,
        notes: String,
        photoUri: String?,
        photo: android.graphics.Bitmap? = null,
        foreground: Boolean = true
    ): Notification {
        val open = Intent(this, ReminderFullScreenActivity::class.java).apply {
            putExtra(ReminderNotificationManager.EXTRA_REMINDER_ID, id)
            putExtra(ReminderNotificationManager.EXTRA_REMINDER_TITLE, title)
            putExtra(ReminderNotificationManager.EXTRA_REMINDER_NOTES, notes)
            putExtra(ReminderNotificationManager.EXTRA_REMINDER_PHOTO_URI, photoUri)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openIntent = PendingIntent.getActivity(this, id.toInt(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = Intent(this, AlarmSoundService::class.java).setAction(ACTION_STOP)
        val stopIntent = PendingIntent.getService(this, id.toInt(), stop, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val complete = actionPendingIntent(id, NotificationActionReceiver.ACTION_COMPLETE)
        val snooze = actionPendingIntent(id, NotificationActionReceiver.ACTION_SNOOZE)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
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
            .addAction(0, getString(R.string.dismiss), stopIntent)
        photo?.let { notification.setStyle(NotificationCompat.BigPictureStyle().bigPicture(it)) }
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || manager.canUseFullScreenIntent()) {
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
        val saved = runCatching {
            runBlocking { UserPreferencesRepository(applicationContext).themeSettings.first().alarmSoundUri }
        }.getOrNull()
        val uri = saved?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        runCatching {
            player?.release()
            player = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
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

    private fun releasePlayer() {
        runCatching { player?.stop() }
        player?.release()
        player = null
    }

    private fun stopPlayback(showFallback: Boolean = false) {
        mainHandler.removeCallbacks(preparationTimeout)
        mainHandler.removeCallbacks(playbackWatchdog)
        releasePlayer()
        if (reminderId >= 0L) {
            if (showFallback) {
                val extras = lastNotificationContent
                stopForeground(STOP_FOREGROUND_DETACH)
                if (extras != null) {
                    val fallback = buildNotification(reminderId, extras.title, extras.notes, extras.photoUri, foreground = false)
                    getSystemService(NotificationManager::class.java).notify(reminderId.toInt(), fallback)
                } else {
                    getSystemService(NotificationManager::class.java).cancel(reminderId.toInt())
                }
            } else {
                getSystemService(NotificationManager::class.java).cancel(reminderId.toInt())
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }
        stopSelf()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(preparationTimeout)
        mainHandler.removeCallbacks(playbackWatchdog)
        runCatching { player?.stop() }
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.thelastecho.reminder.STOP_ALARM_SOUND"
        const val EXTRA_START_FROM_VISIBLE_ACTIVITY = "start_alarm_sound_from_visible_activity"
        private const val CHANNEL_ID = "alarm_sound_foreground"
        private const val PREPARE_TIMEOUT_MS = 15_000L
        private const val PLAYBACK_WATCHDOG_MS = 30_000L

        fun start(context: Context, reminderId: Long, title: String, notes: String, photoUri: String?) {
            val intent = Intent(context, AlarmSoundService::class.java).apply {
                putExtra(ReminderNotificationManager.EXTRA_REMINDER_ID, reminderId)
                putExtra(ReminderNotificationManager.EXTRA_REMINDER_TITLE, title)
                putExtra(ReminderNotificationManager.EXTRA_REMINDER_NOTES, notes)
                putExtra(ReminderNotificationManager.EXTRA_REMINDER_PHOTO_URI, photoUri)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        }
    }
}
