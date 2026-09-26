package com.thelastecho.reminder.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thelastecho.reminder.R
import com.thelastecho.reminder.core.alarm.AlarmScheduler
import com.thelastecho.reminder.domain.repository.ReminderRepository
import com.thelastecho.reminder.domain.usecase.SnoozeReminderUseCase
import com.thelastecho.reminder.domain.usecase.ToggleReminderCompleteUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReminderAlarmViewModel(
    initialState: ReminderAlarmUiState,
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<ReminderAlarmEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: ReminderAlarmIntent) {
        when (intent) {
            ReminderAlarmIntent.Dismiss -> viewModelScope.launch { _effect.send(ReminderAlarmEffect.Finish()) }
            ReminderAlarmIntent.OpenReminder -> viewModelScope.launch { _effect.send(ReminderAlarmEffect.Finish(openReminder = true)) }
            ReminderAlarmIntent.Delete -> dispatchDeleteAction()
            ReminderAlarmIntent.Complete,
            ReminderAlarmIntent.Snooze -> performAction(intent)
        }
    }

    private fun dispatchDeleteAction() {
        if (_uiState.value.isProcessing) return
        _uiState.update { it.copy(isProcessing = true, errorMessageRes = null) }
        viewModelScope.launch { _effect.send(ReminderAlarmEffect.DispatchDeleteAction) }
    }

    private fun performAction(intent: ReminderAlarmIntent) {
        if (_uiState.value.isProcessing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessageRes = null) }
            try {
                val id = _uiState.value.reminderId
                when (intent) {
                    ReminderAlarmIntent.Complete -> ToggleReminderCompleteUseCase(repository, alarmScheduler)(id, isCompleted = true)
                    ReminderAlarmIntent.Snooze -> SnoozeReminderUseCase(repository, alarmScheduler)(id)
                    else -> return@launch
                }
                _effect.send(ReminderAlarmEffect.Finish())
            } catch (_: Exception) {
                _uiState.update { it.copy(isProcessing = false, errorMessageRes = R.string.reminder_action_failed) }
            }
        }
    }
}
