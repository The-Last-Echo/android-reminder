package com.thelastecho.reminder.data.attachments

import android.content.Context
import android.net.Uri
import com.thelastecho.reminder.data.local.dao.ReminderDao
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID

class AttachmentStore(context: Context) {
    private val appContext = context.applicationContext
    private val attachmentDirectory = File(appContext.filesDir, DIRECTORY_NAME)

    data class StoredAttachment(val relativePath: String)
    data class MigrationReport(val migrated: Int, val unreadable: Int)

    fun copyFromUri(uri: Uri): StoredAttachment {
        val input = appContext.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not read the selected photo.")
        val extension = extensionFor(appContext.contentResolver.getType(uri))
        return copy(input, extension)
    }

    fun copyFrom(input: InputStream): StoredAttachment = copy(input, "img")

    fun open(relativePath: String?, legacyUri: String?): InputStream? {
        if (!relativePath.isNullOrBlank()) {
            resolveRelativePath(relativePath)?.let { file ->
                runCatching { FileInputStream(file) }.getOrNull()?.let { return it }
            }
        }
        if (legacyUri.isNullOrBlank()) return null
        return runCatching {
            appContext.contentResolver.openInputStream(Uri.parse(legacyUri))
        }.getOrNull()
    }

    fun openReference(reference: String?): InputStream? =
        if (reference?.startsWith("$DIRECTORY_NAME/") == true) open(reference, null)
        else open(null, reference)

    fun delete(relativePath: String?) {
        if (relativePath.isNullOrBlank()) return
        resolveRelativePath(relativePath)?.delete()
    }

    suspend fun migrateLegacyReferences(dao: ReminderDao): MigrationReport {
        var migrated = 0
        var unreadable = 0
        dao.getLegacyAttachmentReminders().forEach { reminder ->
            val legacyUri = reminder.imageUri ?: return@forEach
            val stored = runCatching { copyFromUri(Uri.parse(legacyUri)) }.getOrElse {
                if (!reminder.isDeleted) unreadable++
                return@forEach
            }
            try {
                dao.setMigratedAttachment(reminder.id, stored.relativePath)
                migrated++
            } catch (error: Throwable) {
                delete(stored.relativePath)
                throw error
            }
        }
        return MigrationReport(migrated, unreadable)
    }

    private fun copy(input: InputStream, extension: String): StoredAttachment {
        if (!attachmentDirectory.exists() && !attachmentDirectory.mkdirs()) {
            input.close()
            throw IOException("Could not create the private attachment directory.")
        }
        val id = UUID.randomUUID().toString()
        val temporary = File(attachmentDirectory, "$id.tmp")
        val destination = File(attachmentDirectory, "$id.$extension")
        try {
            input.use { source ->
                FileOutputStream(temporary).use { output ->
                    source.copyTo(output)
                    output.fd.sync()
                }
            }
            if (!temporary.renameTo(destination)) throw IOException("Could not finalize the private photo copy.")
            return StoredAttachment(relativePath = "$DIRECTORY_NAME/${destination.name}")
        } catch (error: Throwable) {
            temporary.delete()
            destination.delete()
            throw error
        }
    }

    private fun resolveRelativePath(relativePath: String): File? {
        if (!relativePath.startsWith("$DIRECTORY_NAME/")) return null
        val name = relativePath.removePrefix("$DIRECTORY_NAME/")
        if (name.isBlank() || '/' in name || '\\' in name) return null
        val root = attachmentDirectory.canonicalFile
        val file = File(root, name).canonicalFile
        return file.takeIf { it.parentFile == root }
    }

    private fun extensionFor(mimeType: String?): String = when (mimeType?.lowercase()) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "image/heic" -> "heic"
        "image/heif" -> "heif"
        else -> "img"
    }

    companion object {
        private const val DIRECTORY_NAME = "attachments"
    }
}
