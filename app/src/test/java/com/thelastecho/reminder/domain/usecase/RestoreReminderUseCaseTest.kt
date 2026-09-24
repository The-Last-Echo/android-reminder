package com.thelastecho.reminder.domain.usecase

import com.thelastecho.reminder.core.alarm.AlarmScheduler
import com.thelastecho.reminder.domain.model.Reminder
import com.thelastecho.reminder.domain.repository.ReminderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreReminderUseCaseTest {
    private val repository = mockk<ReminderRepository>(relaxed = true)
    private val scheduler = mockk<AlarmScheduler>(relaxed = true)

    @Test
    fun restoresOriginalRecordAndReschedulesFutureAlarm() = runTest {
        val reminder = Reminder(id = 21, title = "Original title", dueDateTimeEpochMillis = System.currentTimeMillis() + 60_000, notes = "Original notes")
        coEvery { repository.getDeletedReminderByIdOnce(21) } returns reminder
        val restored = RestoreReminderUseCase(repository, scheduler)(21)
        assertTrue(restored)
        coVerify(exactly = 1) { repository.restoreReminder(21) }
        verify(exactly = 1) { scheduler.schedule(match { it.id == 21L && it.title == "Original title" && it.notes == "Original notes" && it.deletedAt == null && it.expiresAt == null }) }
    }

    @Test
    fun completedReminderDoesNotScheduleAlarm() = runTest {
        coEvery { repository.getDeletedReminderByIdOnce(22) } returns Reminder(id = 22, title = "Done", isCompleted = true)
        assertTrue(RestoreReminderUseCase(repository, scheduler)(22))
        verify(exactly = 0) { scheduler.schedule(any()) }
    }

    @Test
    fun missingDeletedRecordDoesNothing() = runTest {
        coEvery { repository.getDeletedReminderByIdOnce(23) } returns null
        assertFalse(RestoreReminderUseCase(repository, scheduler)(23))
        coVerify(exactly = 0) { repository.restoreReminder(any()) }
        verify(exactly = 0) { scheduler.schedule(any()) }
    }
}
