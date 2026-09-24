package com.thelastecho.reminder.presentation.settings

import com.thelastecho.reminder.core.preferences.DarkThemeConfig

data class SettingsUiState(
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    val isAmoledMode: Boolean = true,
    val useDynamicColors: Boolean = true,
    val notificationStyle: com.thelastecho.reminder.core.preferences.NotificationStyle = com.thelastecho.reminder.core.preferences.NotificationStyle.HEADS_UP,
    val alarmSoundUri: String? = null,
    val completedReminderRetentionDays: Int = 0,
    val addButtonOnLeft: Boolean = false,
    val automaticUpdateChecks: Boolean = false,
    val latestReleaseTag: String? = null,
    val latestReleaseUrl: String? = null,
    val lastUpdateCheckMillis: Long = 0L
)

sealed interface SettingsIntent {
    data class SetDarkThemeConfig(val config: DarkThemeConfig) : SettingsIntent
    data class SetAmoledMode(val enabled: Boolean) : SettingsIntent
    data class SetDynamicColors(val enabled: Boolean) : SettingsIntent
    data class SetNotificationStyle(val style: com.thelastecho.reminder.core.preferences.NotificationStyle) : SettingsIntent
    data class SetAlarmSound(val uri: String?) : SettingsIntent
    data class SetCompletedRetention(val days: Int) : SettingsIntent
    data class SetAddButtonOnLeft(val enabled: Boolean) : SettingsIntent
    data class SetAutomaticUpdateChecks(val enabled: Boolean) : SettingsIntent
    data object NavigateToCategories : SettingsIntent
    data object NavigateToTrash : SettingsIntent
    data object NavigateToBackup : SettingsIntent
    data object NavigateToPrivacy : SettingsIntent
}

sealed interface SettingsEffect {
    data object NavigateToCategories : SettingsEffect
    data object NavigateToTrash : SettingsEffect
    data object NavigateToBackup : SettingsEffect
    data object NavigateToPrivacy : SettingsEffect
}
