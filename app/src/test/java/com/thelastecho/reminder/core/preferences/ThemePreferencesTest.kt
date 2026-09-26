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


class AccentSeedResolutionTest {
    @org.junit.Test
    fun `custom seed wins only when custom accent is active`() {
        val custom = 0xFF13A9C2.toInt()
        org.junit.Assert.assertEquals(custom, resolveAccentSeedArgb(AccentColor.RED, custom, true))
        org.junit.Assert.assertEquals(0xFFBA1A1A.toInt(), resolveAccentSeedArgb(AccentColor.RED, custom, false))
    }

    @org.junit.Test
    fun `preset is fallback when custom seed is missing`() {
        org.junit.Assert.assertEquals(0xFF6750A4.toInt(), resolveAccentSeedArgb(AccentColor.VIOLET, null, true))
    }
}
