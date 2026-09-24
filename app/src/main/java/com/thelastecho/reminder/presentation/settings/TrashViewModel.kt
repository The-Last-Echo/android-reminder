package com.thelastecho.reminder.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thelastecho.reminder.domain.repository.ReminderRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrashViewModel(
    private val repository: ReminderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    private val _effect = Channel<TrashEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observeDeletedReminders()
    }

    private fun observeDeletedReminders() {
        viewModelScope.launch { repository.permanentlyDeleteExpiredReminders(System.currentTimeMillis()) }
        repository.getDeletedReminders().onEach { reminders ->
            _uiState.update { 
                it.copy(
                    deletedReminders = reminders,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onIntent(intent: TrashIntent) {
        when (intent) {
            TrashIntent.LoadTrash -> {
                observeDeletedReminders()
            }
            is TrashIntent.RestoreReminder -> {
                viewModelScope.launch {
                    repository.restoreReminder(intent.reminderId)
                    _effect.send(TrashEffect.ShowSnackbar(com.thelastecho.reminder.R.string.reminder_restored))
                }
            }
            is TrashIntent.PermanentlyDeleteReminder -> {
                viewModelScope.launch {
                    repository.permanentlyDeleteReminder(intent.reminderId)
                    _effect.send(TrashEffect.ShowSnackbar(com.thelastecho.reminder.R.string.reminder_permanently_deleted))
                }
            }
            is TrashIntent.UpdateSearch -> _uiState.update { it.copy(searchQuery = intent.query) }
            TrashIntent.EmptyTrash -> {
                viewModelScope.launch {
                    repository.permanentlyDeleteAllDeletedReminders()
                    _effect.send(TrashEffect.ShowSnackbar(com.thelastecho.reminder.R.string.trash_emptied))
                }
            }
        }
    }
}
