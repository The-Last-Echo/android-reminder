package com.thelastecho.reminder.core.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferencesTest {
    @Test
    fun explicitAmoledIsMigratedFromDarkMode() {
        assertEquals(ThemeMode.AMOLED, migrateThemeMode(null, "DARK", true))
    }

    @Test
    fun systemModeDoesNotSilentlyMigrateToAmoled() {
        assertEquals(ThemeMode.SYSTEM, migrateThemeMode(null, "FOLLOW_SYSTEM", true))
    }

    @Test
    fun explicitDarkWithoutAmoledRemainsStandardDark() {
        assertEquals(ThemeMode.DARK, migrateThemeMode(null, "DARK", false))
    }

    @Test
    fun explicitLightRemainsLight() {
        assertEquals(ThemeMode.LIGHT, migrateThemeMode(null, "LIGHT", true))
    }

    @Test
    fun storedNewModeWinsOverLegacyValues() {
        assertEquals(ThemeMode.AMOLED, migrateThemeMode("AMOLED", "LIGHT", false))
    }
}
