package com.thelastecho.reminder.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.thelastecho.reminder.core.alarm.AndroidAlarmScheduler
import com.thelastecho.reminder.core.alarm.ImportedReminderAlarmScheduler
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.data.attachments.AttachmentStore
import com.thelastecho.reminder.data.local.ReminderDatabase
import kotlinx.coroutines.flow.first
import java.io.IOException

class BackupRunner(private val context: Context) {
    data class Result(val succeeded: Boolean, val status: BackupStatus, val retentionWarning: Boolean)

    private val settingsRepository = BackupSettingsRepository(context)
    private val secretStore by lazy { EncryptedBackupSecretStore(context) }
    private val passwordResolver by lazy { BackupPasswordResolver { secretStore.loadPassword() } }

    suspend fun runBackup(): Result {
        val settings = settingsRepository.settings.first()
        val password = try {
            passwordResolver.forAutomatic(settings.encryptionEnabled)
        } catch (_: LocalBackupSecretUnavailableException) {
            return finishFailure(BackupStatus.PASSWORD_REQUIRED)
        } ?: if (settings.encryptionEnabled) return finishFailure(BackupStatus.PASSWORD_REQUIRED) else null
        return performBackup(SafBackupDestination.Origin.AUTOMATIC, password)
    }

    suspend fun runManualBackup(password: CharArray?): Result =
        performBackup(SafBackupDestination.Origin.MANUAL, passwordResolver.forManual(password))

    private suspend fun performBackup(origin: SafBackupDestination.Origin, password: CharArray?): Result {
        val settings = settingsRepository.settings.first()
        val treeUri = settings.destinationFor(origin)?.let(Uri::parse)
        if (treeUri == null) {
            password?.fill('\u0000')
            return if (origin == SafBackupDestination.Origin.AUTOMATIC) finishFailure(BackupStatus.DESTINATION_UNAVAILABLE)
            else Result(false, BackupStatus.DESTINATION_UNAVAILABLE, false)
        }

        var created: SafBackupDestination.BackupDocument? = null
        try {
            created = SafBackupDestination.createBackup(context, treeUri, System.currentTimeMillis(), origin)
            val output = context.contentResolver.openOutputStream(created.uri, "wt")
                ?: throw IOException("The selected folder cannot write backup files.")
            val database = ReminderDatabase.getInstance(context)
            val archiveRepository = ReminderArchiveRepository(
                context,
                database,
                UserPreferencesRepository(context),
                ImportedReminderAlarmScheduler(AndroidAlarmScheduler(context)),
                AttachmentStore(context)
            )
            output.use { archiveRepository.export(it, password) }
        } catch (_: SecurityException) {
            created?.let { runCatching { SafBackupDestination.delete(context, it) } }
            password?.fill('\u0000')
            return if (origin == SafBackupDestination.Origin.AUTOMATIC) finishFailure(BackupStatus.DESTINATION_UNAVAILABLE)
            else Result(false, BackupStatus.DESTINATION_UNAVAILABLE, false)
        } catch (_: Exception) {
            created?.let { runCatching { SafBackupDestination.delete(context, it) } }
            password?.fill('\u0000')
            return if (origin == SafBackupDestination.Origin.AUTOMATIC) finishFailure(BackupStatus.BACKUP_FAILED)
            else Result(false, BackupStatus.BACKUP_FAILED, false)
        } finally {
            password?.fill('\u0000')
        }

        val timestamp = System.currentTimeMillis()
        if (origin == SafBackupDestination.Origin.AUTOMATIC) {
            settingsRepository.setStatus(BackupStatus.NONE)
            settingsRepository.markAutomaticBackup(timestamp)
            val retentionWarning = applyRetention(treeUri, settings.retention)
            settingsRepository.setRetentionWarning(retentionWarning)
            return Result(true, BackupStatus.NONE, retentionWarning)
        }
        settingsRepository.markManualBackup(timestamp)
        return Result(true, BackupStatus.NONE, false)
    }

    private suspend fun applyRetention(treeUri: Uri, retention: BackupRetention): Boolean {
        if (retention == BackupRetention.UNLIMITED) return false
        return try {
            val documents = SafBackupDestination.listBackups(context, treeUri)
                .sortedByDescending { it.displayName }
            var failed = false
            documents.drop(retention.count).forEach { document ->
                if (!runCatching { SafBackupDestination.delete(context, document) }.getOrDefault(false)) failed = true
            }
            if (failed) Log.w(TAG, "Backup retention cleanup could not be completed for the selected destination.")
            failed
        } catch (_: Exception) {
            Log.w(TAG, "Backup retention cleanup could not list the selected destination.")
            true
        }
    }

    private suspend fun finishFailure(status: BackupStatus): Result {
        settingsRepository.setStatus(status)
        return Result(false, status, false)
    }

    companion object {
        private const val TAG = "ReminderBackup"
    }
}

internal object BackupPasswordPolicy {
    fun forAutomatic(encryptionEnabled: Boolean, localPassword: CharArray?): CharArray? =
        if (encryptionEnabled) localPassword else null

    fun forManual(password: CharArray?): CharArray? = password?.takeIf { it.isNotEmpty() }
}

internal class BackupPasswordResolver(private val loadAutomaticPassword: () -> CharArray?) {
    fun forAutomatic(encryptionEnabled: Boolean): CharArray? =
        BackupPasswordPolicy.forAutomatic(
            encryptionEnabled,
            if (encryptionEnabled) loadAutomaticPassword() else null
        )

    fun forManual(password: CharArray?): CharArray? = BackupPasswordPolicy.forManual(password)
}
