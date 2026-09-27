package com.thelastecho.reminder.presentation.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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
import com.thelastecho.reminder.R
import com.thelastecho.reminder.core.alarm.AndroidAlarmScheduler
import com.thelastecho.reminder.core.alarm.ImportedReminderAlarmScheduler
import com.thelastecho.reminder.core.designsystem.ReminderShapes
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.data.attachments.AttachmentStore
import com.thelastecho.reminder.data.backup.BackupEnvelope
import com.thelastecho.reminder.data.backup.BackupPasswordOrCorruptException
import com.thelastecho.reminder.data.backup.BackupPasswordRequiredException
import com.thelastecho.reminder.data.backup.BackupRunner
import com.thelastecho.reminder.data.backup.BackupSettings
import com.thelastecho.reminder.data.backup.BackupSettingsRepository
import com.thelastecho.reminder.data.backup.InvalidReminderBackupException
import com.thelastecho.reminder.data.backup.ReminderArchiveRepository
import com.thelastecho.reminder.data.backup.SafBackupDestination
import com.thelastecho.reminder.data.backup.UnsupportedBackupEnvelopeException
import com.thelastecho.reminder.data.backup.UnsupportedBackupFormatException
import com.thelastecho.reminder.data.local.ReminderDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ManualBackupScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val settingsRepository = remember(context) { BackupSettingsRepository(context) }
    val settings by settingsRepository.settings.collectAsState(initial = BackupSettings())
    val userPreferences = remember(context) { UserPreferencesRepository(context) }
    val archiveRepository = remember(context) {
        ReminderArchiveRepository(
            context,
            ReminderDatabase.getInstance(context),
            userPreferences,
            ImportedReminderAlarmScheduler(AndroidAlarmScheduler(context)),
            AttachmentStore(context)
        )
    }
    var busy by remember { mutableStateOf(false) }
    var protectManualBackup by remember { mutableStateOf(false) }
    var manualPassword by remember { mutableStateOf("") }
    var manualPasswordConfirmation by remember { mutableStateOf("") }
    var showManualDialog by remember { mutableStateOf(false) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var importEncrypted by remember { mutableStateOf(false) }
    var restorePassword by remember { mutableStateOf("") }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf(ReminderArchiveRepository.RestoreMode.MERGE) }
    val manualDestinationUri = settings.manualDestinationTreeUri ?: settings.destinationTreeUri
    var destinationName by remember(manualDestinationUri) { mutableStateOf<String?>(null) }

    LaunchedEffect(manualDestinationUri) {
        destinationName = manualDestinationUri?.let { uri ->
            withContext(Dispatchers.IO) { readBackupDestinationName(context, uri) }
        }
    }

    val folderPicker = rememberBackupDestinationPicker(manual = true) { result ->
        if (result == BackupDestinationPickResult.SAVED) showManualDialog = true
        else if (result == BackupDestinationPickResult.FAILED) scope.launch {
            snackbar.showSnackbar(context.getString(R.string.backup_status_destination_unavailable))
        }
    }
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val detection = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use(BackupEnvelope::isEncrypted)
                        ?: throw IllegalStateException("The selected backup cannot be read.")
                }
            }
            detection.onSuccess { encrypted ->
                importUri = uri
                importEncrypted = encrypted
                restorePassword = ""
                showRestoreDialog = true
            }.onFailure { snackbar.showSnackbar(context.getString(R.string.backup_failed)) }
        }
    }

    fun openManualDialog() {
        if (manualDestinationUri.isNullOrBlank()) folderPicker.launch(null)
        else showManualDialog = true
    }

    fun restoreError(error: Throwable): String = when (error) {
        is BackupPasswordOrCorruptException -> context.getString(R.string.backup_password_or_damaged)
        is BackupPasswordRequiredException -> context.getString(R.string.backup_restore_password_prompt)
        is UnsupportedBackupFormatException, is UnsupportedBackupEnvelopeException -> context.getString(R.string.backup_unknown_format)
        is InvalidReminderBackupException, is IllegalArgumentException, is JSONException -> context.getString(R.string.backup_invalid)
        else -> context.getString(R.string.backup_failed)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_restore)) },
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.manual_backup_description), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        settings.lastManualBackupMillis?.let { context.getString(R.string.backup_last_manual, formatBackupTime(context, it)) }
                            ?: stringResource(R.string.backup_no_manual_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        context.getString(
                            R.string.backup_destination_label,
                            destinationName
                                ?: stringResource(if (manualDestinationUri == null) R.string.backup_no_folder else R.string.backup_folder_selected)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { folderPicker.launch(null) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                        Text(
                            stringResource(
                                if (settings.manualDestinationTreeUri == null) R.string.backup_manual_choose_folder
                                else R.string.backup_manual_change_folder,
                            ),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Button(onClick = ::openManualDialog, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Backup, contentDescription = null)
                        Text(stringResource(R.string.backup_create_now), modifier = Modifier.padding(start = 8.dp))
                    }
                    OutlinedButton(
                        onClick = { backupPicker.launch(arrayOf(SafBackupDestination.MIME_TYPE, "application/zip", "application/octet-stream")) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Restore, contentDescription = null)
                        Text(stringResource(R.string.backup_restore_action), modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(stringResource(R.string.restore_mode), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    ReminderArchiveRepository.RestoreMode.entries.forEach { mode ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = mode == selectedMode, onClick = { selectedMode = mode })
                            Text(stringResource(if (mode == ReminderArchiveRepository.RestoreMode.MERGE) R.string.merge_backup else R.string.replace_backup))
                        }
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = {
                showManualDialog = false
                protectManualBackup = false
                manualPassword = ""
                manualPasswordConfirmation = ""
            },
            title = { Text(stringResource(R.string.backup_manual_protection_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = protectManualBackup, onCheckedChange = { protectManualBackup = it })
                        Text(stringResource(R.string.backup_manual_protect_checkbox))
                    }
                    if (protectManualBackup) {
                        OutlinedTextField(
                            value = manualPassword,
                            onValueChange = { manualPassword = it },
                            label = { Text(stringResource(R.string.backup_password)) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = manualPasswordConfirmation,
                            onValueChange = { manualPasswordConfirmation = it },
                            label = { Text(stringResource(R.string.backup_password_confirmation)) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (manualPasswordConfirmation.isNotEmpty() && manualPassword != manualPasswordConfirmation) {
                            Text(
                                stringResource(R.string.backup_passwords_do_not_match),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && (!protectManualBackup || (manualPassword.isNotEmpty() && manualPassword == manualPasswordConfirmation)),
                    onClick = {
                        val password = manualPassword.takeIf { protectManualBackup }?.toCharArray()
                        showManualDialog = false
                        protectManualBackup = false
                        manualPassword = ""
                        manualPasswordConfirmation = ""
                        scope.launch {
                            busy = true
                            val result = runCatching {
                                withContext(Dispatchers.IO) { BackupRunner(context).runManualBackup(password) }
                            }
                            password?.fill('\u0000')
                            busy = false
                            val outcome = result.getOrNull()
                            val message = when {
                                result.isFailure -> context.getString(R.string.backup_failed)
                                outcome?.succeeded == true -> context.getString(R.string.backup_exported)
                                outcome?.status == com.thelastecho.reminder.data.backup.BackupStatus.DESTINATION_UNAVAILABLE -> context.getString(R.string.backup_status_destination_unavailable)
                                else -> context.getString(R.string.backup_failed)
                            }
                            snackbar.showSnackbar(message)
                        }
                    }
                ) { Text(stringResource(R.string.backup_create_now)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showManualDialog = false
                    protectManualBackup = false
                    manualPassword = ""
                    manualPasswordConfirmation = ""
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showRestoreDialog && importUri != null) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false; importUri = null; restorePassword = "" },
            title = { Text(stringResource(if (selectedMode == ReminderArchiveRepository.RestoreMode.REPLACE) R.string.replace_backup else R.string.merge_backup)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(if (selectedMode == ReminderArchiveRepository.RestoreMode.REPLACE) R.string.replace_backup_confirmation else R.string.merge_backup_confirmation))
                    if (importEncrypted) {
                        OutlinedTextField(
                            value = restorePassword,
                            onValueChange = { restorePassword = it },
                            label = { Text(stringResource(R.string.backup_password)) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !busy && (!importEncrypted || restorePassword.isNotEmpty()), onClick = {
                    val uri = importUri ?: return@TextButton
                    val password = restorePassword.takeIf { importEncrypted }?.toCharArray()
                    importUri = null
                    showRestoreDialog = false
                    scope.launch {
                        busy = true
                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                val input = context.contentResolver.openInputStream(uri)
                                    ?: throw IllegalStateException("The selected backup cannot be read.")
                                input.use { archiveRepository.restore(it, password, selectedMode) }
                            }
                        }
                        password?.fill('\u0000')
                        busy = false
                        result.onSuccess { report ->
                            val messages = mutableListOf(context.getString(R.string.restore_report, report.importedReminders, report.skippedReminders, report.conflicts))
                            if (report.alarmFailureReminderIds.isNotEmpty()) messages += context.getString(R.string.backup_restore_alarm_failures, report.alarmFailureReminderIds.size)
                            if (report.alarmSoundFallback) messages += context.getString(R.string.backup_restore_sound_fallback)
                            snackbar.showSnackbar(messages.joinToString("\n"))
                        }.onFailure { error -> snackbar.showSnackbar(restoreError(error)) }
                        restorePassword = ""
                    }
                }) { Text(stringResource(R.string.restore)) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false; importUri = null; restorePassword = "" }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
