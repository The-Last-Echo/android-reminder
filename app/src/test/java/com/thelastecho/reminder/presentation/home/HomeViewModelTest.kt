package com.thelastecho.reminder.presentation.home

import com.thelastecho.reminder.domain.model.Reminder
import com.thelastecho.reminder.domain.repository.ReminderRepository
import com.thelastecho.reminder.domain.usecase.DeleteReminderUseCase
import com.thelastecho.reminder.domain.usecase.GetRemindersUseCase
import com.thelastecho.reminder.domain.usecase.ToggleReminderCompleteUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val repository = mockk<ReminderRepository>(relaxed = true)
    private val getRemindersUseCase = mockk<GetRemindersUseCase>(relaxed = true)
    private val toggleReminderCompleteUseCase = mockk<ToggleReminderCompleteUseCase>(relaxed = true)
    private val deleteReminderUseCase = mockk<DeleteReminderUseCase>(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun observeReminders_calculatesOverdueCountCorrectly() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600 * 1000
        val oneHourLater = now + 3600 * 1000

        // 1. Uncompleted with past date -> SHOULD be counted as overdue
        val overdueUncompleted = Reminder(
            id = 1,
            title = "Overdue task",
            dueDateTimeEpochMillis = oneHourAgo,
            isCompleted = false
        )

        // 2. Completed with past date -> SHOULD NOT be counted as overdue
        val overdueCompleted = Reminder(
            id = 2,
            title = "Completed overdue task",
            dueDateTimeEpochMillis = oneHourAgo,
            isCompleted = true
        )

        // 3. Uncompleted with no date -> SHOULD NOT be counted as overdue
        val noDateUncompleted = Reminder(
            id = 3,
            title = "No date task",
            dueDateTimeEpochMillis = null,
            isCompleted = false
        )

        // 4. Uncompleted with future date -> SHOULD NOT be counted as overdue
        val futureUncompleted = Reminder(
            id = 4,
            title = "Future task",
            dueDateTimeEpochMillis = oneHourLater,
            isCompleted = false
        )

        every { repository.getAllReminders() } returns flowOf(
            listOf(overdueUncompleted, overdueCompleted, noDateUncompleted, futureUncompleted)
        )
        every { repository.getAllCategories() } returns flowOf(emptyList())

        // When
        val viewModel = HomeViewModel(
            getRemindersUseCase = getRemindersUseCase,
            toggleReminderCompleteUseCase = toggleReminderCompleteUseCase,
            deleteReminderUseCase = deleteReminderUseCase,
            repository = repository
        )

        // Then
        val state = viewModel.uiState.value
        assertEquals(1, state.overdueCount)
    }
}
