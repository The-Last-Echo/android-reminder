package com.thelastecho.reminder.presentation.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thelastecho.reminder.core.preferences.DarkThemeConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }
    var showNotificationStyleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                SettingsEffect.NavigateToCategories -> onNavigateToCategories()
                SettingsEffect.NavigateToTrash -> onNavigateToTrash()
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Categories Section
            Text(
                text = stringResource(com.thelastecho.reminder.R.string.categories),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                shape = RoundedCornerShape(16.dp),
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

            // Data Management Section
            Text(
                text = stringResource(com.thelastecho.reminder.R.string.data_management),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onIntent(SettingsIntent.NavigateToTrash) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                    }
                }
            }

            // Notifications Section
            Text(
                text = stringResource(com.thelastecho.reminder.R.string.notifications),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                shape = RoundedCornerShape(16.dp),
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
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Appearance Section
            Text(
                text = stringResource(com.thelastecho.reminder.R.string.appearance),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            val localeIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Intent(AndroidSettings.ACTION_APP_LOCALE_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
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
            Text(
                text = stringResource(com.thelastecho.reminder.R.string.privacy_freedom),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
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
            Text(
                text = stringResource(com.thelastecho.reminder.R.string.about),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/The-Last-Echo/android-reminder"))) } }) {
                            Text(stringResource(com.thelastecho.reminder.R.string.app_version, state.appVersion), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(com.thelastecho.reminder.R.string.about_details),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
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
