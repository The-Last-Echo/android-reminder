package com.thelastecho.reminder.presentation.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import com.thelastecho.reminder.core.alarm.AndroidAlarmScheduler
import com.thelastecho.reminder.core.distribution.UpdateCheckController
import com.thelastecho.reminder.R
import com.thelastecho.reminder.core.designsystem.ReminderShapes
import com.thelastecho.reminder.core.preferences.AccentColor
import com.thelastecho.reminder.core.preferences.ThemeMode
import com.thelastecho.reminder.core.preferences.NotificationStyle
import com.thelastecho.reminder.presentation.components.SectionHeading
import com.thelastecho.reminder.presentation.components.CustomColorPickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsGroupScaffold(titleRes: Int, onNavigateBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) { content() }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        shape = ReminderShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().border(
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            ReminderShapes.Card
        )
    ) { Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) { content() } }
}

@Composable
private fun SettingRow(
    title: String,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GeneralSettingsScreen(viewModel: SettingsViewModel, onNavigateBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showPositionDialog by remember { mutableStateOf(false) }
    SettingsGroupScaffold(R.string.settings_group_general_title, onNavigateBack) {
        SettingsCard {
            SettingRow(
                title = stringResource(R.string.app_language),
                description = stringResource(R.string.app_language_details),
                onClick = {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Intent(AndroidSettings.ACTION_APP_LOCALE_SETTINGS).setData(Uri.parse("package:${context.packageName}"))
                    } else Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    runCatching { context.startActivity(intent) }
                }
            )
            SettingRow(
                title = stringResource(R.string.add_button_position),
                description = stringResource(if (state.addButtonOnLeft) R.string.position_left else R.string.position_right),
                onClick = { showPositionDialog = true }
            )
        }
    }
    if (showPositionDialog) {
        AlertDialog(
            onDismissRequest = { showPositionDialog = false },
            title = { Text(stringResource(R.string.add_button_position)) },
            text = {
                Column {
                    listOf(true, false).forEach { onLeft ->
                        Row(Modifier.fillMaxWidth().clickable { viewModel.onIntent(SettingsIntent.SetAddButtonOnLeft(onLeft)); showPositionDialog = false }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = state.addButtonOnLeft == onLeft, onClick = { viewModel.onIntent(SettingsIntent.SetAddButtonOnLeft(onLeft)); showPositionDialog = false })
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(if (onLeft) R.string.position_left else R.string.position_right))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPositionDialog = false }) { Text(stringResource(R.string.ok)) } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceSettingsScreen(viewModel: SettingsViewModel, onNavigateBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCustomColorDialog by remember { mutableStateOf(false) }
    val modeLabel = when (state.themeMode) {
        ThemeMode.SYSTEM -> R.string.theme_mode_system
        ThemeMode.LIGHT -> R.string.theme_mode_light
        ThemeMode.DARK -> R.string.theme_mode_dark
        ThemeMode.AMOLED -> R.string.theme_mode_amoled
    }
    SettingsGroupScaffold(R.string.settings_group_appearance_title, onNavigateBack) {
        SettingsCard {
            SettingRow(
                title = stringResource(R.string.theme_mode_title),
                description = stringResource(modeLabel),
                onClick = { showThemeDialog = true },
                leading = { Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.accent_color_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(if (state.useDynamicColors) R.string.accent_color_dynamic_disabled else R.string.accent_color_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AccentColor.entries.forEach { accent ->
                        val selected = !state.useCustomAccent && state.accentColor == accent
                        val swatch = when (accent) {
                            AccentColor.BLUE -> Color(0xFF0061A4)
                            AccentColor.VIOLET -> Color(0xFF6750A4)
                            AccentColor.GREEN -> Color(0xFF386A20)
                            AccentColor.TEAL -> Color(0xFF006A60)
                            AccentColor.ORANGE -> Color(0xFF8A5000)
                            AccentColor.RED -> Color(0xFFBA1A1A)
                            AccentColor.PINK -> Color(0xFF9A406D)
                        }
                        val accentName = when (accent) {
                            AccentColor.BLUE -> R.string.accent_blue
                            AccentColor.VIOLET -> R.string.accent_violet
                            AccentColor.GREEN -> R.string.accent_green
                            AccentColor.TEAL -> R.string.accent_teal
                            AccentColor.ORANGE -> R.string.accent_orange
                            AccentColor.RED -> R.string.accent_red
                            AccentColor.PINK -> R.string.accent_pink
                        }
                        val accentLabel = stringResource(accentName)
                        Box(
                            modifier = Modifier.size(48.dp).selectable(
                                selected = selected,
                                enabled = !state.useDynamicColors,
                                role = Role.RadioButton,
                                onClick = { viewModel.onIntent(SettingsIntent.SetAccentColor(accent)) }
                            ).semantics { contentDescription = accentLabel },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = swatch.copy(alpha = if (state.useDynamicColors) 0.35f else 1f),
                                border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.useDynamicColors) 0.5f else 1f)) else null
                            ) {}
                        }
                    }
                    val customPreview = state.customAccentColor?.let { Color(it) } ?: Color(0xFF6750A4)
                    val customLabel = stringResource(R.string.accent_custom)
                    Box(
                        modifier = Modifier.size(48.dp).selectable(
                            selected = state.useCustomAccent,
                            enabled = !state.useDynamicColors,
                            role = Role.RadioButton,
                            onClick = { showCustomColorDialog = true }
                        ).semantics { contentDescription = customLabel },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = customPreview.copy(alpha = if (state.useDynamicColors) 0.35f else 1f),
                            border = if (state.useCustomAccent) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.useDynamicColors) 0.5f else 1f)) else null
                        ) {
                            Icon(Icons.Outlined.Palette, contentDescription = null, tint = Color.White.copy(alpha = if (state.useDynamicColors) 0.45f else 1f), modifier = Modifier.padding(7.dp))
                        }
                    }
                }
            }
            SettingRow(
                title = stringResource(R.string.dynamic_colors),
                description = stringResource(R.string.dynamic_colors_description),
                trailing = { Switch(checked = state.useDynamicColors, onCheckedChange = { viewModel.onIntent(SettingsIntent.SetDynamicColors(it)) }) },
                leading = { Icon(Icons.Outlined.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )
        }
    }
    if (showCustomColorDialog) {
        CustomColorPickerDialog(
            initialColor = state.customAccentColor ?: 0xFF6750A4.toInt(),
            title = stringResource(R.string.custom_color_title),
            description = stringResource(R.string.custom_color_description),
            onDismiss = { showCustomColorDialog = false },
            onApply = { argb ->
                viewModel.onIntent(SettingsIntent.SetCustomAccentColor(argb))
                showCustomColorDialog = false
            }
        )
    }
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.theme_mode_title)) },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        val labelRes = when (mode) {
                            ThemeMode.SYSTEM -> R.string.theme_mode_system
                            ThemeMode.LIGHT -> R.string.theme_mode_light
                            ThemeMode.DARK -> R.string.theme_mode_dark
                            ThemeMode.AMOLED -> R.string.theme_mode_amoled
                        }
                        Row(Modifier.fillMaxWidth().clickable { viewModel.onIntent(SettingsIntent.SetThemeMode(mode)); showThemeDialog = false }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = state.themeMode == mode, onClick = { viewModel.onIntent(SettingsIntent.SetThemeMode(mode)); showThemeDialog = false })
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(labelRes))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
fun RemindersSettingsScreen(viewModel: SettingsViewModel, onNavigateBack: () -> Unit, onNavigateToCategories: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showRetentionDialog by remember { mutableStateOf(false) }
    var customRetention by remember { mutableStateOf("") }
    val retentionText = if (state.completedReminderRetentionDays == 0) stringResource(R.string.retention_never) else context.resources.getQuantityString(R.plurals.retention_days, state.completedReminderRetentionDays, state.completedReminderRetentionDays)
    SettingsGroupScaffold(R.string.settings_group_reminders_title, onNavigateBack) {
        SettingsCard {
            SettingRow(
                title = stringResource(R.string.manage_categories),
                description = stringResource(R.string.create_manage_categories),
                onClick = onNavigateToCategories,
                leading = { Icon(Icons.Outlined.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )
            SettingRow(
                title = stringResource(R.string.completed_retention),
                description = retentionText,
                onClick = { showRetentionDialog = true },
                leading = { Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )
        }
    }
    if (showRetentionDialog) {
        AlertDialog(
            onDismissRequest = { showRetentionDialog = false },
            title = { Text(stringResource(R.string.completed_retention)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0, 1, 7, 30, 90).forEach { days ->
                        val label = when (days) {
                            0 -> stringResource(R.string.retention_never)
                            1 -> stringResource(R.string.retention_1_day)
                            7 -> stringResource(R.string.retention_7_days)
                            30 -> stringResource(R.string.retention_30_days)
                            else -> stringResource(R.string.retention_90_days)
                        }
                        Row(Modifier.fillMaxWidth().clickable { viewModel.onIntent(SettingsIntent.SetCompletedRetention(days)); showRetentionDialog = false }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = state.completedReminderRetentionDays == days, onClick = { viewModel.onIntent(SettingsIntent.SetCompletedRetention(days)); showRetentionDialog = false })
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    OutlinedTextField(value = customRetention, onValueChange = { customRetention = it.filter(Char::isDigit).take(4) }, label = { Text(stringResource(R.string.custom_duration)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
            },
            confirmButton = { TextButton(onClick = { customRetention.toIntOrNull()?.takeIf { it in 1..3650 }?.let { viewModel.onIntent(SettingsIntent.SetCompletedRetention(it)) }; showRetentionDialog = false }) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = { showRetentionDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
fun NotificationsAlarmsSettingsScreen(viewModel: SettingsViewModel, onNavigateBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showStyleDialog by remember { mutableStateOf(false) }
    var hasNotificationAccess by remember(context) { mutableStateOf(readNotificationAccess(context)) }
    var hasExactAlarmAccess by remember(context) { mutableStateOf(AndroidAlarmScheduler(context).canScheduleExactAlarms()) }
    var hasFullScreenAccess by remember(context) { mutableStateOf(readFullScreenAccess(context)) }
    val systemSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasNotificationAccess = readNotificationAccess(context)
        hasExactAlarmAccess = AndroidAlarmScheduler(context).canScheduleExactAlarms()
        hasFullScreenAccess = readFullScreenAccess(context)
    }
    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= 33) result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) else @Suppress("DEPRECATION") result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            viewModel.onIntent(SettingsIntent.SetAlarmSound(uri?.toString()))
        }
    }
    SettingsGroupScaffold(R.string.settings_group_notifications_title, onNavigateBack) {
        SettingsCard {
            Text(stringResource(R.string.notification_style), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp))
            SettingRow(
                title = stringResource(R.string.notification_style),
                description = stringResource(when (state.notificationStyle) {
                    NotificationStyle.SIMPLE -> R.string.simple_style_name
                    NotificationStyle.FULL_SCREEN -> R.string.full_screen_style_name
                    NotificationStyle.HEADS_UP -> R.string.heads_up_style_name
                    NotificationStyle.NONE -> R.string.notification_none_name
                }),
                onClick = { showStyleDialog = true },
                leading = { Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )
            SettingRow(
                title = stringResource(R.string.notifications_permission_title),
                description = stringResource(if (hasNotificationAccess) R.string.notifications_permission_granted else R.string.notifications_permission_blocked),
                onClick = { runCatching { systemSettingsLauncher.launch(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)) } }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Text(stringResource(R.string.choose_alarm_sound), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp))
            SettingRow(
                title = stringResource(R.string.choose_alarm_sound),
                description = stringResource(if (state.alarmSoundUri == null) R.string.system_default else R.string.selected),
                onClick = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.choose_alarm_sound))
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, state.alarmSoundUri?.let(Uri::parse))
                    }
                    ringtonePicker.launch(intent)
                },
                leading = { Icon(Icons.Outlined.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingRow(
                    title = stringResource(R.string.exact_alarm_access_label),
                    description = stringResource(if (hasExactAlarmAccess) R.string.exact_alarm_access_granted else R.string.exact_alarm_access_not_granted),
                    onClick = { runCatching { systemSettingsLauncher.launch(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) } }
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                SettingRow(
                    title = stringResource(R.string.full_screen_access_label),
                    description = stringResource(if (hasFullScreenAccess) R.string.full_screen_access_granted else R.string.full_screen_access_not_granted),
                    onClick = { runCatching { systemSettingsLauncher.launch(fullScreenIntentSettingsIntent(context)) } }
                )
                Text(stringResource(R.string.full_screen_fallback_details), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 56.dp, end = 8.dp, bottom = 12.dp))
            }
        }

        DeveloperNotificationDiagnostics()
    }
    if (showStyleDialog) {
        AlertDialog(
            onDismissRequest = { showStyleDialog = false },
            title = { Text(stringResource(R.string.choose_notification_style)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    NotificationStyle.entries.forEach { style ->
                        val labelRes = when (style) {
                            NotificationStyle.SIMPLE -> R.string.simple_notification
                            NotificationStyle.FULL_SCREEN -> R.string.full_screen_notification
                            NotificationStyle.HEADS_UP -> R.string.heads_up_notification
                            NotificationStyle.NONE -> R.string.notification_none_name
                        }
                        Row(Modifier.fillMaxWidth().clickable { viewModel.onIntent(SettingsIntent.SetNotificationStyle(style)); showStyleDialog = false }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = state.notificationStyle == style, onClick = { viewModel.onIntent(SettingsIntent.SetNotificationStyle(style)); showStyleDialog = false })
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(labelRes))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showStyleDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

private fun readNotificationAccess(context: android.content.Context): Boolean =
    (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun readFullScreenAccess(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || context.getSystemService(android.app.NotificationManager::class.java).canUseFullScreenIntent()

@Composable
fun DataSettingsScreen(onNavigateBack: () -> Unit, onNavigateToBackup: () -> Unit, onNavigateToTrash: () -> Unit) {
    SettingsGroupScaffold(R.string.settings_group_data_title, onNavigateBack) {
        SettingsCard {
            SettingRow(
                title = stringResource(R.string.backup_restore),
                description = stringResource(R.string.backup_description),
                onClick = onNavigateToBackup,
                leading = { Icon(Icons.Outlined.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )
            SettingRow(
                title = stringResource(R.string.trash),
                description = stringResource(R.string.view_restore_deleted),
                onClick = onNavigateToTrash,
                leading = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )
        }
    }
}

@Composable
fun AboutSettingsScreen(viewModel: SettingsViewModel, updateChecks: UpdateCheckController, onNavigateBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val packageVersion = remember(context) { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
    SettingsGroupScaffold(R.string.settings_group_about_title, onNavigateBack) {
        SettingsCard {
            SettingRow(
                title = stringResource(R.string.app_version, packageVersion),
                description = stringResource(R.string.about_details),
                onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/The-Last-Echo/android-reminder"))) } }
            )
        }
        if (updateChecks.isAvailable) {
            SettingsCard {
                SettingRow(
                    title = stringResource(R.string.automatic_update_checks),
                    description = stringResource(R.string.automatic_update_checks_details),
                    trailing = { Switch(checked = state.automaticUpdateChecks, onCheckedChange = { viewModel.onIntent(SettingsIntent.SetAutomaticUpdateChecks(it)) }) }
                )
                TextButton(onClick = updateChecks::enqueueManualCheck) {
                    Text(stringResource(R.string.check_updates_now))
                }
                state.latestReleaseTag?.let { tag ->
                    val newer = isVersionNewer(tag, packageVersion)
                    Text(stringResource(if (newer) R.string.update_available else R.string.app_up_to_date), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.latest_release, tag), style = MaterialTheme.typography.bodySmall)
                    state.latestReleaseUrl?.let { url -> TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }) { Text(stringResource(R.string.open_release)) } }
                }
            }
        }
    }
}

private fun isVersionNewer(remoteTag: String, installedVersion: String): Boolean {
    fun parts(value: String) = value.trim().removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
    val remote = parts(remoteTag)
    val local = parts(installedVersion)
    for (index in 0 until maxOf(remote.size, local.size)) {
        val comparison = (remote.getOrElse(index) { 0 }).compareTo(local.getOrElse(index) { 0 })
        if (comparison != 0) return comparison > 0
    }
    return false
}

private fun fullScreenIntentSettingsIntent(context: android.content.Context): Intent =
    Intent(AndroidSettings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
        data = Uri.parse("package:${context.packageName}")
    }
