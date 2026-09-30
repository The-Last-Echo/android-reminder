package com.thelastecho.reminder.presentation.settings

import com.thelastecho.reminder.core.preferences.AccentColor
import com.thelastecho.reminder.core.preferences.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.VIOLET,
    val useDynamicColors: Boolean = true,
    val customAccentColor: Int? = null,
    val useCustomAccent: Boolean = false,
    val notificationStyle: com.thelastecho.reminder.core.preferences.NotificationStyle = com.thelastecho.reminder.core.preferences.NotificationStyle.LIGHT,
    val alarmSoundUri: String? = null,
    val completedReminderRetentionDays: Int = 0,
    val addButtonOnLeft: Boolean = false,
    val automaticUpdateChecks: Boolean = false,
    val latestReleaseTag: String? = null,
    val latestReleaseUrl: String? = null,
    val lastUpdateCheckMillis: Long = 0L
)

sealed interface SettingsIntent {
    data class SetThemeMode(val mode: ThemeMode) : SettingsIntent
    data class SetAccentColor(val color: AccentColor) : SettingsIntent
    data class SetCustomAccentColor(val argb: Int) : SettingsIntent
    data class SetDynamicColors(val enabled: Boolean) : SettingsIntent
    data class SetNotificationStyle(val style: com.thelastecho.reminder.core.preferences.NotificationStyle) : SettingsIntent
    data class SetAlarmSound(val uri: String?) : SettingsIntent
    data class SetCompletedRetention(val days: Int) : SettingsIntent
    data class SetAddButtonOnLeft(val enabled: Boolean) : SettingsIntent
    data class SetAutomaticUpdateChecks(val enabled: Boolean) : SettingsIntent
}
