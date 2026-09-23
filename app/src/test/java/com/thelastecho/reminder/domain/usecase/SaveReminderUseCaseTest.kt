package com.thelastecho.reminder.domain.usecase

import com.thelastecho.reminder.core.alarm.AlarmScheduler
import com.thelastecho.reminder.domain.model.Reminder
import com.thelastecho.reminder.domain.repository.ReminderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveReminderUseCaseTest {

    private val repository = mockk<ReminderRepository>(relaxed = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
    private lateinit var useCase: SaveReminderUseCase

    @Before
    fun setup() {
        useCase = SaveReminderUseCase(repository, alarmScheduler)
    }

    @Test
    fun blankTitle_returnsFailure() = runTest {
        val reminder = Reminder(title = "   ")
        val result = useCase(reminder)
        assertTrue(result.isFailure)
        assertEquals("Reminder title cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun validReminder_withFutureDueDate_schedulesAlarm() = runTest {
        val futureTime = System.currentTimeMillis() + 100000
        val reminder = Reminder(id = 42, title = "Valid reminder", dueDateTimeEpochMillis = futureTime)
        coEvery { repository.saveReminder(any()) } returns 42L

        val result = useCase(reminder)
        assertTrue(result.isSuccess)
        verify { alarmScheduler.schedule(match { it.id == 42L }) }
    }

    @Test
    fun completedReminder_cancelsAlarm() = runTest {
        val reminder = Reminder(id = 10, title = "Done task", isCompleted = true)
        coEvery { repository.saveReminder(any()) } returns 10L

        val result = useCase(reminder)
        assertTrue(result.isSuccess)
        verify { alarmScheduler.cancel(10L) }
    }
}
