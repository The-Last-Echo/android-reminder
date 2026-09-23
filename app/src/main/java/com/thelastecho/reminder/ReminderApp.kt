package com.thelastecho.reminder

import android.app.Application
import com.thelastecho.reminder.core.notification.ReminderNotificationManager
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.data.local.ReminderDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ReminderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize notification channels at application startup
        val preferencesRepository = UserPreferencesRepository(this)
        ReminderNotificationManager(this, preferencesRepository)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "trash-retention-purge",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<com.thelastecho.reminder.data.local.TrashPurgeWorker>(1, TimeUnit.DAYS).build()
        )
        CoroutineScope(Dispatchers.IO).launch {
            val cutoff = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
            ReminderDatabase.getInstance(this@ReminderApp).reminderDao().permanentlyDeleteExpiredReminders(cutoff)
        }
    }
}
