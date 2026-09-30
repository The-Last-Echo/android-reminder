package com.thelastecho.reminder.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.thelastecho.reminder.core.alarm.AlarmScheduler
import com.thelastecho.reminder.core.alarm.ImportedReminderAlarmScheduler
import com.thelastecho.reminder.core.preferences.AppThemeSettings
import com.thelastecho.reminder.core.preferences.UserPreferencesRepository
import com.thelastecho.reminder.data.attachments.AttachmentStore
import com.thelastecho.reminder.data.local.ReminderDatabase
import com.thelastecho.reminder.data.local.dao.CategoryDao
import com.thelastecho.reminder.data.local.dao.ReminderDao
import com.thelastecho.reminder.data.local.entity.CategoryEntity
import com.thelastecho.reminder.data.local.entity.ReminderEntity
import com.thelastecho.reminder.data.local.entity.SubTaskEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

class ReminderArchiveRepositoryTest {
    private val context = mockk<Context>()
    private val database = mockk<ReminderDatabase>()
    private val reminderDao = mockk<ReminderDao>(relaxed = true)
    private val categoryDao = mockk<CategoryDao>(relaxed = true)
    private val preferences = mockk<UserPreferencesRepository>(relaxed = true)
    private val attachments = mockk<AttachmentStore>(relaxed = true)
    private val alarmScheduler = ImportedReminderAlarmScheduler(mockk<AlarmScheduler>(relaxed = true))
    private val tempDirectory = java.nio.file.Files.createTempDirectory("reminder-archive-test").toFile()
    private val repository = ReminderArchiveRepository(context, database, preferences, alarmScheduler, attachments)
    private val sourceReminder = ReminderEntity(
        id = 7,
        title = "Portable reminder",
        notes = "Body",
        repeatDurationId = "COUNT",
        repeatCount = 10,
        repeatCompletedCount = 9
    )

    @Before
    fun setUp() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        every { context.cacheDir } returns tempDirectory
        every { database.reminderDao() } returns reminderDao
        every { database.categoryDao() } returns categoryDao
        every { preferences.themeSettings } returns flowOf(AppThemeSettings())
        coEvery { reminderDao.getAllReminderEntitiesForBackup() } returns listOf(sourceReminder)
        coEvery { reminderDao.getAllSubTaskEntitiesForBackup() } returns listOf(
            SubTaskEntity(id = 3, reminderId = 7, title = "Subtask", isCompleted = true, orderIndex = 0)
        )
        coEvery { categoryDao.getAllCategoryEntitiesForBackup() } returns listOf(
            CategoryEntity(id = 2, name = "Home", colorArgb = 0xFF00FF00, iconName = "home")
        )
        coEvery { database.withTransaction<Unit>(any()) } coAnswers { secondArg<suspend () -> Unit>().invoke() }
    }

    @Test
    fun encryptedArchiveRestoresRowsAndReportsSuccessfulAlarmApplication() = runTest {
        val archive = ByteArrayOutputStream()
        repository.export(archive, "portable password".toCharArray())

        coEvery { reminderDao.getAllReminderEntitiesForBackup() } returns emptyList()
        coEvery { reminderDao.getAllSubTaskEntitiesForBackup() } returns emptyList()
        coEvery { categoryDao.getAllCategoryEntitiesForBackup() } returns emptyList()

        val report = repository.restore(
            ByteArrayInputStream(archive.toByteArray()),
            "portable password".toCharArray(),
            ReminderArchiveRepository.RestoreMode.REPLACE
        )

        assertEquals(1, report.importedReminders)
        assertEquals(0, report.skippedReminders)
        assertEquals(0, report.conflicts)
        assertTrue(report.alarmFailureReminderIds.isEmpty())
        coVerify {
            reminderDao.insertReminderEntitiesForBackup(
                match {
                    it.single().title == "Portable reminder" &&
                        it.single().repeatCount == 10 &&
                        it.single().repeatCompletedCount == 9
                }
            )
        }
        coVerify { reminderDao.insertSubTaskEntitiesForBackup(match { it.single().title == "Subtask" }) }
        coVerify { categoryDao.insertCategoryEntitiesForBackup(match { it.single().name == "Home" }) }
    }

    @Test
    fun unencryptedArchiveRestoresWithoutRequestingPassword() = runTest {
        val archive = ByteArrayOutputStream()
        repository.export(archive, null)
        assertTrue(!BackupEnvelope.isEncrypted(ByteArrayInputStream(archive.toByteArray())))

        coEvery { reminderDao.getAllReminderEntitiesForBackup() } returns emptyList()
        coEvery { reminderDao.getAllSubTaskEntitiesForBackup() } returns emptyList()
        coEvery { categoryDao.getAllCategoryEntitiesForBackup() } returns emptyList()

        val report = repository.restore(
            ByteArrayInputStream(archive.toByteArray()),
            null,
            ReminderArchiveRepository.RestoreMode.REPLACE
        )

        assertEquals(1, report.importedReminders)
        assertTrue(report.alarmFailureReminderIds.isEmpty())
    }

    @Test
    fun damagedArchiveIsRejectedBeforeRoomWrites() = runTest {
        val error = runCatching {
            repository.restore(
                ByteArrayInputStream("not a zip archive".toByteArray()),
                null,
                ReminderArchiveRepository.RestoreMode.REPLACE
            )
        }.exceptionOrNull()

        assertTrue(error is InvalidReminderBackupException)
        coVerify(exactly = 0) { reminderDao.clearReminderDataForBackup() }
        coVerify(exactly = 0) { reminderDao.insertReminderEntitiesForBackup(any()) }
    }
}
