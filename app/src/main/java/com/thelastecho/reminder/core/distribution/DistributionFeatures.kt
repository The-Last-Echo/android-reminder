package com.thelastecho.reminder.core.distribution

interface DistributionFeatures {
    val updateChecks: UpdateCheckController
    val syncEngine: SyncEngine
}

interface UpdateCheckController {
    val isAvailable: Boolean
    fun schedulePeriodic()
    fun enqueueManualCheck()
}

interface SyncEngine {
    suspend fun synchronize(): SyncResult
}

sealed interface SyncResult {
    data object Unavailable : SyncResult
    data object NotConfigured : SyncResult
}
