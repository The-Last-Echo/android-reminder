package com.thelastecho.reminder.presentation.settings

import com.thelastecho.reminder.core.preferences.DarkThemeConfig

data class SettingsUiState(
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    val isAmoledMode: Boolean = true,
    val useDynamicColors: Boolean = true,
    val appVersion: String = "1.0.0"
)

sealed interface SettingsIntent {
    data class SetDarkThemeConfig(val config: DarkThemeConfig) : SettingsIntent
    data class SetAmoledMode(val enabled: Boolean) : SettingsIntent
    data class SetDynamicColors(val enabled: Boolean) : SettingsIntent
}
