package com.thelastecho.reminder.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.thelastecho.reminder.core.designsystem.ReminderTheme
import com.thelastecho.reminder.core.preferences.DarkThemeConfig
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.presentation.navigation.NavDestination
import com.thelastecho.reminder.presentation.navigation.NavGraph

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialReminderId = intent.getLongExtra("reminder_id", -1L)

        setContent {
            val userPreferences = remember { UserPreferencesRepository(applicationContext) }
            val themeSettings by userPreferences.themeSettings.collectAsState(
                initial = com.thelastecho.reminder.core.preferences.AppThemeSettings()
            )

            // Request POST_NOTIFICATIONS runtime permission on Android 13+ (API 33+)
            RequestNotificationPermissionIfNeeded()

            val isDark = when (themeSettings.darkThemeConfig) {
                DarkThemeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
                DarkThemeConfig.LIGHT -> false
                DarkThemeConfig.DARK -> true
            }

            ReminderTheme(
                darkTheme = isDark,
                isAmoledMode = themeSettings.isAmoledMode,
                dynamicColor = themeSettings.useDynamicColors
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
private fun ComponentActivity.RequestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        )
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* Granted or denied handled gracefully */ }

        LaunchedEffect(Unit) {
            if (permissionState != PackageManager.PERMISSION_GRANTED) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
