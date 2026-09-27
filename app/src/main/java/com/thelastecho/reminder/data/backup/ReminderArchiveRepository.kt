package com.thelastecho.reminder.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.thelastecho.reminder.BuildConfig
import com.thelastecho.reminder.core.alarm.ImportedReminderAlarmScheduler
import com.thelastecho.reminder.core.preferences.AccentColor
import com.thelastecho.reminder.core.preferences.AppThemeSettings
import com.thelastecho.reminder.core.preferences.DarkThemeConfig
import com.thelastecho.reminder.core.preferences.NotificationStyle
import com.thelastecho.reminder.core.preferences.ThemeMode
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.core.preferences.migrateThemeMode
import com.thelastecho.reminder.data.attachments.AttachmentStore
import com.thelastecho.reminder.data.local.ReminderDatabase
import com.thelastecho.reminder.data.local.entity.CategoryEntity
import com.thelastecho.reminder.data.local.entity.ReminderEntity
import com.thelastecho.reminder.data.local.entity.SubTaskEntity
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.EOFException
import java.util.zip.ZipException
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class UnsupportedBackupFormatException : IOException("This backup format version is not supported.")
class InvalidReminderBackupException(message: String) : IOException(message)
class BackupPasswordRequiredException : IOException("Enter the backup password to restore this file.")

