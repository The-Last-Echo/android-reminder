package com.thelastecho.reminder

import android.app.Application
import com.thelastecho.reminder.core.notification.ReminderNotificationManager
import com.thelastecho.reminder.core.distribution.DistributionFeatures
import com.thelastecho.reminder.core.distribution.DistributionFeaturesFactory
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.data.attachments.AttachmentStore
import com.thelastecho.reminder.data.local.ReminderDatabase
import com.thelastecho.reminder.data.repository.ReminderRepositoryImpl
import com.thelastecho.reminder.presentation.widget.updateReminderWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ReminderApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        val database = ReminderDatabase.getInstance(this)
        applicationScope.launch {
            ReminderRepositoryImpl(database.reminderDao(), database.categoryDao())
                .getActiveReminders()
                .distinctUntilChanged()
                .collect { updateReminderWidgets(this@ReminderApp) }
        }
        applicationScope.launch {
            preferencesRepository.themeSettings
                .distinctUntilChangedBy { settings ->
                    listOf(
                        settings.themeMode,
                        settings.accentColor,
                        settings.useDynamicColors,
                        settings.customAccentColor,
                        settings.useCustomAccent
                    )
                }
                .collect { updateReminderWidgets(this@ReminderApp) }
        }
        applicationScope.launch {
            val now = System.currentTimeMillis()
            val attachmentMigration = AttachmentStore(this@ReminderApp).migrateLegacyReferences(database.reminderDao())
            preferencesRepository.setUnreadableLegacyAttachmentCount(attachmentMigration.unreadable)
            val settings = preferencesRepository.themeSettings.first()
            if (settings.completedReminderRetentionDays > 0) {
                database.reminderDao().moveExpiredCompletedRemindersToTrash(
                    now - settings.completedReminderRetentionDays * 24L * 60 * 60 * 1000,
                    now,
                    now + 90L * 24 * 60 * 60 * 1000
                )
            }
            database.reminderDao().permanentlyDeleteExpiredReminders(now)
        }
    }
}
