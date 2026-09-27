package com.thelastecho.reminder.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.map
import com.thelastecho.reminder.core.designsystem.ReminderTheme
import com.thelastecho.reminder.core.preferences.AppThemeSettings
import com.thelastecho.reminder.core.preferences.ThemeMode
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.presentation.navigation.NavDestination
import com.thelastecho.reminder.presentation.navigation.NavGraph

class MainActivity : ComponentActivity() {

    @Volatile private var themeSettingsLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !themeSettingsLoaded }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialReminderId = intent.getLongExtra("reminder_id", -1L)

        setContent {
            val userPreferences = remember { UserPreferencesRepository(applicationContext) }
            val loadedThemeSettings by remember(userPreferences) {
                userPreferences.themeSettings.map { it as AppThemeSettings? }
            }.collectAsState(initial = null)
            val themeSettings = loadedThemeSettings ?: AppThemeSettings(
                themeMode = ThemeMode.SYSTEM
            )
            androidx.compose.runtime.SideEffect { themeSettingsLoaded = loadedThemeSettings != null }

            // Request POST_NOTIFICATIONS runtime permission on Android 13+ (API 33+)
            RequestNotificationPermissionIfNeeded(userPreferences, loadedThemeSettings?.notificationPermissionAsked)

            val isDark = when (themeSettings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK, ThemeMode.AMOLED -> true
            }

            ReminderTheme(
                darkTheme = isDark,
                isAmoledMode = themeSettings.themeMode == ThemeMode.AMOLED,
                dynamicColor = themeSettings.useDynamicColors,
                accentColor = themeSettings.accentColor,
                customAccentColor = themeSettings.customAccentColor,
                useCustomAccent = themeSettings.useCustomAccent
            ) {
                val navController = rememberNavController()

                LaunchedEffect(initialReminderId) {
                    if (initialReminderId > 0) {
                        navController.navigate(NavDestination.Editor.createRoute(initialReminderId))
                    }
                }

                NavGraph(navController = navController)
            }
        }
    }
}

@Composable
private fun ComponentActivity.RequestNotificationPermissionIfNeeded(preferences: UserPreferencesRepository, alreadyAsked: Boolean?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        )
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* Granted or denied handled gracefully */ }

        LaunchedEffect(permissionState, alreadyAsked) {
            if (permissionState != PackageManager.PERMISSION_GRANTED && alreadyAsked == false) {
                preferences.markNotificationPermissionAsked()
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
