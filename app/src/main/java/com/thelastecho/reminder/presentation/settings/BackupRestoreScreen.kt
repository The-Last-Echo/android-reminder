package com.thelastecho.reminder.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thelastecho.reminder.data.backup.ReminderBackupRepository
import com.thelastecho.reminder.data.local.ReminderDatabase
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val repository = remember { ReminderBackupRepository(context, ReminderDatabase.getInstance(context), UserPreferencesRepository(context)) }
    var restoreMode by remember { mutableStateOf(ReminderBackupRepository.RestoreMode.MERGE) }
    var importUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val result = runCatching { withContext(Dispatchers.IO) { repository.export(uri) } }
            busy = false
            snackbar.showSnackbar(result.fold({ context.getString(com.thelastecho.reminder.R.string.backup_exported) }, { context.getString(com.thelastecho.reminder.R.string.backup_failed) }))
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> importUri = uri }
    fun restoreError(error: Throwable): String = context.getString(
        if (error is IllegalArgumentException || error is org.json.JSONException)
            com.thelastecho.reminder.R.string.backup_invalid
        else com.thelastecho.reminder.R.string.backup_failed
    )

    Scaffold(modifier = modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snackbar) }, topBar = {
        TopAppBar(title = { Text(stringResource(com.thelastecho.reminder.R.string.backup_restore)) }, navigationIcon = {
            IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(com.thelastecho.reminder.R.string.backup_description), style = MaterialTheme.typography.bodyLarge)
            Button(enabled = !busy, onClick = { export.launch("Reminder-backup.json") }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(com.thelastecho.reminder.R.string.export_backup))
            }
            Text(stringResource(com.thelastecho.reminder.R.string.restore_mode), style = MaterialTheme.typography.titleMedium)
            ReminderBackupRepository.RestoreMode.entries.forEach { mode ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = mode == restoreMode, onClick = { restoreMode = mode })
                    Text(if (mode == ReminderBackupRepository.RestoreMode.MERGE) stringResource(com.thelastecho.reminder.R.string.merge_backup) else stringResource(com.thelastecho.reminder.R.string.replace_backup))
                }
            }
            Text(stringResource(com.thelastecho.reminder.R.string.backup_merge_replace_details), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(enabled = !busy, onClick = { import.launch(arrayOf("application/json", "text/json", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(com.thelastecho.reminder.R.string.choose_backup_file))
            }
            if (busy) CircularProgressIndicator()
        }
    }
    importUri?.let { uri ->
        if (restoreMode == ReminderBackupRepository.RestoreMode.REPLACE) {
            AlertDialog(onDismissRequest = { importUri = null }, title = { Text(stringResource(com.thelastecho.reminder.R.string.replace_backup)) },
                text = { Text(stringResource(com.thelastecho.reminder.R.string.replace_backup_confirmation)) },
                confirmButton = { TextButton(onClick = {
                    importUri = null
                    scope.launch {
                        busy = true
                        val result = runCatching { withContext(Dispatchers.IO) { repository.restore(uri, restoreMode) } }
                        busy = false
                        snackbar.showSnackbar(result.fold({ context.getString(com.thelastecho.reminder.R.string.restore_report, it.importedReminders, it.skippedReminders, it.conflicts) }, { restoreError(it) }))
                    }
                }) { Text(stringResource(com.thelastecho.reminder.R.string.replace)) } },
                dismissButton = { TextButton(onClick = { importUri = null }) { Text(stringResource(com.thelastecho.reminder.R.string.cancel)) } })
        } else {
            AlertDialog(onDismissRequest = { importUri = null }, title = { Text(stringResource(com.thelastecho.reminder.R.string.merge_backup)) },
                text = { Text(stringResource(com.thelastecho.reminder.R.string.merge_backup_confirmation)) },
                confirmButton = { TextButton(onClick = {
                    importUri = null
                    scope.launch {
                        busy = true
                        val result = runCatching { withContext(Dispatchers.IO) { repository.restore(uri, restoreMode) } }
                        busy = false
                        snackbar.showSnackbar(result.fold({ context.getString(com.thelastecho.reminder.R.string.restore_report, it.importedReminders, it.skippedReminders, it.conflicts) }, { restoreError(it) }))
                    }
                }) { Text(stringResource(com.thelastecho.reminder.R.string.merge)) } },
                dismissButton = { TextButton(onClick = { importUri = null }) { Text(stringResource(com.thelastecho.reminder.R.string.cancel)) } })
        }
    }
}
