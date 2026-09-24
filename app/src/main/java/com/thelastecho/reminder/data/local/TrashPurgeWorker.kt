package com.thelastecho.reminder.data.local

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first

class TrashPurgeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val now = System.currentTimeMillis()
        val database = ReminderDatabase.getInstance(applicationContext)
        val preferences = com.thelastecho.reminder.core.preferences.UserPreferencesRepository(applicationContext).themeSettings.first()
        val retentionDays = preferences.completedReminderRetentionDays
        if (retentionDays > 0) {
            database.reminderDao().moveExpiredCompletedRemindersToTrash(
                now - retentionDays * 24L * 60 * 60 * 1000,
                now,
                now + 90L * 24 * 60 * 60 * 1000
            )
        }
        database.reminderDao().permanentlyDeleteExpiredReminders(now)
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }

}