class ReminderArchiveRepository(
    private val context: Context,
    private val database: ReminderDatabase,
    private val preferences: UserPreferencesRepository,
    private val alarmScheduler: ImportedReminderAlarmScheduler,
    private val attachments: AttachmentStore = AttachmentStore(context)
) {
    enum class RestoreMode { MERGE, REPLACE }

    data class RestoreReport(
        val importedReminders: Int,
        val skippedReminders: Int,
        val conflicts: Int,
        val alarmFailureReminderIds: Set<Long>,
        val alarmSoundFallback: Boolean
    )

    suspend fun export(output: OutputStream, password: CharArray? = null) {
        require(password == null || password.isNotEmpty()) { "The backup password must not be empty." }
        val reminders = database.reminderDao().getAllReminderEntitiesForBackup()
        val categories = database.categoryDao().getAllCategoryEntitiesForBackup()
        val subtasks = database.reminderDao().getAllSubTaskEntitiesForBackup()
        val settings = preferences.themeSettings.first()
        val attachmentIds = reminders.associate { entity ->
            entity.id to if (entity.imagePath != null || entity.imageUri != null) UUID.randomUUID().toString() else null
        }
        val reminderRows = JSONArray().apply {
            reminders.forEach { entity -> put(entity.toArchiveJson(attachmentIds[entity.id])) }
        }
        val databaseJson = JSONObject()
            .put("reminders", reminderRows)
            .put("categories", JSONArray().apply { categories.forEach { put(it.toArchiveJson()) } })
            .put("subtasks", JSONArray().apply { subtasks.forEach { put(it.toArchiveJson()) } })
            .put("preferences", settings.toArchiveJson())
            .toString().toByteArray(Charsets.UTF_8)
        require(databaseJson.size <= MAX_ARCHIVE_BYTES) { "The backup exceeds the supported size limit." }

        val zipFile = File.createTempFile("reminder-backup-", ".zip", context.cacheDir)
        try {
            var totalAttachmentBytes = 0L
            ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
                val manifest = JSONObject()
                    .put("formatVersion", FORMAT_VERSION)
                    .put("appVersion", BuildConfig.VERSION_NAME)
                    .put("createdAt", System.currentTimeMillis())
                    .put("databaseVersion", DATABASE_VERSION)
                    .put("encrypted", password != null)
                    .put("attachmentsIncluded", attachmentIds.values.any { it != null })
                zip.writeEntry("manifest.json", manifest.toString().toByteArray(Charsets.UTF_8))
                zip.writeEntry("database.json", databaseJson)
                reminders.forEach { entity ->
                    val attachmentId = attachmentIds[entity.id] ?: return@forEach
                    val input = attachments.open(entity.imagePath, entity.imageUri)
                        ?: throw IOException("A reminder photo could not be read for backup.")
                    zip.putNextEntry(ZipEntry("attachments/$attachmentId.bin"))
                    input.use { source ->
                        val buffer = ByteArray(8192)
                        var attachmentBytes = 0L
                        var count: Int
                        while (source.read(buffer).also { count = it } != -1) {
                            attachmentBytes += count
                            totalAttachmentBytes += count
                            require(attachmentBytes <= MAX_ATTACHMENT_BYTES) { "A photo exceeds the 20 MB limit." }
                            require(totalAttachmentBytes <= MAX_TOTAL_ATTACHMENT_BYTES) { "Photos exceed the 35 MB total limit." }
                            require(databaseJson.size + totalAttachmentBytes <= MAX_ARCHIVE_BYTES) { "The backup exceeds the supported size limit." }
                            zip.write(buffer, 0, count)
                        }
                    }
                    zip.closeEntry()
                }
            }
            require(zipFile.length() <= MAX_ARCHIVE_BYTES) { "The backup exceeds the supported size limit." }
            if (password == null) FileInputStream(zipFile).use { it.copyTo(output) }
            else BackupEnvelope.encrypt(zipFile, output, password)
            output.flush()
        } finally {
            zipFile.delete()
        }
    }

    suspend fun restore(input: InputStream, password: CharArray?, mode: RestoreMode): RestoreReport {
        val archiveSource = readArchiveFile(input, password)
        val extracted = try {
            extractArchive(archiveSource.file, archiveSource.encrypted)
        } finally {
            archiveSource.file.delete()
        }
        val installedPaths = mutableListOf<String>()
        try {
            val parsed = parseAndValidate(extracted)
            val dao = database.reminderDao()
            val categoryDao = database.categoryDao()
            val oldReminders = dao.getAllReminderEntitiesForBackup()
            val oldCategories = categoryDao.getAllCategoryEntitiesForBackup()
            val oldSubtasks = dao.getAllSubTaskEntitiesForBackup()
            val oldPreferences = preferences.themeSettings.first()
            val existingReminderIds = oldReminders.map { it.id }.toSet()
            val existingCategoryIds = oldCategories.map { it.id }.toSet()
            val existingSubtaskIds = oldSubtasks.map { it.id }.toSet()

            val chosenReminders: List<ArchiveReminder>
            val chosenCategories: List<CategoryEntity>
            val chosenSubtasks: List<SubTaskEntity>
            var skipped = 0
            var conflicts = 0
            if (mode == RestoreMode.REPLACE) {
                chosenReminders = parsed.reminders
                chosenCategories = parsed.categories
                chosenSubtasks = parsed.subtasks
            } else {
                val categoryIds = existingCategoryIds + parsed.categories.map { it.id }
                chosenReminders = parsed.reminders.filter {
                    it.entity.id !in existingReminderIds && (it.entity.categoryId == null || it.entity.categoryId in categoryIds)
                }
                skipped = parsed.reminders.size - chosenReminders.size
                conflicts += parsed.categories.count { it.id in existingCategoryIds }
                conflicts += parsed.reminders.count { it.entity.id in existingReminderIds }
                conflicts += parsed.reminders.count {
                    it.entity.id !in existingReminderIds && it.entity.categoryId != null && it.entity.categoryId !in categoryIds
                }
                val chosenIds = chosenReminders.map { it.entity.id }.toSet()
                val candidateTasks = parsed.subtasks.filter { it.reminderId in chosenIds }
                conflicts += candidateTasks.count { it.id in existingSubtaskIds }
                chosenSubtasks = candidateTasks.map { if (it.id in existingSubtaskIds) it.copy(id = 0L) else it }
                chosenCategories = parsed.categories.filter { it.id !in existingCategoryIds }
            }

            val restoredReminders = chosenReminders.map { row ->
                val attachmentFile = row.attachmentId?.let { id ->
                    extracted.attachments[id] ?: throw InvalidReminderBackupException("A referenced photo is missing from the backup.")
                }
                if (attachmentFile == null) row.entity else {
                    val stored = FileInputStream(attachmentFile).use(attachments::copyFrom)
                    installedPaths += stored.relativePath
                    row.entity.copy(imagePath = stored.relativePath, imageUri = null)
                }
            }
            var soundFallback = false
            val restoredPreferences = if (mode == RestoreMode.REPLACE) {
                parsed.preferences.copy(alarmSoundUri = parsed.preferences.alarmSoundUri?.takeIf(::canOpenUri).also {
                    if (parsed.preferences.alarmSoundUri != null && it == null) soundFallback = true
                })
            } else null

            var databaseCommitted = false
            try {
                database.withTransaction {
                    if (mode == RestoreMode.REPLACE) {
                        dao.clearReminderDataForBackup()
                        categoryDao.clearCategoriesForBackup()
                        categoryDao.insertCategoryEntitiesForBackup(chosenCategories)
                        dao.insertReminderEntitiesForBackup(restoredReminders)
                        dao.insertSubTaskEntitiesForBackup(chosenSubtasks)
                    } else {
                        categoryDao.insertCategoryEntitiesForBackup(chosenCategories)
                        dao.insertReminderEntitiesForBackup(restoredReminders)
                        dao.insertSubTaskEntitiesForBackup(chosenSubtasks)
                    }
                }
                databaseCommitted = true
                if (restoredPreferences != null) preferences.restoreReminderPreferences(restoredPreferences)
            } catch (error: Throwable) {
                if (databaseCommitted) {
                    runCatching { restoreDatabaseSnapshot(oldCategories, oldReminders, oldSubtasks) }
                        .exceptionOrNull()?.let(error::addSuppressed)
                }
                if (mode == RestoreMode.REPLACE) {
                    runCatching { preferences.restoreReminderPreferences(oldPreferences) }
                        .exceptionOrNull()?.let(error::addSuppressed)
                }
                throw IOException("The backup could not be committed; the previous data was restored.", error)
            }

            val importedModels = restoredReminders.map { it.toDomain() }
            val alarmResult = alarmScheduler.apply(
                importedModels,
                replacedReminderIds = if (mode == RestoreMode.REPLACE) oldReminders.map { it.id }.toSet() else emptySet()
            )
            val retainedPaths = restoredReminders.mapNotNull { it.imagePath }.toSet()
            if (mode == RestoreMode.REPLACE) {
                oldReminders.mapNotNull { it.imagePath }.filter { it !in retainedPaths }.forEach(attachments::delete)
            }
            return RestoreReport(
                importedReminders = restoredReminders.size,
                skippedReminders = skipped,
                conflicts = conflicts,
                alarmFailureReminderIds = alarmResult.failedReminderIds,
                alarmSoundFallback = soundFallback
            )
        } catch (error: Throwable) {
            installedPaths.forEach(attachments::delete)
            throw error
        } finally {
            extracted.directory.deleteRecursively()
        }
    }

    private suspend fun restoreDatabaseSnapshot(
        categories: List<CategoryEntity>,
        reminders: List<ReminderEntity>,
        subtasks: List<SubTaskEntity>
    ) {
        database.withTransaction {
            database.reminderDao().clearReminderDataForBackup()
            database.categoryDao().clearCategoriesForBackup()
            database.categoryDao().insertCategoryEntitiesForBackup(categories)
            database.reminderDao().insertReminderEntitiesForBackup(reminders)
            database.reminderDao().insertSubTaskEntitiesForBackup(subtasks)
        }
    }

    private fun canOpenUri(raw: String): Boolean = runCatching {
        context.contentResolver.openInputStream(Uri.parse(raw))?.use { }
            ?: return false
        true
    }.getOrDefault(false)

    private data class ArchiveSource(val file: File, val encrypted: Boolean)

    private fun readArchiveFile(input: InputStream, password: CharArray?): ArchiveSource {
        val buffered = BufferedInputStream(input)
        buffered.mark(BackupEnvelope.HEADER_BYTES)
        val prefix = ByteArray(4)
        val read = buffered.read(prefix)
        buffered.reset()
        val encrypted = read == prefix.size && BackupEnvelope.hasMagic(prefix)
        if (encrypted) {
            val secret = password ?: throw BackupPasswordRequiredException()
            return try {
                ArchiveSource(BackupEnvelope.decrypt(buffered, context.cacheDir, MAX_ARCHIVE_BYTES.toLong(), secret), true)
            } catch (_: EOFException) {
                throw InvalidReminderBackupException("The encrypted backup is truncated.")
            }
        }
        val file = File.createTempFile("reminder-restore-", ".zip", context.cacheDir)
        try {
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(8192)
                var total = 0L
                var count: Int
                while (buffered.read(buffer).also { count = it } != -1) {
                    total += count
                    if (total > MAX_ARCHIVE_BYTES) throw InvalidReminderBackupException("The backup exceeds the supported size limit.")
                    output.write(buffer, 0, count)
                }
            }
            return ArchiveSource(file, false)
        } catch (error: Throwable) {
            file.delete()
            throw error
        }
    }

    private data class ExtractedArchive(
        val directory: File,
        val manifest: JSONObject,
        val database: JSONObject,
        val attachments: Map<String, File>,
        val encrypted: Boolean
    )

    private fun extractArchive(file: File, encrypted: Boolean): ExtractedArchive {
        val directory = File(context.cacheDir, "restore-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            var totalBytes = 0L
            var totalAttachmentBytes = 0L
            var entryCount = 0
            val extracted = mutableMapOf<String, File>()
            ZipInputStream(FileInputStream(file)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entryCount++
                    if (entryCount > MAX_ENTRIES || entry.isDirectory) throw InvalidReminderBackupException("The backup contains unsupported archive entries.")
                    val isAttachment = ATTACHMENT_ENTRY.matches(entry.name)
                    if (entry.name != "manifest.json" && entry.name != "database.json" && !isAttachment) {
                        throw InvalidReminderBackupException("The backup contains an unsupported archive entry.")
                    }
                    if (entry.name in extracted) throw InvalidReminderBackupException("The backup contains duplicate archive entries.")
                    val destination = File(directory, entry.name).canonicalFile
                    if (!destination.path.startsWith(directory.canonicalPath + File.separator)) {
                        throw InvalidReminderBackupException("The backup contains an invalid archive path.")
                    }
                    destination.parentFile?.mkdirs()
                    var entryBytes = 0L
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(8192)
                        var count: Int
                        while (zip.read(buffer).also { count = it } != -1) {
                            entryBytes += count
                            totalBytes += count
                            if (totalBytes > MAX_ARCHIVE_BYTES) throw InvalidReminderBackupException("The backup exceeds the supported size limit.")
                            if (isAttachment) {
                                totalAttachmentBytes += count
                                if (entryBytes > MAX_ATTACHMENT_BYTES || totalAttachmentBytes > MAX_TOTAL_ATTACHMENT_BYTES) {
                                    throw InvalidReminderBackupException("The backup photo size limits were exceeded.")
                                }
                            }
                            output.write(buffer, 0, count)
                        }
                    }
                    extracted[entry.name] = destination
                    zip.closeEntry()
                }
            }
            val manifestFile = extracted["manifest.json"] ?: throw InvalidReminderBackupException("The backup manifest is missing.")
            val databaseFile = extracted["database.json"] ?: throw InvalidReminderBackupException("The backup database is missing.")
            val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
            val databaseJson = JSONObject(databaseFile.readText(Charsets.UTF_8))
            val attachmentFiles = extracted.mapNotNull { (name, path) ->
                ATTACHMENT_ENTRY.matchEntire(name)?.groupValues?.get(1)?.let { it to path }
            }.toMap()
            return ExtractedArchive(directory, manifest, databaseJson, attachmentFiles, encrypted)
        } catch (error: Throwable) {
            directory.deleteRecursively()
            if (error is JSONException) throw InvalidReminderBackupException("The backup JSON is damaged.")
            if (error is ZipException || error is EOFException) throw InvalidReminderBackupException("The backup archive is damaged or truncated.")
            throw error
        }
    }

    private data class ArchiveReminder(val entity: ReminderEntity, val attachmentId: String?)
    private data class ParsedBackup(
        val reminders: List<ArchiveReminder>,
        val categories: List<CategoryEntity>,
        val subtasks: List<SubTaskEntity>,
        val preferences: AppThemeSettings
    )

    private fun parseAndValidate(archive: ExtractedArchive): ParsedBackup {
        val manifest = archive.manifest
        if (!manifest.has("formatVersion") || !manifest.has("appVersion") || !manifest.has("createdAt") ||
            !manifest.has("databaseVersion") || !manifest.has("encrypted") || !manifest.has("attachmentsIncluded")
        ) throw InvalidReminderBackupException("The backup manifest is incomplete.")
        if (manifest.getString("appVersion").isBlank() || manifest.getLong("createdAt") <= 0L) {
            throw InvalidReminderBackupException("The backup manifest contains invalid metadata.")
        }
        if (manifest.optInt("formatVersion", -1) != FORMAT_VERSION) throw UnsupportedBackupFormatException()
        if (manifest.optInt("databaseVersion", -1) !in 1..DATABASE_VERSION) throw UnsupportedBackupFormatException()
        if (manifest.optBoolean("encrypted") != archive.encrypted) {
            throw InvalidReminderBackupException("The manifest does not match the archive encryption state.")
        }
        val categories = archive.database.getJSONArray("categories").toEntities { row ->
            CategoryEntity(row.getLong("id"), row.getString("name"), row.getLong("color"), row.getString("icon"))
        }
        val reminders = archive.database.getJSONArray("reminders").toEntities { row ->
            ArchiveReminder(row.toReminderEntity(), row.nullableString("imageAttachmentId"))
        }
        val subtasks = archive.database.getJSONArray("subtasks").toEntities { row ->
            SubTaskEntity(row.getLong("id"), row.getLong("reminderId"), row.getString("title"), row.optBoolean("completed"), row.optInt("order"))
        }
        val categoryIds = categories.map { it.id }.toSet()
        if (categories.any { it.id <= 0 || it.name.isBlank() }) throw InvalidReminderBackupException("The backup contains an invalid category.")
        if (reminders.any { it.entity.id <= 0 || it.entity.title.isBlank() }) throw InvalidReminderBackupException("The backup contains an invalid reminder.")
        if (subtasks.any { it.id <= 0 }) throw InvalidReminderBackupException("The backup contains an invalid subtask.")
        if (reminders.map { it.entity.id }.distinct().size != reminders.size) throw InvalidReminderBackupException("The backup has duplicate reminder IDs.")
        if (categories.map { it.id }.distinct().size != categories.size) throw InvalidReminderBackupException("The backup has duplicate category IDs.")
        if (subtasks.map { it.id }.distinct().size != subtasks.size) throw InvalidReminderBackupException("The backup has duplicate subtask IDs.")
        if (subtasks.any { task -> reminders.none { it.entity.id == task.reminderId } }) throw InvalidReminderBackupException("A subtask has no reminder in the backup.")
        if (reminders.any { it.entity.categoryId != null && it.entity.categoryId !in categoryIds }) throw InvalidReminderBackupException("A reminder refers to a missing category.")
        val referencedAttachmentIds = reminders.mapNotNull { it.attachmentId }.toSet()
        if (referencedAttachmentIds.any { it !in archive.attachments } || archive.attachments.keys.any { it !in referencedAttachmentIds }) {
            throw InvalidReminderBackupException("The backup photo references are incomplete or inconsistent.")
        }
        if (manifest.optBoolean("attachmentsIncluded") != archive.attachments.isNotEmpty()) {
            throw InvalidReminderBackupException("The backup attachment manifest is inconsistent.")
        }
        val preferences = archive.database.getJSONObject("preferences").toAppThemeSettings()
        return ParsedBackup(reminders, categories, subtasks, preferences)
    }

    private fun ReminderEntity.toArchiveJson(attachmentId: String?) = JSONObject().apply {
        put("id", id); put("title", title); put("notes", notes)
        put("due", dueDateTimeEpochMillis ?: JSONObject.NULL); put("completed", isCompleted)
        put("priority", priorityLevel); put("repeat", repeatIntervalId)
        put("category", categoryId ?: JSONObject.NULL); put("style", notificationStyleId ?: JSONObject.NULL)
        put("created", createdAt); put("completedAt", completedAt ?: JSONObject.NULL)
        put("deleted", isDeleted); put("deletedAt", deletedAt ?: JSONObject.NULL); put("expiresAt", expiresAt ?: JSONObject.NULL)
        if (attachmentId != null) put("imageAttachmentId", attachmentId)
    }

    private fun CategoryEntity.toArchiveJson() = JSONObject()
        .put("id", id).put("name", name).put("color", colorArgb).put("icon", iconName)

    private fun SubTaskEntity.toArchiveJson() = JSONObject()
        .put("id", id).put("reminderId", reminderId).put("title", title).put("completed", isCompleted).put("order", orderIndex)

    private fun AppThemeSettings.toArchiveJson() = JSONObject()
        .put("themeMode", themeMode.name).put("accentColor", accentColor.name)
        .put("customAccentColor", customAccentColor ?: JSONObject.NULL).put("useCustomAccent", useCustomAccent)
        .put("widgetBackgroundOpacity", widgetBackgroundOpacity)
        .put("dynamic", useDynamicColors).put("notificationStyle", notificationStyle.name)
        .put("alarmSound", alarmSoundUri ?: JSONObject.NULL)
        .put("completedRetentionDays", completedReminderRetentionDays).put("addButtonOnLeft", addButtonOnLeft)

    private fun JSONObject.toReminderEntity() = ReminderEntity(
        id = getLong("id"),
        title = getString("title"),
        notes = optString("notes"),
        dueDateTimeEpochMillis = nullableLong("due"),
        isCompleted = optBoolean("completed"),
        priorityLevel = optInt("priority"),
        repeatIntervalId = optString("repeat", "ONCE"),
        categoryId = nullableLong("category"),
        imageUri = null,
        notificationStyleId = nullableString("style"),
        createdAt = optLong("created"),
        completedAt = nullableLong("completedAt"),
        isDeleted = optBoolean("deleted"),
        deletedAt = nullableLong("deletedAt"),
        expiresAt = nullableLong("expiresAt")
    )

    private fun JSONObject.toAppThemeSettings() = AppThemeSettings(
        themeMode = migrateThemeMode(
            optString("themeMode").takeIf(String::isNotBlank),
            optString("theme").takeIf(String::isNotBlank),
            optBoolean("amoled", true)
        ),
        accentColor = runCatching { AccentColor.valueOf(optString("accentColor", AccentColor.VIOLET.name)) }.getOrDefault(AccentColor.VIOLET),
        useDynamicColors = optBoolean("dynamic", true),
        customAccentColor = if (isNull("customAccentColor")) null else optInt("customAccentColor"),
        useCustomAccent = optBoolean("useCustomAccent", false),
        widgetBackgroundOpacity = optInt("widgetBackgroundOpacity", 100).coerceIn(0, 100),
        notificationStyle = runCatching { NotificationStyle.valueOf(optString("notificationStyle", NotificationStyle.HEADS_UP.name)) }.getOrDefault(NotificationStyle.HEADS_UP),
        alarmSoundUri = nullableString("alarmSound"),
        completedReminderRetentionDays = optInt("completedRetentionDays").coerceIn(0, 3650),
        addButtonOnLeft = optBoolean("addButtonOnLeft")
    )

    private fun JSONObject.nullableLong(key: String): Long? = if (isNull(key)) null else getLong(key)
    private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)
    private fun <T> JSONArray.toEntities(parse: (JSONObject) -> T): List<T> = (0 until length()).map { parse(getJSONObject(it)) }

    private fun ZipOutputStream.writeEntry(name: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(bytes)
        closeEntry()
    }

    companion object {
        const val FORMAT_VERSION = 1
        const val DATABASE_VERSION = 5
        private const val MAX_ARCHIVE_BYTES = 50 * 1024 * 1024
        private const val MAX_ATTACHMENT_BYTES = 20 * 1024 * 1024
        private const val MAX_TOTAL_ATTACHMENT_BYTES = 35 * 1024 * 1024
        private const val MAX_ENTRIES = 10_002
        private val ATTACHMENT_ENTRY = Regex("attachments/([a-f0-9-]{36})\\.bin")
    }
}
