package com.thelastecho.reminder.data.local

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class TrashPurgeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val cutoff = System.currentTimeMillis() - RETENTION_MILLIS
        ReminderDatabase.getInstance(applicationContext).reminderDao().permanentlyDeleteExpiredReminders(cutoff)
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }

    companion object {
        private const val RETENTION_MILLIS = 90L * 24 * 60 * 60 * 1000
    }
}
