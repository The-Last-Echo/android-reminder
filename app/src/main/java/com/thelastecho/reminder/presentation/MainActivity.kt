package com.thelastecho.reminder.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.Flow
import com.thelastecho.reminder.core.designsystem.ReminderTheme
import com.thelastecho.reminder.core.preferences.AppThemeSettings
import com.thelastecho.reminder.core.preferences.ThemeMode
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.core.preferences.shouldOfferFullScreenAccessPrompt
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
        val openNewReminder = intent.getBooleanExtra("open_new_reminder", false)

        setContent {
            val userPreferences = remember { UserPreferencesRepository(applicationContext) }
            val themeSettingsFlow: Flow<AppThemeSettings?> = remember(userPreferences) {
                userPreferences.themeSettings
            }
            val loadedThemeSettings by themeSettingsFlow.collectAsState(initial = null)
            var notificationPermissionRequestVersion by remember { mutableIntStateOf(0) }
            val themeSettings = loadedThemeSettings ?: AppThemeSettings(
                themeMode = ThemeMode.SYSTEM
            )
            androidx.compose.runtime.SideEffect { themeSettingsLoaded = loadedThemeSettings != null }

            // Request POST_NOTIFICATIONS runtime permission on Android 13+ (API 33+)
            RequestNotificationPermissionIfNeeded(userPreferences, loadedThemeSettings?.notificationPermissionAsked) {
                notificationPermissionRequestVersion++
            }
            FullScreenAccessPrompt(
                userPreferences,
                loadedThemeSettings?.fullScreenAccessPromptAsked,
                notificationPermissionRequestVersion
            )

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

                LaunchedEffect(initialReminderId, openNewReminder) {
                    if (openNewReminder) {
                        navController.navigate(NavDestination.Editor.createRoute())
                    } else if (initialReminderId > 0) {
                        navController.navigate(NavDestination.Editor.createRoute(initialReminderId))
                    }
                }

                NavGraph(navController = navController)
            }
        }
    }
}

@Composable
private fun ComponentActivity.FullScreenAccessPrompt(
    preferences: UserPreferencesRepository,
    alreadyAsked: Boolean?,
    permissionRequestVersion: Int
) {
    val context = this
    val notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val notificationsEnabled = notificationsGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    var showPrompt by remember { androidx.compose.runtime.mutableStateOf(false) }

    val overlaySettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val fullScreenSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val manager = getSystemService(android.app.NotificationManager::class.java)
        val hasFullScreenAccess = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || manager.canUseFullScreenIntent()
        if (hasFullScreenAccess && !AndroidSettings.canDrawOverlays(this)) {
            runCatching {
                overlaySettings.launch(Intent(AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        }
    }

    LaunchedEffect(notificationsEnabled, alreadyAsked, permissionRequestVersion) {
        if (shouldOfferFullScreenAccessPrompt(notificationsEnabled, alreadyAsked ?: true)) {
            preferences.markFullScreenAccessPromptAsked()
            showPrompt = true
        }
    }

    if (showPrompt) {
        AlertDialog(
            onDismissRequest = { showPrompt = false },
            title = { Text(stringResource(com.thelastecho.reminder.R.string.full_screen_access_prompt_title)) },
            text = { Text(stringResource(com.thelastecho.reminder.R.string.full_screen_access_prompt_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showPrompt = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        runCatching { fullScreenSettings.launch(fullScreenIntentSettingsIntent()) }
                    } else {
                        runCatching {
                            overlaySettings.launch(Intent(AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                        }
                    }
                }) { Text(stringResource(com.thelastecho.reminder.R.string.enable_full_screen_access)) }
            },
            dismissButton = {
                TextButton(onClick = { showPrompt = false }) {
                    Text(stringResource(com.thelastecho.reminder.R.string.not_now))
                }
            }
        )
    }
}

private fun ComponentActivity.fullScreenIntentSettingsIntent(): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        Intent(AndroidSettings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).setData(Uri.parse("package:$packageName"))
    } else {
        Intent(AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
    }

@Composable
private fun ComponentActivity.RequestNotificationPermissionIfNeeded(
    preferences: UserPreferencesRepository,
    alreadyAsked: Boolean?,
    onPermissionRequestCompleted: () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        )
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { onPermissionRequestCompleted() }

        LaunchedEffect(permissionState, alreadyAsked) {
            if (permissionState != PackageManager.PERMISSION_GRANTED && alreadyAsked == false) {
                preferences.markNotificationPermissionAsked()
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
