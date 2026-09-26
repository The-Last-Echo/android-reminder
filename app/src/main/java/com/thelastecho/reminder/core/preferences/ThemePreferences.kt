package com.thelastecho.reminder.core.preferences

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

enum class ThemeStyle { MATERIAL, GLASS }

fun parseThemeStyle(storedStyle: String?): ThemeStyle =
    runCatching { ThemeStyle.valueOf(storedStyle ?: ThemeStyle.MATERIAL.name) }
        .getOrDefault(ThemeStyle.MATERIAL)

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


/** Returns the active manual seed; dynamic system colors are resolved separately by the theme. */
fun resolveAccentSeedArgb(accentColor: AccentColor, customAccentColor: Int?, useCustomAccent: Boolean): Int =
    if (useCustomAccent && customAccentColor != null) customAccentColor else when (accentColor) {
        AccentColor.BLUE -> 0xFF0061A4.toInt()
        AccentColor.VIOLET -> 0xFF6750A4.toInt()
        AccentColor.GREEN -> 0xFF386A20.toInt()
        AccentColor.TEAL -> 0xFF006A60.toInt()
        AccentColor.ORANGE -> 0xFF8A5000.toInt()
        AccentColor.RED -> 0xFFBA1A1A.toInt()
        AccentColor.PINK -> 0xFF9A406D.toInt()
    }
