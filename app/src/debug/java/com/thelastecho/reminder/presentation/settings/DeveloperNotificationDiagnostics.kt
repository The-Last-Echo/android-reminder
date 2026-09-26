package com.thelastecho.reminder.presentation.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thelastecho.reminder.R
import com.thelastecho.reminder.core.notification.NotificationDiagnostics
import com.thelastecho.reminder.core.notification.NotificationDiagnosticsSnapshot
import com.thelastecho.reminder.presentation.components.SectionHeading

@Composable
internal fun DeveloperNotificationDiagnostics() {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf<NotificationDiagnosticsSnapshot?>(null) }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        snapshot = NotificationDiagnostics.read(context)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeading(stringResource(R.string.developer_tools))
        TextButton(onClick = { snapshot = NotificationDiagnostics.read(context) }) {
            Text(stringResource(R.string.notification_diagnostics))
        }
    }

    snapshot?.let { diagnostics ->
        AlertDialog(
            onDismissRequest = { snapshot = null },
            title = { Text(stringResource(R.string.notification_diagnostics)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(stringResource(R.string.post_notifications_permission) + ": " + diagnosticAccessLabel(diagnostics.postNotificationsPermission))
                    Text(
                        stringResource(R.string.app_notifications_enabled) + ": " +
                            stringResource(if (diagnostics.appNotificationsEnabled) R.string.diagnostic_enabled else R.string.diagnostic_blocked)
                    )
                    Text(stringResource(R.string.full_screen_access_label) + ": " + diagnosticAccessLabel(diagnostics.fullScreenIntentAccess))
                    if (diagnostics.fullScreenIntentAccess == NotificationDiagnosticsSnapshot.AccessState.BLOCKED) {
                        Text(stringResource(R.string.full_screen_fallback_details), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = {
                            runCatching {
                                settingsLauncher.launch(
                                    Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                )
                            }
                        }) {
                            Text(stringResource(R.string.full_screen_settings))
                        }
                    }
                    SectionHeading(stringResource(R.string.effective_channels_label))
                    if (diagnostics.channels.isEmpty()) Text(stringResource(R.string.diagnostic_channel_unavailable))
                    diagnostics.channels.forEach { channel ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(channel.name, style = MaterialTheme.typography.titleSmall)
                            Text(channel.id, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val details = if (channel.importance == null) {
                                stringResource(R.string.diagnostic_channel_unavailable)
                            } else {
                                stringResource(
                                    R.string.diagnostic_channel_details,
                                    diagnosticImportanceLabel(channel.importance),
                                    if (channel.hasSound == true) stringResource(R.string.diagnostic_enabled) + " (${channel.soundUri.orEmpty()})" else stringResource(R.string.diagnostic_disabled),
                                    stringResource(if (channel.vibrates == true) R.string.diagnostic_enabled else R.string.diagnostic_disabled)
                                )
                            }
                            Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { snapshot = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun diagnosticAccessLabel(state: NotificationDiagnosticsSnapshot.AccessState): String = stringResource(when (state) {
    NotificationDiagnosticsSnapshot.AccessState.ALLOWED -> R.string.diagnostic_allowed
    NotificationDiagnosticsSnapshot.AccessState.BLOCKED -> R.string.diagnostic_blocked
    NotificationDiagnosticsSnapshot.AccessState.NOT_REQUIRED -> R.string.diagnostic_not_required
    NotificationDiagnosticsSnapshot.AccessState.NOT_AVAILABLE -> R.string.diagnostic_not_available
})

@Composable
private fun diagnosticImportanceLabel(importance: Int): String = stringResource(when (importance) {
    android.app.NotificationManager.IMPORTANCE_NONE -> R.string.diagnostic_importance_none
    android.app.NotificationManager.IMPORTANCE_MIN -> R.string.diagnostic_importance_min
    android.app.NotificationManager.IMPORTANCE_LOW -> R.string.diagnostic_importance_low
    android.app.NotificationManager.IMPORTANCE_DEFAULT -> R.string.diagnostic_importance_default
    android.app.NotificationManager.IMPORTANCE_HIGH -> R.string.diagnostic_importance_high
    android.app.NotificationManager.IMPORTANCE_MAX -> R.string.diagnostic_importance_max
    else -> R.string.diagnostic_not_available
})
