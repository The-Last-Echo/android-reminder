package com.thelastecho.reminder.core.distribution

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.thelastecho.reminder.data.local.UpdateCheckWorker
import java.util.concurrent.TimeUnit

object DistributionFeaturesFactory {
    fun create(context: Context): DistributionFeatures = OnlineDistributionFeatures(context.applicationContext)
}

private class OnlineDistributionFeatures(context: Context) : DistributionFeatures {
    override val updateChecks: UpdateCheckController = OnlineUpdateCheckController(context)
    override val syncEngine: SyncEngine = PendingOnlineSyncEngine
}

private class OnlineUpdateCheckController(context: Context) : UpdateCheckController {
    private val workManager = WorkManager.getInstance(context)
    override val isAvailable: Boolean = true

    override fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniquePeriodicWork(
            UpdateCheckWorker.UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun enqueueManualCheck() {
        val request = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
            .setInputData(workDataOf(UpdateCheckWorker.KEY_MANUAL to true))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniqueWork(UpdateCheckWorker.UNIQUE_MANUAL, ExistingWorkPolicy.REPLACE, request)
    }
}

private object PendingOnlineSyncEngine : SyncEngine {
    override suspend fun synchronize(): SyncResult = SyncResult.NotConfigured
}
