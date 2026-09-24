package com.thelastecho.reminder.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

data class AppThemeSettings(
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    val isAmoledMode: Boolean = true,
    val useDynamicColors: Boolean = true,
    val notificationStyle: NotificationStyle = NotificationStyle.HEADS_UP,
    val alarmSoundUri: String? = null,
    val completedReminderRetentionDays: Int = 0,
    val addButtonOnLeft: Boolean = false,
    val automaticUpdateChecks: Boolean = false,
    val lastUpdateCheckMillis: Long = 0L,
    val latestReleaseTag: String? = null,
    val latestReleaseUrl: String? = null,
    val notificationPermissionAsked: Boolean = false
)

enum class NotificationStyle {
    SIMPLE,
    FULL_SCREEN,
    HEADS_UP,
    NONE
}

enum class DarkThemeConfig {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK
}

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val DARK_THEME_CONFIG = androidx.datastore.preferences.core.stringPreferencesKey("dark_theme_config")
        val IS_AMOLED_MODE = booleanPreferencesKey("is_amoled_mode")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
        val NOTIFICATION_STYLE = androidx.datastore.preferences.core.stringPreferencesKey("notification_style")
        val ALARM_SOUND_URI = androidx.datastore.preferences.core.stringPreferencesKey("alarm_sound_uri")
        val COMPLETED_RETENTION_DAYS = androidx.datastore.preferences.core.intPreferencesKey("completed_retention_days")
        val ADD_BUTTON_ON_LEFT = booleanPreferencesKey("add_button_on_left")
        val AUTOMATIC_UPDATE_CHECKS = booleanPreferencesKey("automatic_update_checks")
        val LAST_UPDATE_CHECK = androidx.datastore.preferences.core.longPreferencesKey("last_update_check")
        val LATEST_RELEASE_TAG = androidx.datastore.preferences.core.stringPreferencesKey("latest_release_tag")
        val LATEST_RELEASE_URL = androidx.datastore.preferences.core.stringPreferencesKey("latest_release_url")
        val NOTIFICATION_PERMISSION_ASKED = booleanPreferencesKey("notification_permission_asked")
    }

    val themeSettings: Flow<AppThemeSettings> = context.dataStore.data.map { preferences ->
        val configStr = preferences[PreferencesKeys.DARK_THEME_CONFIG] ?: DarkThemeConfig.FOLLOW_SYSTEM.name
        val config = try {
            DarkThemeConfig.valueOf(configStr)
        } catch (e: Exception) {
            DarkThemeConfig.FOLLOW_SYSTEM
        }
        val isAmoled = preferences[PreferencesKeys.IS_AMOLED_MODE] ?: true
        val dynamicColors = preferences[PreferencesKeys.USE_DYNAMIC_COLORS] ?: true
        val notificationStyleStr = preferences[PreferencesKeys.NOTIFICATION_STYLE] ?: NotificationStyle.HEADS_UP.name
        val notificationStyle = try {
            NotificationStyle.valueOf(notificationStyleStr)
        } catch (e: Exception) {
            NotificationStyle.HEADS_UP
        }

        AppThemeSettings(
            darkThemeConfig = config,
            isAmoledMode = isAmoled,
            useDynamicColors = dynamicColors,
            notificationStyle = notificationStyle,
            alarmSoundUri = preferences[PreferencesKeys.ALARM_SOUND_URI],
            completedReminderRetentionDays = preferences[PreferencesKeys.COMPLETED_RETENTION_DAYS] ?: 0,
            addButtonOnLeft = preferences[PreferencesKeys.ADD_BUTTON_ON_LEFT] ?: false,
            automaticUpdateChecks = preferences[PreferencesKeys.AUTOMATIC_UPDATE_CHECKS] ?: false,
            lastUpdateCheckMillis = preferences[PreferencesKeys.LAST_UPDATE_CHECK] ?: 0L,
            latestReleaseTag = preferences[PreferencesKeys.LATEST_RELEASE_TAG],
            latestReleaseUrl = preferences[PreferencesKeys.LATEST_RELEASE_URL],
            notificationPermissionAsked = preferences[PreferencesKeys.NOTIFICATION_PERMISSION_ASKED] ?: false
        )
    }

    suspend fun setDarkThemeConfig(config: DarkThemeConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_THEME_CONFIG] = config.name
            if (config == DarkThemeConfig.LIGHT) preferences[PreferencesKeys.IS_AMOLED_MODE] = false
        }
    }

    suspend fun setAmoledMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_AMOLED_MODE] = enabled
            if (enabled) preferences[PreferencesKeys.DARK_THEME_CONFIG] = DarkThemeConfig.DARK.name
        }
    }

    suspend fun setUseDynamicColors(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_DYNAMIC_COLORS] = enabled
        }
    }

    suspend fun setAlarmSoundUri(uri: String?) { context.dataStore.edit { if (uri == null) it.remove(PreferencesKeys.ALARM_SOUND_URI) else it[PreferencesKeys.ALARM_SOUND_URI] = uri } }
    suspend fun setCompletedRetentionDays(days: Int) { context.dataStore.edit { it[PreferencesKeys.COMPLETED_RETENTION_DAYS] = days.coerceIn(0, 3650) } }
    suspend fun setAddButtonOnLeft(enabled: Boolean) { context.dataStore.edit { it[PreferencesKeys.ADD_BUTTON_ON_LEFT] = enabled } }
    suspend fun setAutomaticUpdateChecks(enabled: Boolean) { context.dataStore.edit { it[PreferencesKeys.AUTOMATIC_UPDATE_CHECKS] = enabled } }
    suspend fun markNotificationPermissionAsked() { context.dataStore.edit { it[PreferencesKeys.NOTIFICATION_PERMISSION_ASKED] = true } }
    suspend fun saveUpdateCheck(timestamp: Long, tag: String?, url: String?) { context.dataStore.edit { p -> p[PreferencesKeys.LAST_UPDATE_CHECK] = timestamp; if (tag != null) p[PreferencesKeys.LATEST_RELEASE_TAG] = tag; if (url != null) p[PreferencesKeys.LATEST_RELEASE_URL] = url } }


    suspend fun restoreReminderPreferences(settings: AppThemeSettings) {
        context.dataStore.edit { p ->
            p[PreferencesKeys.DARK_THEME_CONFIG] = settings.darkThemeConfig.name
            p[PreferencesKeys.IS_AMOLED_MODE] = settings.isAmoledMode && settings.darkThemeConfig != DarkThemeConfig.LIGHT
            p[PreferencesKeys.USE_DYNAMIC_COLORS] = settings.useDynamicColors
            p[PreferencesKeys.NOTIFICATION_STYLE] = settings.notificationStyle.name
            if (settings.alarmSoundUri == null) p.remove(PreferencesKeys.ALARM_SOUND_URI) else p[PreferencesKeys.ALARM_SOUND_URI] = settings.alarmSoundUri
            p[PreferencesKeys.COMPLETED_RETENTION_DAYS] = settings.completedReminderRetentionDays.coerceIn(0, 3650)
            p[PreferencesKeys.ADD_BUTTON_ON_LEFT] = settings.addButtonOnLeft
        }
    }

    suspend fun setNotificationStyle(style: NotificationStyle) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_STYLE] = style.name
        }
    }
}
