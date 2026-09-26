package com.thelastecho.reminder.core.preferences

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

enum class AccentColor { BLUE, VIOLET, GREEN, TEAL, ORANGE, RED, PINK }

/** Retained only to decode existing backup files. */
enum class DarkThemeConfig { FOLLOW_SYSTEM, LIGHT, DARK }

fun migrateThemeMode(
    storedThemeMode: String?,
    legacyDarkThemeConfig: String?,
    legacyIsAmoledMode: Boolean
): ThemeMode {
    storedThemeMode?.let { return runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM) }
    val legacy = runCatching {
        DarkThemeConfig.valueOf(legacyDarkThemeConfig ?: DarkThemeConfig.FOLLOW_SYSTEM.name)
    }.getOrDefault(DarkThemeConfig.FOLLOW_SYSTEM)
    return when {
        legacy == DarkThemeConfig.FOLLOW_SYSTEM -> ThemeMode.SYSTEM
        legacy == DarkThemeConfig.LIGHT -> ThemeMode.LIGHT
        legacyIsAmoledMode -> ThemeMode.AMOLED
        else -> ThemeMode.DARK
    }
}
