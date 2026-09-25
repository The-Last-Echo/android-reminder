package com.thelastecho.reminder.presentation.home

import com.thelastecho.reminder.domain.model.Category
import com.thelastecho.reminder.domain.model.Reminder
import com.thelastecho.reminder.domain.usecase.ReminderFilter

data class HomeUiState(
    val reminders: List<Reminder> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedFilter: ReminderFilter = ReminderFilter.ALL,
    val selectedCategoryId: Long? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val allCount: Int = 0,
    val todayCount: Int = 0,
    val scheduledCount: Int = 0,
    val completedCount: Int = 0,
    val addButtonOnLeft: Boolean = false
)

sealed interface HomeIntent {
    data class SelectFilter(val filter: ReminderFilter) : HomeIntent
    data class SelectCategory(val categoryId: Long?) : HomeIntent
    data class UpdateSearch(val query: String) : HomeIntent
    data class ToggleComplete(val reminderId: Long, val isCompleted: Boolean) : HomeIntent
    data class ToggleSubTask(val reminderId: Long, val subTaskId: Long, val isCompleted: Boolean) : HomeIntent
    data class DeleteReminder(val reminderId: Long) : HomeIntent
    data class UndoDelete(val reminderId: Long) : HomeIntent
    data object CreateNewReminder : HomeIntent
    data class EditReminder(val reminderId: Long) : HomeIntent
    data object OpenSettings : HomeIntent
    data object Refresh : HomeIntent
}

sealed interface HomeEffect {
    data class NavigateToEditor(val reminderId: Long? = null) : HomeEffect
    data object NavigateToSettings : HomeEffect
    data class ShowUndoDelete(val reminderId: Long) : HomeEffect
}
