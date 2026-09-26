package com.thelastecho.reminder

import android.app.Application
import com.thelastecho.reminder.core.notification.ReminderNotificationManager
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.data.local.ReminderDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ReminderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val preferencesRepository = UserPreferencesRepository(this)
        ReminderNotificationManager(this, preferencesRepository).ensureReminderChannels()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "trash-retention-purge",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<com.thelastecho.reminder.data.local.TrashPurgeWorker>(1, TimeUnit.DAYS).build()
        )
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            com.thelastecho.reminder.data.local.UpdateCheckWorker.UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<com.thelastecho.reminder.data.local.UpdateCheckWorker>(1, TimeUnit.DAYS)
                .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build())
                .build()
        )
        CoroutineScope(Dispatchers.IO).launch {
            val now = System.currentTimeMillis()
            val db = ReminderDatabase.getInstance(this@ReminderApp)
            val settings = preferencesRepository.themeSettings.first()
            if (settings.completedReminderRetentionDays > 0) {
                db.reminderDao().moveExpiredCompletedRemindersToTrash(
                    now - settings.completedReminderRetentionDays * 24L * 60 * 60 * 1000,
                    now,
                    now + 90L * 24 * 60 * 60 * 1000
                )
            }
            db.reminderDao().permanentlyDeleteExpiredReminders(now)
        }
    }
}
