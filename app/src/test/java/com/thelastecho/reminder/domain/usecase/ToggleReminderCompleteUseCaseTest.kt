package com.thelastecho.reminder.domain.usecase

import com.thelastecho.reminder.core.alarm.AlarmScheduler
import com.thelastecho.reminder.domain.model.Reminder
import com.thelastecho.reminder.domain.model.RepeatInterval
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
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

class ToggleReminderCompleteUseCaseTest {

    private val repository = mockk<ReminderRepository>(relaxed = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
    private lateinit var useCase: ToggleReminderCompleteUseCase

    @Before
    fun setup() {
        useCase = ToggleReminderCompleteUseCase(repository, alarmScheduler)
    }

    @Test
    fun nonRepeatingReminder_whenCompleted_cancelsAlarmAndMarksComplete() = runTest {
        val reminder = Reminder(
            id = 5,
            title = "Single task",
            repeatInterval = RepeatInterval.ONCE,
            isCompleted = false
        )
        coEvery { repository.getReminderByIdOnce(5) } returns reminder

        useCase(5, isCompleted = true)

        verify { alarmScheduler.cancel(5) }
        coVerify { repository.toggleReminderComplete(5, true) }
    }

    @Test
    fun repeatingDailyReminder_whenCompleted_schedulesNextOccurrence() = runTest {
        val now = System.currentTimeMillis()
        val reminder = Reminder(
            id = 6,
            title = "Daily meditation",
            dueDateTimeEpochMillis = now + 1000,
            repeatInterval = RepeatInterval.DAILY,
            isCompleted = false
        )
        coEvery { repository.getReminderByIdOnce(6) } returns reminder

        useCase(6, isCompleted = true)

        verify { alarmScheduler.cancel(6) }
        coVerify { repository.saveReminder(match { it.id == 6L && !it.isCompleted }) }
        verify { alarmScheduler.schedule(match { it.id == 6L }) }
    }

    @Test
    fun weekdaysCalculation_skipsWeekends() {
        val zone = ZoneId.systemDefault()
        // Friday at 10:00
        val friday = java.time.LocalDate.of(2026, 9, 25).atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        val nextOccurrence = useCase.calculateNextOccurrence(friday, RepeatInterval.WEEKDAYS)

        val nextDate = Instant.ofEpochMilli(nextOccurrence).atZone(zone).toLocalDate()
        assertEquals(DayOfWeek.MONDAY, nextDate.dayOfWeek)
    }
}
