package com.thelastecho.reminder.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.thelastecho.reminder.core.preferences.AppThemeSettings
import com.thelastecho.reminder.core.preferences.ThemeMode
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.data.local.ReminderDatabase
import com.thelastecho.reminder.data.local.dao.CategoryDao
import com.thelastecho.reminder.data.local.dao.ReminderDao
import com.thelastecho.reminder.data.local.entity.CategoryEntity
import com.thelastecho.reminder.data.local.entity.ReminderEntity
import com.thelastecho.reminder.data.local.entity.SubTaskEntity
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

class ReminderBackupRepositoryTest {

    private lateinit var fixture: TestFixture

    @Before
    fun setUp() {
        fixture = TestFixture()
    }

    @After
    fun tearDown() {
        if (::fixture.isInitialized) fixture.tempDirectory.deleteRecursively()
        unmockkAll()
    }

    @Test
    fun exportWritesVersionedJsonAndReminderData() = runTest {
        coEvery { fixture.reminderDao.getAllReminderEntitiesForBackup() } returns listOf(
            ReminderEntity(id = 42, title = "Take medicine", notes = "After lunch")
        )

        fixture.repository.export(fixture.backupUri)

        val root = JSONObject(fixture.output.toString(StandardCharsets.UTF_8.name()))
        assertEquals(ReminderBackupRepository.FORMAT, root.getString("format"))
        assertEquals(ReminderBackupRepository.VERSION, root.getInt("version"))
        assertEquals("Take medicine", root.getJSONArray("reminders").getJSONObject(0).getString("title"))
        assertEquals("After lunch", root.getJSONArray("reminders").getJSONObject(0).getString("notes"))
        assertEquals(0, root.getJSONObject("attachments").length())
    }

