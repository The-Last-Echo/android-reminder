package com.thelastecho.reminder.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.thelastecho.reminder.data.local.UpdateCheckWorker
import androidx.compose.ui.Alignment
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelastecho.reminder.core.designsystem.ReminderShapes
import com.thelastecho.reminder.core.preferences.DarkThemeConfig
import com.thelastecho.reminder.presentation.components.SectionHeading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val packageVersion = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val picked = if (Build.VERSION.SDK_INT >= 33) result.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI, android.net.Uri::class.java) else @Suppress("DEPRECATION") result.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            viewModel.onIntent(SettingsIntent.SetAlarmSound(picked?.toString()))
        }
    }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAddPositionDialog by remember { mutableStateOf(false) }
    var showNotificationStyleDialog by remember { mutableStateOf(false) }
    var showRetentionDialog by remember { mutableStateOf(false) }
    var customRetention by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                SettingsEffect.NavigateToCategories -> onNavigateToCategories()
                SettingsEffect.NavigateToTrash -> onNavigateToTrash()
                SettingsEffect.NavigateToBackup -> onNavigateToBackup()
                SettingsEffect.NavigateToPrivacy -> onNavigateToPrivacy()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.thelastecho.reminder.R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(com.thelastecho.reminder.R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeading(stringResource(com.thelastecho.reminder.R.string.general))

            Card(
                shape = ReminderShapes.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            val localeIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Intent(AndroidSettings.ACTION_APP_LOCALE_SETTINGS).setData(android.net.Uri.parse("package:${context.packageName}"))
                            } else {
                                Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
                            }
                            runCatching { context.startActivity(localeIntent) }
                        }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(com.thelastecho.reminder.R.string.app_language), style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(com.thelastecho.reminder.R.string.app_language_details), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().clickable { showAddPositionDialog = true }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(com.thelastecho.reminder.R.string.add_button_position), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(if (state.addButtonOnLeft) com.thelastecho.reminder.R.string.position_left else com.thelastecho.reminder.R.string.position_right),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Categories Section
            SectionHeading(stringResource(com.thelastecho.reminder.R.string.categories))

            Card(
                shape = ReminderShapes.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onIntent(SettingsIntent.NavigateToCategories) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(stringResource(com.thelastecho.reminder.R.string.manage_categories), style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    stringResource(com.thelastecho.reminder.R.string.create_manage_categories),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            SectionHeading(stringResource(com.thelastecho.reminder.R.string.reminders))

            Card(
                shape = ReminderShapes.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onIntent(SettingsIntent.NavigateToTrash) }
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(com.thelastecho.reminder.R.string.trash), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(com.thelastecho.reminder.R.string.view_restore_deleted),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRetentionDialog = true }
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(com.thelastecho.reminder.R.string.completed_retention), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (state.completedReminderRetentionDays == 0) {
                                    stringResource(com.thelastecho.reminder.R.string.retention_never)
                                } else {
                                    context.resources.getQuantityString(
                                        com.thelastecho.reminder.R.plurals.retention_days,
                                        state.completedReminderRetentionDays,
                                        state.completedReminderRetentionDays
                                    )
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            SectionHeading(stringResource(com.thelastecho.reminder.R.string.data_management))

            Card(
                shape = ReminderShapes.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth().clickable { viewModel.onIntent(SettingsIntent.NavigateToBackup) }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(com.thelastecho.reminder.R.string.backup_restore), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(com.thelastecho.reminder.R.string.backup_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Notifications Section
            SectionHeading(stringResource(com.thelastecho.reminder.R.string.notifications))

            Card(
                shape = ReminderShapes.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNotificationStyleDialog = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(stringResource(com.thelastecho.reminder.R.string.notification_style), style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = when (state.notificationStyle) {
                                        com.thelastecho.reminder.core.preferences.NotificationStyle.SIMPLE -> stringResource(com.thelastecho.reminder.R.string.simple_style_name)
                                        com.thelastecho.reminder.core.preferences.NotificationStyle.FULL_SCREEN -> stringResource(com.thelastecho.reminder.R.string.full_screen_style_name)
                                        com.thelastecho.reminder.core.preferences.NotificationStyle.HEADS_UP -> stringResource(com.thelastecho.reminder.R.string.heads_up_style_name)
                                        com.thelastecho.reminder.core.preferences.NotificationStyle.NONE -> stringResource(com.thelastecho.reminder.R.string.notification_none_name)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (state.notificationStyle == com.thelastecho.reminder.core.preferences.NotificationStyle.FULL_SCREEN && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !context.getSystemService(android.app.NotificationManager::class.java).canUseFullScreenIntent()) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(com.thelastecho.reminder.R.string.full_screen_fallback_details), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { runCatching { context.startActivity(Intent(AndroidSettings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).setData(android.net.Uri.parse("package:${context.packageName}"))) } }) { Text(stringResource(com.thelastecho.reminder.R.string.full_screen_settings)) }
                }
            }
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                TextButton(onClick = { runCatching { context.startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)) } }) {
                    Text(stringResource(com.thelastecho.reminder.R.string.open_notification_settings))
                }
            }
            Card(
                shape = ReminderShapes.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth().clickable {
                    val intent = Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM)
                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(com.thelastecho.reminder.R.string.choose_alarm_sound))
                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, state.alarmSoundUri?.let(android.net.Uri::parse))
                    }
                    ringtonePicker.launch(intent)
                }
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(com.thelastecho.reminder.R.string.choose_alarm_sound), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (state.alarmSoundUri == null) stringResource(com.thelastecho.reminder.R.string.system_default) else stringResource(com.thelastecho.reminder.R.string.selected),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Appearance Section
            SectionHeading(stringResource(com.thelastecho.reminder.R.string.appearance))

            Card(
                shape = ReminderShapes.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Theme selector item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showThemeDialog = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(stringResource(com.thelastecho.reminder.R.string.app_theme), style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = when (state.darkThemeConfig) {
                                        DarkThemeConfig.FOLLOW_SYSTEM -> stringResource(com.thelastecho.reminder.R.string.system_default)
                                        DarkThemeConfig.LIGHT -> stringResource(com.thelastecho.reminder.R.string.light)
                                        DarkThemeConfig.DARK -> stringResource(com.thelastecho.reminder.R.string.dark)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Pure AMOLED black toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(com.thelastecho.reminder.R.string.amoled_pure_black), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(com.thelastecho.reminder.R.string.amoled_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.isAmoledMode,
                            onCheckedChange = { viewModel.onIntent(SettingsIntent.SetAmoledMode(it)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))



                    // Dynamic colors toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(stringResource(com.thelastecho.reminder.R.string.dynamic_colors), style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    stringResource(com.thelastecho.reminder.R.string.dynamic_colors_description),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = state.useDynamicColors,
                            onCheckedChange = { viewModel.onIntent(SettingsIntent.SetDynamicColors(it)) }
                        )
                    }
                }
            }

            // Privacy & Freedom Section
            SectionHeading(stringResource(com.thelastecho.reminder.R.string.privacy_freedom))

            Card(
                shape = ReminderShapes.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth().clickable { viewModel.onIntent(SettingsIntent.NavigateToPrivacy) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(com.thelastecho.reminder.R.string.foss_description), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(com.thelastecho.reminder.R.string.foss_details),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // About Section
            SectionHeading(stringResource(com.thelastecho.reminder.R.string.about))

            Card(
                shape = ReminderShapes.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth().clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/The-Last-Echo/android-reminder"))) } }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(com.thelastecho.reminder.R.string.app_version, packageVersion), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(com.thelastecho.reminder.R.string.about_details),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
                        Card(shape = ReminderShapes.Card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(com.thelastecho.reminder.R.string.automatic_update_checks), style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(com.thelastecho.reminder.R.string.automatic_update_checks_details), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = state.automaticUpdateChecks, onCheckedChange = { viewModel.onIntent(SettingsIntent.SetAutomaticUpdateChecks(it)) })
                    }
                    TextButton(onClick = {
                        val request = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
                            .setInputData(workDataOf(UpdateCheckWorker.KEY_MANUAL to true))
                            .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build())
                            .build()
                        WorkManager.getInstance(context).enqueueUniqueWork(UpdateCheckWorker.UNIQUE_MANUAL, androidx.work.ExistingWorkPolicy.REPLACE, request)
                    }) { Text(stringResource(com.thelastecho.reminder.R.string.check_updates_now)) }
                    state.latestReleaseTag?.let { tag ->
                        val newer = isVersionNewer(tag, packageVersion)
                        Text(stringResource(if (newer) com.thelastecho.reminder.R.string.update_available else com.thelastecho.reminder.R.string.app_up_to_date), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(com.thelastecho.reminder.R.string.latest_release, tag), style = MaterialTheme.typography.bodySmall)
                        state.latestReleaseUrl?.let { url -> TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) } }) { Text(stringResource(com.thelastecho.reminder.R.string.open_release)) } }
                    }
                }
            }

        }
    }

    if (showRetentionDialog) {
        androidx.compose.material3.AlertDialog(onDismissRequest = { showRetentionDialog = false }, title = { Text(stringResource(com.thelastecho.reminder.R.string.completed_retention)) }, text = {
            Column {
                listOf(0, 1, 7, 30, 90).forEach { days ->
                    Row(Modifier.fillMaxWidth().clickable { viewModel.onIntent(SettingsIntent.SetCompletedRetention(days)); showRetentionDialog = false }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = state.completedReminderRetentionDays == days, onClick = { viewModel.onIntent(SettingsIntent.SetCompletedRetention(days)); showRetentionDialog = false })
                        Text(when (days) { 0 -> stringResource(com.thelastecho.reminder.R.string.retention_never); 1 -> stringResource(com.thelastecho.reminder.R.string.retention_1_day); 7 -> stringResource(com.thelastecho.reminder.R.string.retention_7_days); 30 -> stringResource(com.thelastecho.reminder.R.string.retention_30_days); else -> stringResource(com.thelastecho.reminder.R.string.retention_90_days) })
                    }
                }
                OutlinedTextField(value = customRetention, onValueChange = { customRetention = it.filter(Char::isDigit).take(4) }, label = { Text(stringResource(com.thelastecho.reminder.R.string.custom_duration)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        }, confirmButton = { TextButton(onClick = { customRetention.toIntOrNull()?.takeIf { it in 1..3650 }?.let { viewModel.onIntent(SettingsIntent.SetCompletedRetention(it)) }; showRetentionDialog = false }) { Text(stringResource(com.thelastecho.reminder.R.string.save)) } }, dismissButton = { TextButton(onClick = { showRetentionDialog = false }) { Text(stringResource(com.thelastecho.reminder.R.string.cancel)) } })
    }

    if (showAddPositionDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddPositionDialog = false },
            title = { Text(stringResource(com.thelastecho.reminder.R.string.add_button_position)) },
            text = {
                Column {
                    listOf(true, false).forEach { onLeft ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    viewModel.onIntent(SettingsIntent.SetAddButtonOnLeft(onLeft))
                                    showAddPositionDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.addButtonOnLeft == onLeft,
                                onClick = {
                                    viewModel.onIntent(SettingsIntent.SetAddButtonOnLeft(onLeft))
                                    showAddPositionDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(if (onLeft) com.thelastecho.reminder.R.string.position_left else com.thelastecho.reminder.R.string.position_right))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddPositionDialog = false }) {
                    Text(stringResource(com.thelastecho.reminder.R.string.ok))
                }
            }
        )
    }

    if (showNotificationStyleDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showNotificationStyleDialog = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showNotificationStyleDialog = false }) {
                    Text(stringResource(com.thelastecho.reminder.R.string.cancel))
                }
            },
            title = { Text(stringResource(com.thelastecho.reminder.R.string.choose_notification_style)) },
            text = {
                Column {
                    com.thelastecho.reminder.core.preferences.NotificationStyle.entries.forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onIntent(SettingsIntent.SetNotificationStyle(style))
                                    showNotificationStyleDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.notificationStyle == style,
                                onClick = {
                                    viewModel.onIntent(SettingsIntent.SetNotificationStyle(style))
                                    showNotificationStyleDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (style) {
                                    com.thelastecho.reminder.core.preferences.NotificationStyle.SIMPLE -> stringResource(com.thelastecho.reminder.R.string.simple_notification)
                                    com.thelastecho.reminder.core.preferences.NotificationStyle.FULL_SCREEN -> stringResource(com.thelastecho.reminder.R.string.full_screen_notification)
                                    com.thelastecho.reminder.core.preferences.NotificationStyle.HEADS_UP -> stringResource(com.thelastecho.reminder.R.string.heads_up_notification)
                                    com.thelastecho.reminder.core.preferences.NotificationStyle.NONE -> stringResource(com.thelastecho.reminder.R.string.notification_none_name)
                                }
                            )
                        }
                    }
                }
            }
        )
    }

    if (showThemeDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(com.thelastecho.reminder.R.string.cancel))
                }
            },
            title = { Text(stringResource(com.thelastecho.reminder.R.string.choose_theme)) },
            text = {
                Column {
                    DarkThemeConfig.entries.forEach { config ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onIntent(SettingsIntent.SetDarkThemeConfig(config))
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.darkThemeConfig == config,
                                onClick = {
                                    viewModel.onIntent(SettingsIntent.SetDarkThemeConfig(config))
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (config) {
                                    DarkThemeConfig.FOLLOW_SYSTEM -> stringResource(com.thelastecho.reminder.R.string.system_default)
                                    DarkThemeConfig.LIGHT -> stringResource(com.thelastecho.reminder.R.string.light)
                                    DarkThemeConfig.DARK -> stringResource(com.thelastecho.reminder.R.string.dark)
                                }
                            )
                        }
                    }
                }
            }
        )
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
