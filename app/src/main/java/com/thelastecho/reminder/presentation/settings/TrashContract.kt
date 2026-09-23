package com.thelastecho.reminder.presentation.settings

import com.thelastecho.reminder.domain.model.Reminder

data class TrashUiState(
    val deletedReminders: List<Reminder> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface TrashIntent {
    data object LoadTrash : TrashIntent
    data class RestoreReminder(val reminderId: Long) : TrashIntent
    data class PermanentlyDeleteReminder(val reminderId: Long) : TrashIntent
    data object EmptyTrash : TrashIntent
}

sealed interface TrashEffect {
    data class ShowSnackbar(val message: String) : TrashEffect
}