    @Test
    fun exportRejectsAnAttachmentLargerThan20MiB() = runTest {
        coEvery { fixture.reminderDao.getAllReminderEntitiesForBackup() } returns listOf(
            ReminderEntity(id = 1, title = "Photo", imageUri = "content://photos/1")
        )
        fixture.useImageStreams(MAX_ATTACHMENT_BYTES + 1)

        val failure = runCatching { fixture.repository.export(fixture.backupUri) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals("An attachment is larger than 20 MB.", failure?.message)
    }

    @Test
    fun exportRejectsAttachmentsOver35MiBInTotal() = runTest {
        coEvery { fixture.reminderDao.getAllReminderEntitiesForBackup() } returns listOf(
            ReminderEntity(id = 1, title = "Photo 1", imageUri = "content://photos/1"),
            ReminderEntity(id = 2, title = "Photo 2", imageUri = "content://photos/2")
        )
        fixture.useImageStreams(18 * MEBIBYTE, 18 * MEBIBYTE)

        val failure = runCatching { fixture.repository.export(fixture.backupUri) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals("Attachments exceed the backup size limit.", failure?.message)
    }

    @Test
    fun exportRejectsJsonLargerThan50MiB() = runTest {
        coEvery { fixture.reminderDao.getAllReminderEntitiesForBackup() } returns listOf(
            ReminderEntity(id = 1, title = "Large note", notes = "x".repeat(MAX_BACKUP_BYTES + 1))
        )

        val failure = runCatching { fixture.repository.export(fixture.backupUri) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals("Backup exceeds the file size limit.", failure?.message)
    }

    @Test
    fun restoreRejectsUnknownFormatBeforeWriting() = runTest {
        val failure = rejectInvalidBackup(backupJson(format = "not-a-reminder-backup"))

        assertTrue(failure is IllegalArgumentException)
        assertEquals("This is not a Reminder backup.", failure.message)
    }

    @Test
    fun restoreRejectsUnsupportedVersionBeforeWriting() = runTest {
        val failure = rejectInvalidBackup(backupJson(version = ReminderBackupRepository.VERSION + 1))

        assertTrue(failure is IllegalArgumentException)
        assertEquals("This backup version is not supported.", failure.message)
    }

    @Test
    fun restoreRejectsNonPositiveReminderId() = runTest {
        val failure = rejectInvalidBackup(backupJson(reminders = listOf(ReminderEntity(id = 0, title = "Invalid"))))

        assertTrue(failure is IllegalArgumentException)
        assertEquals("The backup contains an invalid reminder.", failure.message)
    }

    @Test
    fun restoreRejectsNonPositiveCategoryId() = runTest {
        val failure = rejectInvalidBackup(backupJson(categories = listOf(CategoryEntity(id = 0, name = "Invalid", colorArgb = 0, iconName = "tag"))))

        assertTrue(failure is IllegalArgumentException)
        assertEquals("The backup contains an invalid category.", failure.message)
    }

    @Test
    fun restoreRejectsNonPositiveSubtaskId() = runTest {
        val failure = rejectInvalidBackup(
            backupJson(
                reminders = listOf(ReminderEntity(id = 1, title = "Reminder")),
                subtasks = listOf(SubTaskEntity(id = 0, reminderId = 1, title = "Invalid", isCompleted = false, orderIndex = 0))
            )
        )

        assertTrue(failure is IllegalArgumentException)
        assertEquals("The backup contains an invalid subtask.", failure.message)
    }

    @Test
    fun restoreRejectsDuplicateReminderIds() = runTest {
        val failure = rejectInvalidBackup(
            backupJson(reminders = listOf(ReminderEntity(id = 1, title = "One"), ReminderEntity(id = 1, title = "Two")))
        )

        assertTrue(failure is IllegalArgumentException)
        assertEquals("The backup has duplicate reminder IDs.", failure.message)
    }

    @Test
    fun restoreRejectsDuplicateCategoryIds() = runTest {
        val categories = listOf(
            CategoryEntity(id = 1, name = "One", colorArgb = 0, iconName = "tag"),
            CategoryEntity(id = 1, name = "Two", colorArgb = 0, iconName = "tag")
        )
        val failure = rejectInvalidBackup(backupJson(categories = categories))

        assertTrue(failure is IllegalArgumentException)
        assertEquals("The backup has duplicate category IDs.", failure.message)
    }

    @Test
    fun restoreRejectsDuplicateSubtaskIds() = runTest {
        val reminders = listOf(ReminderEntity(id = 1, title = "Reminder"))
        val subtasks = listOf(
            SubTaskEntity(id = 3, reminderId = 1, title = "One", isCompleted = false, orderIndex = 0),
            SubTaskEntity(id = 3, reminderId = 1, title = "Two", isCompleted = false, orderIndex = 1)
        )
        val failure = rejectInvalidBackup(backupJson(reminders = reminders, subtasks = subtasks))

        assertTrue(failure is IllegalArgumentException)
        assertEquals("The backup has duplicate subtask IDs.", failure.message)
    }

    @Test
    fun restoreRejectsOrphanSubtasks() = runTest {
        val failure = rejectInvalidBackup(
            backupJson(subtasks = listOf(SubTaskEntity(id = 2, reminderId = 99, title = "Orphan", isCompleted = false, orderIndex = 0)))
        )

        assertTrue(failure is IllegalArgumentException)
        assertEquals("The backup has a subtask without its reminder.", failure.message)
    }

    @Test
    fun restoreRejectsReminderWithCategoryMissingFromBackup() = runTest {
        val failure = rejectInvalidBackup(
            backupJson(reminders = listOf(ReminderEntity(id = 1, title = "Reminder", categoryId = 99)))
        )

        assertTrue(failure is IllegalArgumentException)
        assertEquals("A reminder refers to a category missing from the backup.", failure.message)
    }

    @Test
    fun restoreMergeAddsNewRowsSkipsDuplicateRemindersAndReportsConflicts() = runTest {
        val existingCategory = CategoryEntity(id = 10, name = "Existing", colorArgb = 0, iconName = "tag")
        val newCategory = CategoryEntity(id = 11, name = "New", colorArgb = 1, iconName = "work")
        val existingReminder = ReminderEntity(id = 1, title = "Already here")
        coEvery { fixture.categoryDao.getAllCategoryEntitiesForBackup() } returns listOf(existingCategory)
        coEvery { fixture.reminderDao.getAllReminderEntitiesForBackup() } returns listOf(existingReminder)
        coEvery { fixture.reminderDao.getAllSubTaskEntitiesForBackup() } returns listOf(
            SubTaskEntity(id = 90, reminderId = 1, title = "Existing subtask", isCompleted = false, orderIndex = 0)
        )
        fixture.setInput(
            backupJson(
                categories = listOf(existingCategory, newCategory),
                reminders = listOf(
                    existingReminder,
                    ReminderEntity(id = 2, title = "New reminder", categoryId = 10),
                    ReminderEntity(id = 3, title = "Another reminder", categoryId = 11)
                ),
                subtasks = listOf(
                    SubTaskEntity(id = 90, reminderId = 2, title = "Collision", isCompleted = false, orderIndex = 0),
                    SubTaskEntity(id = 91, reminderId = 3, title = "New subtask", isCompleted = false, orderIndex = 0)
                )
            )
        )

        val report = fixture.repository.restore(fixture.backupUri, ReminderBackupRepository.RestoreMode.MERGE)

        assertEquals(ReminderBackupRepository.RestoreReport(importedReminders = 2, skippedReminders = 1, conflicts = 3), report)
        coVerify(exactly = 0) { fixture.reminderDao.clearReminderDataForBackup() }
        coVerify(exactly = 0) { fixture.categoryDao.clearCategoriesForBackup() }
        coVerify { fixture.categoryDao.insertCategoryEntitiesForBackup(match { it.map(CategoryEntity::id) == listOf(11L) }) }
        coVerify { fixture.reminderDao.insertReminderEntitiesForBackup(match { it.map(ReminderEntity::id) == listOf(2L, 3L) }) }
        coVerify {
            fixture.reminderDao.insertSubTaskEntitiesForBackup(
                match { tasks -> tasks.map(SubTaskEntity::id) == listOf(0L, 91L) }
            )
        }
    }

    @Test
    fun restoreReplaceClearsOldRowsAndRestoresBackupRowsAndPreferences() = runTest {
        coEvery { fixture.reminderDao.getAllReminderEntitiesForBackup() } returns listOf(
            ReminderEntity(id = 99, title = "Old reminder")
        )
        fixture.setInput(
            backupJson(
                reminders = listOf(ReminderEntity(id = 1, title = "Restored")),
                categories = listOf(CategoryEntity(id = 2, name = "Restored category", colorArgb = 3, iconName = "work")),
                subtasks = listOf(SubTaskEntity(id = 4, reminderId = 1, title = "Step", isCompleted = false, orderIndex = 0)),
                themeMode = ThemeMode.LIGHT.name
            )
        )

        val report = fixture.repository.restore(fixture.backupUri, ReminderBackupRepository.RestoreMode.REPLACE)

        assertEquals(ReminderBackupRepository.RestoreReport(importedReminders = 1, skippedReminders = 0, conflicts = 0), report)
        coVerify(exactly = 1) { fixture.reminderDao.clearReminderDataForBackup() }
        coVerify(exactly = 1) { fixture.categoryDao.clearCategoriesForBackup() }
        coVerify { fixture.categoryDao.insertCategoryEntitiesForBackup(match { it.map(CategoryEntity::id) == listOf(2L) }) }
        coVerify { fixture.reminderDao.insertReminderEntitiesForBackup(match { it.map(ReminderEntity::id) == listOf(1L) }) }
        coVerify { fixture.reminderDao.insertSubTaskEntitiesForBackup(match { it.map(SubTaskEntity::id) == listOf(4L) }) }
        coVerify(exactly = 1) { fixture.preferences.restoreReminderPreferences(match { it.themeMode == ThemeMode.LIGHT }) }
    }

    @Test
    fun restoreRejectsCorruptedJsonWithoutChangingData() = runTest {
        val failure = rejectInvalidBackup("{not valid json")

        assertTrue(failure is JSONException)
    }

    private suspend fun rejectInvalidBackup(json: String): Throwable {
        fixture.setInput(json)
        val failure = runCatching {
            fixture.repository.restore(fixture.backupUri, ReminderBackupRepository.RestoreMode.MERGE)
        }.exceptionOrNull()
        assertNotNull("Expected the backup to be rejected", failure)
        assertNoDatabaseWrites()
        return requireNotNull(failure)
    }

    private fun assertNoDatabaseWrites() {
        coVerify(exactly = 0) { fixture.reminderDao.clearReminderDataForBackup() }
        coVerify(exactly = 0) { fixture.categoryDao.clearCategoriesForBackup() }
        coVerify(exactly = 0) { fixture.reminderDao.insertReminderEntitiesForBackup(any()) }
        coVerify(exactly = 0) { fixture.reminderDao.insertSubTaskEntitiesForBackup(any()) }
        coVerify(exactly = 0) { fixture.categoryDao.insertCategoryEntitiesForBackup(any()) }
    }

    private fun backupJson(
        reminders: List<ReminderEntity> = emptyList(),
        categories: List<CategoryEntity> = emptyList(),
        subtasks: List<SubTaskEntity> = emptyList(),
        format: String = ReminderBackupRepository.FORMAT,
        version: Int = ReminderBackupRepository.VERSION,
        themeMode: String = ThemeMode.SYSTEM.name
    ): String {
        val reminderArray = JSONArray().apply { reminders.forEach { put(it.toJson()) } }
        val categoryArray = JSONArray().apply { categories.forEach { put(it.toJson()) } }
        val subtaskArray = JSONArray().apply { subtasks.forEach { put(it.toJson()) } }
        val settings = JSONObject()
            .put("themeMode", themeMode)
            .put("accentColor", "VIOLET")
            .put("notificationStyle", "HEADS_UP")
        return JSONObject()
            .put("format", format)
            .put("version", version)
            .put("reminders", reminderArray)
            .put("categories", categoryArray)
            .put("subtasks", subtaskArray)
            .put("attachments", JSONObject())
            .put("preferences", settings)
            .toString()
    }

    private fun ReminderEntity.toJson() = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("notes", notes)
        .put("due", dueDateTimeEpochMillis ?: JSONObject.NULL)
        .put("completed", isCompleted)
        .put("priority", priorityLevel)
        .put("repeat", repeatIntervalId)
        .put("category", categoryId ?: JSONObject.NULL)
        .put("imageUri", imageUri ?: JSONObject.NULL)
        .put("style", notificationStyleId ?: JSONObject.NULL)
        .put("created", createdAt)
        .put("completedAt", completedAt ?: JSONObject.NULL)
        .put("deleted", isDeleted)
        .put("deletedAt", deletedAt ?: JSONObject.NULL)
        .put("expiresAt", expiresAt ?: JSONObject.NULL)

    private fun CategoryEntity.toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("color", colorArgb)
        .put("icon", iconName)

    private fun SubTaskEntity.toJson() = JSONObject()
        .put("id", id)
        .put("reminderId", reminderId)
        .put("title", title)
        .put("completed", isCompleted)
        .put("order", orderIndex)

    private class TestFixture {
        val context = mockk<Context>()
        val contentResolver = mockk<ContentResolver>()
        val database = mockk<ReminderDatabase>()
        val reminderDao = mockk<ReminderDao>(relaxed = true)
        val categoryDao = mockk<CategoryDao>(relaxed = true)
        val preferences = mockk<UserPreferencesRepository>(relaxed = true)
        val backupUri = mockk<Uri>()
        private val parsedUri = mockk<Uri>()
        private val internalFileUri = mockk<Uri>()
        val output = ByteArrayOutputStream()
        val tempDirectory = java.nio.file.Files.createTempDirectory("reminder-backup-test").toFile()
        private var inputBytes = byteArrayOf()
        var inputStreamProvider: () -> InputStream = { ByteArrayInputStream(inputBytes) }
        val repository = ReminderBackupRepository(context, database, preferences)

        init {
            mockkStatic(Uri::class)
            mockkStatic("androidx.room.RoomDatabaseKt")
            every { Uri.parse(any()) } returns parsedUri
            every { Uri.fromFile(any()) } returns internalFileUri
            every { internalFileUri.toString() } returns "file:///backup-attachment.img"
            every { context.contentResolver } returns contentResolver
            every { context.filesDir } returns tempDirectory
            every { database.reminderDao() } returns reminderDao
            every { database.categoryDao() } returns categoryDao
            every { contentResolver.openOutputStream(backupUri, "wt") } returns output
            every { contentResolver.openInputStream(any()) } answers { inputStreamProvider() }
            every { preferences.themeSettings } returns flowOf(AppThemeSettings())
            coEvery { reminderDao.getAllReminderEntitiesForBackup() } returns emptyList()
            coEvery { reminderDao.getAllSubTaskEntitiesForBackup() } returns emptyList()
            coEvery { categoryDao.getAllCategoryEntitiesForBackup() } returns emptyList()
            coEvery { database.withTransaction<Unit>(any()) } coAnswers { secondArg<suspend () -> Unit>().invoke() }
        }

        fun setInput(json: String) {
            inputBytes = json.toByteArray(StandardCharsets.UTF_8)
        }

        fun useImageStreams(vararg sizes: Int) {
            var streamCount = 0
            inputStreamProvider = {
                val imageIndex = (streamCount / 2).coerceAtMost(sizes.lastIndex)
                streamCount++
                RepeatingInputStream(sizes[imageIndex].toLong())
            }
        }
    }

    private class RepeatingInputStream(private var remaining: Long) : InputStream() {
        override fun read(): Int {
            if (remaining == 0L) return -1
            remaining--
            return 0
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (remaining == 0L) return -1
            if (length == 0) return 0
            val count = minOf(remaining, length.toLong()).toInt()
            buffer.fill(0, offset, offset + count)
            remaining -= count
            return count
        }
    }

    private companion object {
        const val MEBIBYTE = 1024 * 1024
        const val MAX_ATTACHMENT_BYTES = 20 * MEBIBYTE
        const val MAX_BACKUP_BYTES = 50 * MEBIBYTE
    }
}