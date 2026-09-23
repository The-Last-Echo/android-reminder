package com.thelastecho.reminder.presentation.settings

import com.thelastecho.reminder.core.preferences.DarkThemeConfig

data class SettingsUiState(
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    val isAmoledMode: Boolean = true,
    val useDynamicColors: Boolean = true,
    val notificationStyle: com.thelastecho.reminder.core.preferences.NotificationStyle = com.thelastecho.reminder.core.preferences.NotificationStyle.HEADS_UP,
    val appVersion: String = "1.1.0"
)

sealed interface SettingsIntent {
    data class SetDarkThemeConfig(val config: DarkThemeConfig) : SettingsIntent
    data class SetAmoledMode(val enabled: Boolean) : SettingsIntent
    data class SetDynamicColors(val enabled: Boolean) : SettingsIntent
    data class SetNotificationStyle(val style: com.thelastecho.reminder.core.preferences.NotificationStyle) : SettingsIntent
    data object NavigateToCategories : SettingsIntent
    data object NavigateToTrash : SettingsIntent
}

sealed interface SettingsEffect {
    data object NavigateToCategories : SettingsEffect
    data object NavigateToTrash : SettingsEffect
}
