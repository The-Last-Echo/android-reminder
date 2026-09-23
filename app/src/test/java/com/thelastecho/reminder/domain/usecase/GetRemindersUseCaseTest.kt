package com.thelastecho.reminder.domain.usecase

import com.thelastecho.reminder.domain.model.Priority
import com.thelastecho.reminder.domain.model.Reminder
import com.thelastecho.reminder.domain.model.RepeatInterval
import com.thelastecho.reminder.domain.repository.ReminderRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GetRemindersUseCaseTest {

    private val repository = mockk<ReminderRepository>()
    private lateinit var useCase: GetRemindersUseCase

    private val now = System.currentTimeMillis()
    private val oneHourLater = now + 3600 * 1000
    private val oneHourAgo = now - 3600 * 1000

    private val activeReminder = Reminder(
        id = 1,
        title = "Active",
        dueDateTimeEpochMillis = oneHourLater,
        isCompleted = false
    )

    private val completedReminder = Reminder(
        id = 2,
        title = "Completed",
        isCompleted = true,
        completedAt = now
    )

    private val overdueReminder = Reminder(
        id = 3,
        title = "Overdue",
        dueDateTimeEpochMillis = oneHourAgo,
        isCompleted = false
    )

    @Before
    fun setup() {
        useCase = GetRemindersUseCase(repository)
        every { repository.getAllReminders() } returns flowOf(
            listOf(activeReminder, completedReminder, overdueReminder)
        )
    }

    @Test
    fun filterAll_returnsOnlyNonCompleted() = runTest {
        val result = useCase(ReminderFilter.ALL).first()
        assertEquals(2, result.size)
        assertTrue(result.none { it.isCompleted })
    }

    @Test
    fun filterCompleted_returnsOnlyCompleted() = runTest {
        val result = useCase(ReminderFilter.COMPLETED).first()
        assertEquals(1, result.size)
        assertTrue(result.all { it.isCompleted })
    }

    @Test
    fun filterOverdue_returnsOnlyOverdue() = runTest {
        val result = useCase(ReminderFilter.OVERDUE).first()
        assertEquals(1, result.size)
        assertEquals("Overdue", result.first().title)
    }
}
