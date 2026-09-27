package com.thelastecho.reminder

import android.app.Application
import com.thelastecho.reminder.core.notification.ReminderNotificationManager
import com.thelastecho.reminder.core.distribution.DistributionFeatures
import com.thelastecho.reminder.core.distribution.DistributionFeaturesFactory
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.data.attachments.AttachmentStore
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

    lateinit var distributionFeatures: DistributionFeatures
        private set

    override fun onCreate() {
        super.onCreate()
        val preferencesRepository = UserPreferencesRepository(this)
        // Flavor services are composed here without a DI framework: Gradle selects one factory from src/offline or src/online.
        distributionFeatures = DistributionFeaturesFactory.create(this)
        ReminderNotificationManager(this, preferencesRepository).ensureReminderChannels()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "trash-retention-purge",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<com.thelastecho.reminder.data.local.TrashPurgeWorker>(1, TimeUnit.DAYS).build()
        )
        distributionFeatures.updateChecks.schedulePeriodic()
        CoroutineScope(Dispatchers.IO).launch {
            val now = System.currentTimeMillis()
            val db = ReminderDatabase.getInstance(this@ReminderApp)
            val attachmentMigration = AttachmentStore(this@ReminderApp).migrateLegacyReferences(db.reminderDao())
            preferencesRepository.setUnreadableLegacyAttachmentCount(attachmentMigration.unreadable)
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
