package com.thelastecho.reminder.presentation.settings

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.work.WorkManager
import com.thelastecho.reminder.R
import com.thelastecho.reminder.core.designsystem.ReminderShapes
import com.thelastecho.reminder.core.preferences.AppThemeSettings
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.data.backup.BackupFrequency
import com.thelastecho.reminder.data.backup.BackupRetention
import com.thelastecho.reminder.data.backup.BackupSettings
import com.thelastecho.reminder.data.backup.BackupSettingsRepository
import com.thelastecho.reminder.data.backup.BackupStatus
import com.thelastecho.reminder.data.backup.BackupWorkScheduler
import com.thelastecho.reminder.data.backup.EncryptedBackupSecretStore
import com.thelastecho.reminder.data.backup.ReminderBackupWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

private enum class BackupPasswordDialogAction { ENABLE, CHANGE }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AutomaticBackupScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val settingsRepository = remember(context) { BackupSettingsRepository(context) }
    val settings by settingsRepository.settings.collectAsState(initial = BackupSettings())
    val appSettings by remember(context) { UserPreferencesRepository(context) }.themeSettings.collectAsState(initial = AppThemeSettings())
    val secretStore = remember(context) { EncryptedBackupSecretStore(context) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordConfirmation by remember { mutableStateOf("") }
    var passwordDialogAction by remember { mutableStateOf<BackupPasswordDialogAction?>(null) }
    var showDisableEncryptionDialog by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var nextBackupMillis by remember { mutableStateOf<Long?>(null) }
    var destinationName by remember(settings.destinationTreeUri) { mutableStateOf<String?>(null) }

    val folderPicker = rememberBackupDestinationPicker { result ->
        if (result == BackupDestinationPickResult.FAILED) scope.launch {
            snackbar.showSnackbar(context.getString(R.string.backup_status_destination_unavailable))
        }
    }

    LaunchedEffect(settings.frequency, settings.destinationTreeUri) {
        BackupWorkScheduler.synchronize(context, settings)
    }
    LaunchedEffect(settings.frequency, settings.destinationTreeUri, settings.lastAutomaticBackupMillis) {
        nextBackupMillis = withContext(Dispatchers.IO) {
            runCatching {
                WorkManager.getInstance(context)
                    .getWorkInfosForUniqueWork(ReminderBackupWorker.UNIQUE_WORK_NAME)
                    .get()
                    .firstOrNull { !it.state.isFinished }
                    ?.nextScheduleTimeMillis
                    ?.takeIf { it > 0L }
            }.getOrNull()
        }
    }
    LaunchedEffect(settings.destinationTreeUri) {
        destinationName = settings.destinationTreeUri?.let { uri ->
            withContext(Dispatchers.IO) { readBackupDestinationName(context, uri) }
        }
    }

    fun frequencyLabel(value: BackupFrequency): String = context.getString(when (value) {
        BackupFrequency.DISABLED -> R.string.backup_disabled
        BackupFrequency.DAILY -> R.string.backup_daily
        BackupFrequency.EVERY_THREE_DAYS -> R.string.backup_every_three_days
        BackupFrequency.WEEKLY -> R.string.backup_weekly
        BackupFrequency.EVERY_THIRTY_DAYS -> R.string.backup_every_30_days
    })
    fun retentionLabel(value: BackupRetention): String = context.getString(when (value) {
        BackupRetention.THREE -> R.string.backup_keep_3
        BackupRetention.FIVE -> R.string.backup_keep_5
        BackupRetention.TEN -> R.string.backup_keep_10
        BackupRetention.TWENTY -> R.string.backup_keep_20
        BackupRetention.UNLIMITED -> R.string.backup_keep_unlimited
    })

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_automatic_title)) },
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
        ) {
            if (busy) CircularProgressIndicator()
            Card(
                shape = ReminderShapes.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(R.string.backup_automatic_description), style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(onClick = { folderPicker.launch(null) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                        Text(
                            destinationName ?: stringResource(
                                if (settings.destinationTreeUri == null) R.string.backup_select_folder
                                else R.string.backup_folder_selected
                            ),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    HorizontalDivider()
                    Text(stringResource(R.string.backup_auto_frequency), style = MaterialTheme.typography.titleSmall)
                    BackupFrequency.entries.forEach { option ->
                        Row(Modifier.fillMaxWidth().clickable(enabled = !busy && (settings.destinationTreeUri != null || option == BackupFrequency.DISABLED)) {
                            scope.launch { settingsRepository.setFrequency(option) }
                        }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = settings.frequency == option,
                                onClick = { scope.launch { settingsRepository.setFrequency(option) } },
                                enabled = !busy && (settings.destinationTreeUri != null || option == BackupFrequency.DISABLED)
                            )
                            Text(frequencyLabel(option), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Text(stringResource(R.string.backup_retention), style = MaterialTheme.typography.titleSmall)
                    BackupRetention.entries.forEach { option ->
                        Row(Modifier.fillMaxWidth().clickable(enabled = !busy) {
                            scope.launch { settingsRepository.setRetention(option) }
                        }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = settings.retention == option, onClick = {
                                scope.launch { settingsRepository.setRetention(option) }
                            }, enabled = !busy)
                            Text(retentionLabel(option), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.backup_auto_encryption_label), modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.encryptionEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    passwordInput = ""
                                    passwordConfirmation = ""
                                    passwordDialogAction = BackupPasswordDialogAction.ENABLE
                                } else {
                                    showDisableEncryptionDialog = true
                                }
                            },
                            enabled = !busy
                        )
                    }
                    if (settings.encryptionEnabled) {
                        OutlinedButton(
                            onClick = {
                                passwordInput = ""
                                passwordConfirmation = ""
                                passwordDialogAction = BackupPasswordDialogAction.CHANGE
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.backup_change_password)) }
                        OutlinedButton(
                            onClick = { showDisableEncryptionDialog = true },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.backup_disable_encryption)) }
                    }
                    HorizontalDivider()
                    Text(
                        settings.lastAutomaticBackupMillis?.let { context.getString(R.string.backup_last_automatic, formatBackupTime(context, it)) }
                            ?: stringResource(R.string.backup_no_automatic_yet),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        nextBackupMillis?.let { context.getString(R.string.backup_next_automatic, formatBackupTime(context, it)) }
                            ?: stringResource(if (settings.frequency == BackupFrequency.DISABLED) R.string.backup_next_disabled else R.string.backup_next_pending),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (settings.retentionWarning) {
                        Text(stringResource(R.string.backup_retention_warning), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    val statusMessage = when (settings.status) {
                        BackupStatus.NONE -> null
                        BackupStatus.DESTINATION_UNAVAILABLE -> R.string.backup_status_destination_unavailable
                        BackupStatus.PASSWORD_REQUIRED -> R.string.backup_status_password_required
                        BackupStatus.BACKUP_FAILED -> R.string.backup_failed
                    }
                    statusMessage?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    if (appSettings.unreadableLegacyAttachmentCount > 0) {
                        Text(
                            stringResource(R.string.unreadable_legacy_attachments, appSettings.unreadableLegacyAttachmentCount),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    passwordDialogAction?.let { action ->
        AlertDialog(
            onDismissRequest = {
                passwordDialogAction = null
                passwordInput = ""
                passwordConfirmation = ""
            },
            title = { Text(stringResource(if (action == BackupPasswordDialogAction.ENABLE) R.string.backup_define_password else R.string.backup_change_password)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text(stringResource(R.string.backup_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = passwordConfirmation,
                        onValueChange = { passwordConfirmation = it },
                        label = { Text(stringResource(R.string.backup_password_confirmation)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passwordConfirmation.isNotEmpty() && passwordInput != passwordConfirmation) {
                        Text(stringResource(R.string.backup_passwords_do_not_match), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && passwordInput.isNotEmpty() && passwordInput == passwordConfirmation,
                    onClick = {
                        val chars = passwordInput.toCharArray()
                        passwordDialogAction = null
                        passwordInput = ""
                        passwordConfirmation = ""
                        scope.launch {
                            busy = true
                            val saved = runCatching {
                                withContext(Dispatchers.IO) {
                                    secretStore.savePassword(chars)
                                    if (action == BackupPasswordDialogAction.ENABLE) settingsRepository.setEncryptionEnabled(true)
                                }
                            }
                            chars.fill('\u0000')
                            busy = false
                            if (saved.isSuccess) snackbar.showSnackbar(context.getString(R.string.backup_password_saved))
                            else snackbar.showSnackbar(context.getString(R.string.backup_failed))
                        }
                    }
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    passwordDialogAction = null
                    passwordInput = ""
                    passwordConfirmation = ""
                }, enabled = !busy) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showDisableEncryptionDialog) {
        AlertDialog(
            onDismissRequest = { showDisableEncryptionDialog = false },
            title = { Text(stringResource(R.string.backup_disable_encryption)) },
            text = { Text(stringResource(R.string.backup_disable_encryption_confirmation)) },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    showDisableEncryptionDialog = false
                    scope.launch {
                        busy = true
                        val disabled = runCatching {
                            withContext(Dispatchers.IO) { secretStore.clear() }
                            settingsRepository.setEncryptionEnabled(false)
                        }
                        busy = false
                        if (disabled.isFailure) snackbar.showSnackbar(context.getString(R.string.backup_failed))
                    }
                }) { Text(stringResource(R.string.backup_disable_encryption)) }
            },
            dismissButton = {
                TextButton(onClick = { showDisableEncryptionDialog = false }, enabled = !busy) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

}
