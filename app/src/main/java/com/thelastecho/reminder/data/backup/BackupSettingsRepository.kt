package com.thelastecho.reminder.data.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.backupDataStore: DataStore<Preferences> by preferencesDataStore(name = "backup_settings")

enum class BackupFrequency(val days: Long) {
    DISABLED(0), DAILY(1), EVERY_THREE_DAYS(3), WEEKLY(7), EVERY_THIRTY_DAYS(30)
}

enum class BackupRetention(val count: Int) {
    THREE(3), FIVE(5), TEN(10), TWENTY(20), UNLIMITED(0)
}

enum class BackupStatus { NONE, DESTINATION_UNAVAILABLE, PASSWORD_REQUIRED, BACKUP_FAILED }

data class BackupSettings(
    val destinationTreeUri: String? = null,
    val manualDestinationTreeUri: String? = null,
    val frequency: BackupFrequency = BackupFrequency.DISABLED,
    val retention: BackupRetention = BackupRetention.FIVE,
    val encryptionEnabled: Boolean = false,
    val retentionWarning: Boolean = false,
    val status: BackupStatus = BackupStatus.NONE,
    val lastAutomaticBackupMillis: Long? = null,
    val lastManualBackupMillis: Long? = null
) {
    internal fun destinationFor(origin: SafBackupDestination.Origin): String? =
        if (origin == SafBackupDestination.Origin.MANUAL) manualDestinationTreeUri ?: destinationTreeUri
        else destinationTreeUri
}

class BackupSettingsRepository(private val context: Context) {
    private object Keys {
        val destination = stringPreferencesKey("destination_tree_uri")
        val manualDestination = stringPreferencesKey("manual_destination_tree_uri")
        val frequency = stringPreferencesKey("frequency")
        val retention = stringPreferencesKey("retention")
        val encrypted = booleanPreferencesKey("encryption_enabled")
        val retentionWarning = booleanPreferencesKey("retention_warning")
        val status = stringPreferencesKey("last_status")
        val lastAutomaticBackup = androidx.datastore.preferences.core.longPreferencesKey("last_automatic_backup")
        val lastManualBackup = androidx.datastore.preferences.core.longPreferencesKey("last_manual_backup")
    }

    val settings: Flow<BackupSettings> = context.backupDataStore.data.map { prefs ->
        BackupSettings(
            destinationTreeUri = prefs[Keys.destination],
            manualDestinationTreeUri = prefs[Keys.manualDestination],
            frequency = enumValue(prefs[Keys.frequency], BackupFrequency.DISABLED),
            retention = enumValue(prefs[Keys.retention], BackupRetention.FIVE),
            encryptionEnabled = prefs[Keys.encrypted] ?: false,
            retentionWarning = prefs[Keys.retentionWarning] ?: false,
            status = enumValue(prefs[Keys.status], BackupStatus.NONE),
            lastAutomaticBackupMillis = prefs[Keys.lastAutomaticBackup],
            lastManualBackupMillis = prefs[Keys.lastManualBackup]
        )
    }

    suspend fun setDestination(uri: String?) = context.backupDataStore.edit { prefs ->
        if (uri == null) prefs.remove(Keys.destination) else prefs[Keys.destination] = uri
    }

    suspend fun setManualDestination(uri: String?) = context.backupDataStore.edit { prefs ->
        if (uri == null) prefs.remove(Keys.manualDestination) else prefs[Keys.manualDestination] = uri
    }

    suspend fun setFrequency(value: BackupFrequency) = context.backupDataStore.edit { it[Keys.frequency] = value.name }
    suspend fun setRetention(value: BackupRetention) = context.backupDataStore.edit { it[Keys.retention] = value.name }
    suspend fun setEncryptionEnabled(value: Boolean) = context.backupDataStore.edit { it[Keys.encrypted] = value }

    suspend fun setRetentionWarning(value: Boolean) = context.backupDataStore.edit { it[Keys.retentionWarning] = value }
    suspend fun setStatus(value: BackupStatus) = context.backupDataStore.edit { it[Keys.status] = value.name }
    suspend fun markAutomaticBackup(millis: Long) = context.backupDataStore.edit { it[Keys.lastAutomaticBackup] = millis }
    suspend fun markManualBackup(millis: Long) = context.backupDataStore.edit { it[Keys.lastManualBackup] = millis }

    private inline fun <reified T : Enum<T>> enumValue(raw: String?, default: T): T =
        raw?.let { value -> runCatching { enumValueOf<T>(value) }.getOrNull() } ?: default
}
