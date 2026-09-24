package com.thelastecho.reminder.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.thelastecho.reminder.core.preferences.AppThemeSettings
import com.thelastecho.reminder.core.preferences.DarkThemeConfig
import com.thelastecho.reminder.core.preferences.NotificationStyle
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.data.local.ReminderDatabase
import com.thelastecho.reminder.data.local.entity.CategoryEntity
import com.thelastecho.reminder.data.local.entity.ReminderEntity
import com.thelastecho.reminder.data.local.entity.SubTaskEntity
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.UUID

/** Portable JSON backup format. Version 1 stores stable Room IDs and embeds photo bytes. */
class ReminderBackupRepository(
    private val context: Context,
    private val database: ReminderDatabase,
    private val preferences: UserPreferencesRepository
) {
    enum class RestoreMode { MERGE, REPLACE }
    data class RestoreReport(val importedReminders: Int, val skippedReminders: Int, val conflicts: Int)

    suspend fun export(uri: Uri) {
        val reminders = database.reminderDao().getAllReminderEntitiesForBackup()
        val categories = database.categoryDao().getAllCategoryEntitiesForBackup()
        val subtasks = database.reminderDao().getAllSubTaskEntitiesForBackup()
        val attachments = JSONObject()
        var totalAttachmentBytes = 0
        val exported = JSONArray()
        reminders.forEach { entity ->
            val row = entity.toJson()
            val imageUri = entity.imageUri
            if (!imageUri.isNullOrBlank()) {
                val bytes = context.contentResolver.openInputStream(Uri.parse(imageUri))?.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var count: Int
                    var total = 0
                    while (input.read(buffer).also { count = it } >= 0) {
                        total += count
                        require(total <= MAX_ATTACHMENT_BYTES) { "An attachment is larger than 20 MB." }
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                } ?: throw IllegalStateException("Could not read a reminder photo.")
                totalAttachmentBytes += bytes.size
                require(totalAttachmentBytes <= MAX_TOTAL_ATTACHMENT_BYTES) { "Attachments exceed the backup size limit." }
                val key = UUID.randomUUID().toString()
                attachments.put(key, Base64.getEncoder().encodeToString(bytes))
                row.put("imageAttachment", key)
                row.remove("imageUri")
            }
            exported.put(row)
        }
        val categoryArray = JSONArray().apply { categories.forEach { put(it.toJson()) } }
        val subtaskArray = JSONArray().apply { subtasks.forEach { put(it.toJson()) } }
        val settings = preferences.themeSettings.first()
        val root = JSONObject().put("format", FORMAT).put("version", VERSION)
            .put("reminders", exported).put("categories", categoryArray)
            .put("subtasks", subtaskArray).put("attachments", attachments)
            .put("preferences", settings.toJson())
        val backupText = root.toString()
        require(backupText.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_BYTES) { "Backup exceeds the file size limit." }
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(backupText) }
            ?: throw IllegalStateException("Could not create the selected backup file.")
    }

    suspend fun restore(uri: Uri, mode: RestoreMode): RestoreReport {
        // Parse and validate everything before any current data can be changed.
        val raw = context.contentResolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            var n: Int
            while (input.read(buffer).also { n = it } >= 0) {
                total += n
                require(total <= MAX_BACKUP_BYTES) { "The backup is larger than 50 MB." }
                out.write(buffer, 0, n)
            }
            out.toString(Charsets.UTF_8.name())
        } ?: throw IllegalStateException("Could not read the selected backup file.")
        val root = JSONObject(raw)
        require(root.getString("format") == FORMAT) { "This is not a Reminder backup." }
        require(root.getInt("version") == VERSION) { "This backup version is not supported." }
        val categories = root.getJSONArray("categories").toEntities(::categoryFromJson)
        val reminders = root.getJSONArray("reminders").toEntities(::reminderFromJson)
        val subtasks = root.getJSONArray("subtasks").toEntities(::subtaskFromJson)
        val attachmentJson = root.optJSONObject("attachments") ?: JSONObject()
        val backedCategories = categories.map { it.id }.toSet()
        require(categories.all { it.id > 0 && it.name.isNotBlank() }) { "The backup contains an invalid category." }
        require(reminders.all { it.id > 0 && it.title.isNotBlank() }) { "The backup contains an invalid reminder." }
        require(reminders.map { it.id }.distinct().size == reminders.size) { "The backup has duplicate reminder IDs." }
        require(categories.map { it.id }.distinct().size == categories.size) { "The backup has duplicate category IDs." }
        require(subtasks.all { task -> reminders.any { it.id == task.reminderId } }) { "The backup has a subtask without its reminder." }
        require(subtasks.map { it.id }.distinct().size == subtasks.size) { "The backup has duplicate subtask IDs." }
        require(reminders.none { !it.imageUri.isNullOrBlank() }) { "The backup refers to a photo URI from another device instead of an embedded attachment." }
        require(reminders.all { it.categoryId == null || it.categoryId in backedCategories }) { "A reminder refers to a category missing from the backup." }
        val settings = root.getJSONObject("preferences").toThemeSettings()
        val previousSettings = preferences.themeSettings.first()
        val previousImageUris = database.reminderDao().getAllReminderEntitiesForBackup().mapNotNull { it.imageUri }.toSet()
        val newFiles = mutableListOf<java.io.File>()
        val remappedReminders = try { reminders.map { entity ->
            val key = root.getJSONArray("reminders").let { array ->
                (0 until array.length()).firstNotNullOfOrNull { i -> array.getJSONObject(i).takeIf { it.getLong("id") == entity.id }?.optString("imageAttachment") }
            }
            if (key.isNullOrBlank()) entity else {
                val encoded = attachmentJson.optString(key)
                require(encoded.isNotBlank()) { "A reminder photo is missing from the backup." }
                val bytes = Base64.getDecoder().decode(encoded)
                require(bytes.size <= MAX_ATTACHMENT_BYTES) { "A reminder photo exceeds the size limit." }
                val directory = java.io.File(context.filesDir, "attachments").apply { mkdirs() }
                val file = java.io.File(directory, "${UUID.randomUUID()}.img")
                file.writeBytes(bytes)
                newFiles += file
                entity.copy(imageUri = Uri.fromFile(file).toString())
            }
        } } catch (error: Throwable) {
            newFiles.forEach { it.delete() }
            throw error
        }
        var importedReminderIds = emptySet<Long>()
        try {
            var imported = 0
            var skipped = 0
            var conflicts = 0
            if (mode == RestoreMode.REPLACE) preferences.restoreReminderPreferences(settings)
            database.withTransaction {
                val reminderDao = database.reminderDao()
                val categoryDao = database.categoryDao()
                if (mode == RestoreMode.REPLACE) {
                    reminderDao.clearReminderDataForBackup()
                    categoryDao.clearCategoriesForBackup()
                    categoryDao.insertCategoryEntitiesForBackup(categories)
                    reminderDao.insertReminderEntitiesForBackup(remappedReminders)
                    reminderDao.insertSubTaskEntitiesForBackup(subtasks)
                    imported = remappedReminders.size
                    importedReminderIds = remappedReminders.map { it.id }.toSet()
                } else {
                    val existingCategories = categoryDao.getAllCategoryEntitiesForBackup().map { it.id }.toSet()
                    val existingReminders = reminderDao.getAllReminderEntitiesForBackup().map { it.id }.toSet()
                    val categoryIds = (existingCategories + categories.map { it.id }).toSet()
                    val additions = remappedReminders.filter { it.id !in existingReminders && (it.categoryId == null || it.categoryId in categoryIds) }
                    skipped = reminders.size - additions.size
                    val duplicateReminderIds = reminders.count { it.id in existingReminders }
                    val unsafeRelations = reminders.count { it.id !in existingReminders && it.categoryId != null && it.categoryId !in categoryIds }
                    conflicts = categories.count { it.id in existingCategories } + duplicateReminderIds + unsafeRelations
                    categoryDao.insertCategoryEntitiesForBackup(categories.filter { it.id !in existingCategories })
                    reminderDao.insertReminderEntitiesForBackup(additions)
                    val addedIds = additions.map { it.id }.toSet()
                    val existingSubtaskIds = reminderDao.getAllSubTaskEntitiesForBackup().map { it.id }.toSet()
                    val additionsSubtasks = subtasks.filter { it.reminderId in addedIds }
                    val collidingSubtaskIds = additionsSubtasks.count { it.id in existingSubtaskIds }
                    conflicts += collidingSubtaskIds
                    reminderDao.insertSubTaskEntitiesForBackup(additionsSubtasks.map { if (it.id in existingSubtaskIds) it.copy(id = 0L) else it })
                    imported = additions.size
                    importedReminderIds = additions.map { it.id }.toSet()
                }
            }
            val referencedUris = remappedReminders.filter { it.id in importedReminderIds }.mapNotNull { it.imageUri }.toSet()
            newFiles.filter { Uri.fromFile(it).toString() !in referencedUris }.forEach { it.delete() }
            if (mode == RestoreMode.REPLACE) {
                previousImageUris.filter { it !in referencedUris }.forEach(::deleteInternalAttachment)
            }
            return RestoreReport(imported, skipped, conflicts)
        } catch (error: Throwable) {
            if (mode == RestoreMode.REPLACE) runCatching { preferences.restoreReminderPreferences(previousSettings) }
            newFiles.forEach { it.delete() }
            throw error
        }
    }

    private fun deleteInternalAttachment(rawUri: String) {
        runCatching {
            val uri = Uri.parse(rawUri)
            if (uri.scheme == "file") {
                val file = java.io.File(uri.path.orEmpty()).canonicalFile
                val folder = java.io.File(context.filesDir, "attachments").canonicalFile
                if (file.parentFile == folder) file.delete()
            }
        }
    }

    private fun ReminderEntity.toJson() = JSONObject().apply {
        put("id", id); put("title", title); put("notes", notes); put("due", dueDateTimeEpochMillis ?: JSONObject.NULL)
        put("completed", isCompleted); put("priority", priorityLevel); put("repeat", repeatIntervalId)
        put("category", categoryId ?: JSONObject.NULL); put("imageUri", imageUri ?: JSONObject.NULL)
        put("style", notificationStyleId ?: JSONObject.NULL); put("created", createdAt); put("completedAt", completedAt ?: JSONObject.NULL)
        put("deleted", isDeleted); put("deletedAt", deletedAt ?: JSONObject.NULL); put("expiresAt", expiresAt ?: JSONObject.NULL)
    }
    private fun CategoryEntity.toJson() = JSONObject().put("id", id).put("name", name).put("color", colorArgb).put("icon", iconName)
    private fun SubTaskEntity.toJson() = JSONObject().put("id", id).put("reminderId", reminderId).put("title", title).put("completed", isCompleted).put("order", orderIndex)
    private fun AppThemeSettings.toJson() = JSONObject().put("theme", darkThemeConfig.name).put("amoled", isAmoledMode).put("dynamic", useDynamicColors).put("notificationStyle", notificationStyle.name).put("alarmSound", alarmSoundUri ?: JSONObject.NULL).put("completedRetentionDays", completedReminderRetentionDays).put("addButtonOnLeft", addButtonOnLeft)

    companion object {
        const val FORMAT = "the-last-echo-reminder-backup"
        const val VERSION = 1
        private const val MAX_BACKUP_BYTES = 50 * 1024 * 1024
        private const val MAX_ATTACHMENT_BYTES = 20 * 1024 * 1024
        private const val MAX_TOTAL_ATTACHMENT_BYTES = 35 * 1024 * 1024
        private fun <T> JSONArray.toEntities(parse: (JSONObject) -> T): List<T> = (0 until length()).map { parse(getJSONObject(it)) }
        private fun categoryFromJson(o: JSONObject) = CategoryEntity(o.getLong("id"), o.getString("name"), o.getLong("color"), o.getString("icon"))
        private fun reminderFromJson(o: JSONObject) = ReminderEntity(o.getLong("id"), o.getString("title"), o.optString("notes"), o.nullableLong("due"), o.optBoolean("completed"), o.optInt("priority"), o.optString("repeat", "ONCE"), o.nullableLong("category"), o.nullableString("imageUri"), o.nullableString("style"), o.optLong("created"), o.nullableLong("completedAt"), o.optBoolean("deleted"), o.nullableLong("deletedAt"), o.nullableLong("expiresAt"))
        private fun subtaskFromJson(o: JSONObject) = SubTaskEntity(o.getLong("id"), o.getLong("reminderId"), o.getString("title"), o.optBoolean("completed"), o.optInt("order"))
        private fun JSONObject.nullableLong(key: String): Long? = if (isNull(key)) null else getLong(key)
        private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)
        private fun JSONObject.toThemeSettings() = AppThemeSettings(
            darkThemeConfig = runCatching { DarkThemeConfig.valueOf(getString("theme")) }.getOrDefault(DarkThemeConfig.FOLLOW_SYSTEM),
            isAmoledMode = optBoolean("amoled"), useDynamicColors = optBoolean("dynamic"),
            notificationStyle = runCatching { NotificationStyle.valueOf(getString("notificationStyle")) }.getOrDefault(NotificationStyle.HEADS_UP),
            alarmSoundUri = nullableString("alarmSound"), completedReminderRetentionDays = optInt("completedRetentionDays"), addButtonOnLeft = optBoolean("addButtonOnLeft")
        )
    }
}
