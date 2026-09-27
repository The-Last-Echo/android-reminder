package com.thelastecho.reminder.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.thelastecho.reminder.data.backup.BackupSettingsRepository
import com.thelastecho.reminder.data.backup.BackupWorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

enum class BackupDestinationPickResult { SAVED, CANCELLED, FAILED }

@Composable
fun rememberBackupDestinationPicker(
    manual: Boolean = false,
    onResult: (BackupDestinationPickResult) -> Unit
): ActivityResultLauncher<Uri?> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) {
            onResult(BackupDestinationPickResult.CANCELLED)
        } else {
            scope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) { persistDestination(context, uri, manual) }
                }
                onResult(if (result.isSuccess) BackupDestinationPickResult.SAVED else BackupDestinationPickResult.FAILED)
            }
        }
    }
}

private suspend fun persistDestination(context: Context, uri: Uri, manual: Boolean) {
    context.contentResolver.takePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    )
    val repository = BackupSettingsRepository(context)
    if (manual) {
        repository.setManualDestination(uri.toString())
    } else {
        repository.setDestination(uri.toString())
        BackupWorkScheduler.synchronize(context, repository.settings.first())
    }
}

fun readBackupDestinationName(context: Context, treeUriString: String): String? = runCatching {
    val treeUri = Uri.parse(treeUriString)
    val documentUri = DocumentsContract.buildDocumentUriUsingTree(
        treeUri,
        DocumentsContract.getTreeDocumentId(treeUri)
    )
    context.contentResolver.query(
        documentUri,
        arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull()

fun formatBackupTime(context: Context, timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, context.resources.configuration.locales[0])
        .format(Date(timestamp))
