package com.thelastecho.reminder.presentation.settings

import com.thelastecho.reminder.domain.model.Reminder

data class TrashUiState(
    val deletedReminders: List<Reminder> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = ""
)

sealed interface TrashIntent {
    data object LoadTrash : TrashIntent
    data class RestoreReminder(val reminderId: Long) : TrashIntent
    data class PermanentlyDeleteReminder(val reminderId: Long) : TrashIntent
    data object EmptyTrash : TrashIntent
    data class UpdateSearch(val query: String) : TrashIntent
}

sealed interface TrashEffect {
    data class ShowSnackbar(val messageRes: Int) : TrashEffect
}
