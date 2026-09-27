package com.thelastecho.reminder.domain.usecase

import com.thelastecho.reminder.core.alarm.AlarmScheduler
import com.thelastecho.reminder.domain.model.Reminder
import com.thelastecho.reminder.domain.repository.ReminderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnoozeReminderUseCaseTest {

    private val repository = mockk<ReminderRepository>(relaxed = true)
    private val scheduler = mockk<AlarmScheduler>(relaxed = true)

    @Test
    fun snoozeMovesReminderTenMinutesAndSchedulesUpdatedReminder() = runTest {
        val reminderId = 61L
        val reminder = Reminder(
            id = reminderId,
            title = "Call back",
            dueDateTimeEpochMillis = 1_000L,
            isCompleted = true
        )
        val savedReminder = slot<Reminder>()
        coEvery { repository.getReminderByIdOnce(reminderId) } returns reminder
        coEvery { repository.saveReminder(any()) } returns reminderId
        val beforeSnooze = System.currentTimeMillis()

        SnoozeReminderUseCase(repository, scheduler)(reminderId)

        val afterSnooze = System.currentTimeMillis()
        coVerify(exactly = 1) { repository.saveReminder(capture(savedReminder)) }
        val updatedReminder = savedReminder.captured
        assertEquals(reminderId, updatedReminder.id)
        assertEquals("Call back", updatedReminder.title)
        assertFalse(updatedReminder.isCompleted)
        assertTrue(
            updatedReminder.dueDateTimeEpochMillis in
                (beforeSnooze + SNOOZE_DURATION_MILLIS)..(afterSnooze + SNOOZE_DURATION_MILLIS)
        )
        verify(exactly = 1) { scheduler.schedule(updatedReminder) }
    }

    @Test
    fun missingReminderIsNotSavedOrScheduled() = runTest {
        val reminderId = 62L
        coEvery { repository.getReminderByIdOnce(reminderId) } returns null

        SnoozeReminderUseCase(repository, scheduler)(reminderId)

        coVerify(exactly = 0) { repository.saveReminder(any()) }
        verify(exactly = 0) { scheduler.schedule(any()) }
    }

    private companion object {
        const val SNOOZE_DURATION_MILLIS = 10L * 60 * 1000
    }
}