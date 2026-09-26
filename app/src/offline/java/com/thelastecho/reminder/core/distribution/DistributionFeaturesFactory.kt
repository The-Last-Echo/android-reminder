package com.thelastecho.reminder.core.distribution

import android.content.Context

object DistributionFeaturesFactory {
    fun create(context: Context): DistributionFeatures = OfflineDistributionFeatures
}

private object OfflineDistributionFeatures : DistributionFeatures {
    override val updateChecks: UpdateCheckController = OfflineUpdateCheckController
    override val syncEngine: SyncEngine = OfflineSyncEngine
}

private object OfflineUpdateCheckController : UpdateCheckController {
    override val isAvailable: Boolean = false
    override fun schedulePeriodic() = Unit
    override fun enqueueManualCheck() = Unit
}

private object OfflineSyncEngine : SyncEngine {
    override suspend fun synchronize(): SyncResult = SyncResult.Unavailable
}
