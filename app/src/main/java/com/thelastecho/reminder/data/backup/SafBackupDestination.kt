package com.thelastecho.reminder.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.IOException

object SafBackupDestination {
    const val MIME_TYPE = "application/vnd.thelastecho.reminderbackup"
    private const val NAME_PREFIX = "Reminder-"
    private const val NAME_SUFFIX = ".reminderbackup"

    enum class Origin { AUTOMATIC, MANUAL }

    data class BackupDocument(val uri: Uri, val displayName: String, val lastModified: Long)

    fun createBackup(context: Context, treeUri: Uri, timestamp: Long, origin: Origin): BackupDocument {
        val resolver = context.contentResolver
        val treeId = DocumentsContract.getTreeDocumentId(treeUri)
        val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeId)
        val originPrefix = if (origin == Origin.AUTOMATIC) "Auto-" else "Manual-"
        val name = "$NAME_PREFIX$originPrefix$timestamp-${java.util.UUID.randomUUID()}$NAME_SUFFIX"
        val uri = DocumentsContract.createDocument(resolver, parent, MIME_TYPE, name)
            ?: throw IOException("The selected folder cannot create backup files.")
        return BackupDocument(uri, name, timestamp)
    }

    fun listBackups(context: Context, treeUri: Uri): List<BackupDocument> {
        val resolver = context.contentResolver
        val treeId = DocumentsContract.getTreeDocumentId(treeUri)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        val result = mutableListOf<BackupDocument>()
        val cursor = resolver.query(children, projection, null, null, null)
            ?: throw IOException("The selected folder cannot list its contents.")
        cursor.use {
            val idIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val modifiedIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            while (it.moveToNext()) {
                val name = it.getString(nameIndex) ?: continue
                if (!isAutomaticBackupName(name)) continue
                val documentId = it.getString(idIndex) ?: continue
                val modified = if (modifiedIndex >= 0 && !it.isNull(modifiedIndex)) it.getLong(modifiedIndex) else 0L
                result += BackupDocument(
                    DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId),
                    name,
                    modified
                )
            }
        }
        return result
    }

    fun delete(context: Context, document: BackupDocument): Boolean =
        DocumentsContract.deleteDocument(context.contentResolver, document.uri)

    internal fun isAutomaticBackupName(name: String): Boolean =
        name.startsWith("${NAME_PREFIX}Auto-") && name.endsWith(NAME_SUFFIX)
}
