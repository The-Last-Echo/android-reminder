package com.thelastecho.reminder.presentation.editor

import com.thelastecho.reminder.domain.model.Category
import com.thelastecho.reminder.domain.model.Priority
import com.thelastecho.reminder.domain.model.RepeatInterval
import com.thelastecho.reminder.domain.model.SubTask

data class EditorUiState(
    val reminderId: Long? = null,
    val title: String = "",
    val notes: String = "",
    val dueDateTimeEpochMillis: Long? = null,
    val priority: Priority = Priority.NONE,
    val repeatInterval: RepeatInterval = RepeatInterval.ONCE,
    val categoryId: Long? = null,
    val imageUri: String? = null,
    val notificationStyle: String? = null,
    val subTasks: List<SubTask> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isCompleted: Boolean = false,
    val isSaving: Boolean = false,
    val titleErrorRes: Int? = null
)

sealed interface EditorIntent {
    data class UpdateTitle(val title: String) : EditorIntent
    data class UpdateNotes(val notes: String) : EditorIntent
    data class SetDueDate(val epochDayMillis: Long) : EditorIntent
    data class SetDueTime(val hour: Int, val minute: Int) : EditorIntent
    data object ClearDateTime : EditorIntent
    data class SetPriority(val priority: Priority) : EditorIntent
    data class SetRepeatInterval(val interval: RepeatInterval) : EditorIntent
    data class SetCategory(val categoryId: Long?) : EditorIntent
    data class SetImageUri(val uriString: String?) : EditorIntent
    data class SetNotificationStyle(val style: String?) : EditorIntent
    data class AddSubTask(val text: String) : EditorIntent
    data class ToggleSubTask(val index: Int) : EditorIntent
    data class DeleteSubTask(val index: Int) : EditorIntent
    data object SaveReminder : EditorIntent
    data object DeleteReminder : EditorIntent
    data class UndoDelete(val reminderId: Long) : EditorIntent
}

sealed interface EditorEffect {
    data object NavigateBack : EditorEffect
    data class ShowSnackbar(val messageRes: Int) : EditorEffect
    data class ShowDeleteUndo(val reminderId: Long) : EditorEffect
}
