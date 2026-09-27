package com.thelastecho.reminder.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class ReminderBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val result = BackupRunner(applicationContext).runBackup()
        when {
            result.succeeded -> Result.success()
            result.status == BackupStatus.DESTINATION_UNAVAILABLE || result.status == BackupStatus.PASSWORD_REQUIRED -> Result.failure()
            else -> Result.retry()
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        BackupSettingsRepository(applicationContext).setStatus(BackupStatus.BACKUP_FAILED)
        Result.retry()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "reminder-automatic-backup"
    }
}

object BackupWorkScheduler {
    fun synchronize(context: Context, settings: BackupSettings) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (settings.frequency == BackupFrequency.DISABLED || settings.destinationTreeUri.isNullOrBlank()) {
            workManager.cancelUniqueWork(ReminderBackupWorker.UNIQUE_WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<ReminderBackupWorker>(settings.frequency.days, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(
            ReminderBackupWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
