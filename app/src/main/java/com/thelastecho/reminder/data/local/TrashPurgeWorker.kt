package com.thelastecho.reminder.data.local

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class TrashPurgeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        ReminderDatabase.getInstance(applicationContext).reminderDao().permanentlyDeleteExpiredReminders(System.currentTimeMillis())
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }

}
