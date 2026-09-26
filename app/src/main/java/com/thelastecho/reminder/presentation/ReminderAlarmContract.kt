package com.thelastecho.reminder.presentation

data class ReminderAlarmUiState(
    val reminderId: Long,
    val title: String,
    val notes: String,
    val photoUri: String?,
    val isProcessing: Boolean = false,
    val errorMessageRes: Int? = null
)

sealed interface ReminderAlarmIntent {
    data object Complete : ReminderAlarmIntent
    data object Snooze : ReminderAlarmIntent
    data object Delete : ReminderAlarmIntent
    data object Dismiss : ReminderAlarmIntent
    data object OpenReminder : ReminderAlarmIntent
}

sealed interface ReminderAlarmEffect {
    data class Finish(val openReminder: Boolean = false) : ReminderAlarmEffect
    data object DispatchDeleteAction : ReminderAlarmEffect
}
