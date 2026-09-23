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
    val useDynamicColors: Boolean = true
)

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

        AppThemeSettings(
            darkThemeConfig = config,
            isAmoledMode = isAmoled,
            useDynamicColors = dynamicColors
        )
    }

    suspend fun setDarkThemeConfig(config: DarkThemeConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_THEME_CONFIG] = config.name
        }
    }

    suspend fun setAmoledMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_AMOLED_MODE] = enabled
        }
    }

    suspend fun setUseDynamicColors(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_DYNAMIC_COLORS] = enabled
        }
    }
}
