package com.thelastecho.reminder.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thelastecho.reminder.core.notification.ReminderNotificationManager
import com.thelastecho.reminder.domain.model.Priority
import com.thelastecho.reminder.domain.model.Reminder
import com.thelastecho.reminder.domain.model.RepeatInterval
import com.thelastecho.reminder.domain.model.SubTask
import com.thelastecho.reminder.domain.repository.ReminderRepository
import com.thelastecho.reminder.domain.usecase.DeleteReminderUseCase
import com.thelastecho.reminder.domain.usecase.SaveReminderUseCase
import com.thelastecho.reminder.domain.usecase.RestoreReminderUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class EditorViewModel(
    private val reminderId: Long?,
    private val saveReminderUseCase: SaveReminderUseCase,
    private val deleteReminderUseCase: DeleteReminderUseCase,
    private val repository: ReminderRepository,
    private val notificationManager: ReminderNotificationManager? = null,
    private val restoreReminderUseCase: RestoreReminderUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState(reminderId = reminderId))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _effect = Channel<EditorEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Load categories
            val categories = repository.getAllCategories().firstOrNull() ?: emptyList()
            _uiState.update { it.copy(categories = categories) }

            // If editing an existing reminder, load its details
            if (reminderId != null && reminderId > 0) {
                val existing = repository.getReminderByIdOnce(reminderId)
                if (existing != null) {
                    _uiState.update { current ->
                        current.copy(
                            title = existing.title,
                            notes = existing.notes,
                            dueDateTimeEpochMillis = existing.dueDateTimeEpochMillis,
                            priority = existing.priority,
                            repeatInterval = existing.repeatInterval,
                            categoryId = existing.categoryId,
                            imageUri = existing.imageUri,
                            notificationStyle = existing.notificationStyle,
                            subTasks = existing.subTasks,
                            isCompleted = existing.isCompleted
                        )
                    }
                }
            }
        }
    }

    fun onIntent(intent: EditorIntent) {
        when (intent) {
            is EditorIntent.UpdateTitle -> {
                _uiState.update { it.copy(title = intent.title, titleErrorRes = null) }
            }
            is EditorIntent.UpdateNotes -> {
                _uiState.update { it.copy(notes = intent.notes) }
            }
            is EditorIntent.SetDueDate -> {
                val zone = ZoneId.systemDefault()
                val currentMillis = _uiState.value.dueDateTimeEpochMillis
                val currentLocalTime = if (currentMillis != null) {
                    Instant.ofEpochMilli(currentMillis).atZone(zone).toLocalTime()
                } else {
                    LocalTime.of(9, 0) // Default morning reminder at 9:00 AM
                }

                val selectedLocalDate = Instant.ofEpochMilli(intent.epochDayMillis).atZone(ZoneId.of("UTC")).toLocalDate()
                val newZonedDateTime = ZonedDateTime.of(selectedLocalDate, currentLocalTime, zone)

                _uiState.update { it.copy(dueDateTimeEpochMillis = newZonedDateTime.toInstant().toEpochMilli()) }
            }
            is EditorIntent.SetDueTime -> {
                val zone = ZoneId.systemDefault()
                val currentMillis = _uiState.value.dueDateTimeEpochMillis
                val currentLocalDate = if (currentMillis != null) {
                    Instant.ofEpochMilli(currentMillis).atZone(zone).toLocalDate()
                } else {
                    LocalDate.now(zone)
                }

                val newTime = LocalTime.of(intent.hour, intent.minute)
                val newZdt = ZonedDateTime.of(currentLocalDate, newTime, zone)

                _uiState.update { it.copy(dueDateTimeEpochMillis = newZdt.toInstant().toEpochMilli()) }
            }
            is EditorIntent.ClearDateTime -> {
                _uiState.update { it.copy(dueDateTimeEpochMillis = null, repeatInterval = RepeatInterval.ONCE) }
            }
            is EditorIntent.SetPriority -> {
                _uiState.update { it.copy(priority = intent.priority) }
            }
            is EditorIntent.SetRepeatInterval -> {
                _uiState.update { it.copy(repeatInterval = intent.interval) }
            }
            is EditorIntent.SetCategory -> {
                _uiState.update { it.copy(categoryId = intent.categoryId) }
            }
            is EditorIntent.SetImageUri -> {
                _uiState.update { it.copy(imageUri = intent.uriString) }
            }
            is EditorIntent.SetNotificationStyle -> {
                _uiState.update { it.copy(notificationStyle = intent.style) }
            }
            is EditorIntent.AddSubTask -> {
                val text = intent.text.trim()
                if (text.isNotEmpty()) {
                    val newSubTask = SubTask(
                        reminderId = _uiState.value.reminderId ?: 0,
                        title = text,
                        isCompleted = false,
                        orderIndex = _uiState.value.subTasks.size
                    )
                    _uiState.update { it.copy(subTasks = it.subTasks + newSubTask) }
                }
            }
            is EditorIntent.ToggleSubTask -> {
                _uiState.update { current ->
                    val updated = current.subTasks.toMutableList()
                    if (intent.index in updated.indices) {
                        val item = updated[intent.index]
                        updated[intent.index] = item.copy(isCompleted = !item.isCompleted)
                    }
                    current.copy(subTasks = updated)
                }
            }
            is EditorIntent.DeleteSubTask -> {
                _uiState.update { current ->
                    val updated = current.subTasks.toMutableList()
                    if (intent.index in updated.indices) {
                        updated.removeAt(intent.index)
                    }
                    current.copy(subTasks = updated)
                }
            }
            is EditorIntent.SaveReminder -> {
                saveReminder()
            }
            is EditorIntent.DeleteReminder -> deleteReminder()
            is EditorIntent.UndoDelete -> viewModelScope.launch { restoreReminderUseCase?.invoke(intent.reminderId) }
        }
    }

    private fun saveReminder() {
        val state = _uiState.value
        if (state.title.trim().isEmpty()) {
            _uiState.update { it.copy(titleErrorRes = com.thelastecho.reminder.R.string.title_cannot_be_empty) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val reminderToSave = Reminder(
                id = state.reminderId ?: 0,
                title = state.title,
                notes = state.notes,
                dueDateTimeEpochMillis = state.dueDateTimeEpochMillis,
                isCompleted = state.isCompleted,
                priority = state.priority,
                repeatInterval = state.repeatInterval,
                categoryId = state.categoryId,
                imageUri = state.imageUri,
                notificationStyle = state.notificationStyle,
                subTasks = state.subTasks
            )

            val result = saveReminderUseCase(reminderToSave)
            result.fold(
                onSuccess = {
                    _effect.send(EditorEffect.NavigateBack)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false) }
                    _effect.send(EditorEffect.ShowSnackbar(com.thelastecho.reminder.R.string.save_failed))
                }
            )
        }
    }

    private fun deleteReminder() {
        val id = _uiState.value.reminderId ?: return
        viewModelScope.launch {
            notificationManager?.dismissNotification(id)
            deleteReminderUseCase(id)
            _effect.send(EditorEffect.ShowDeleteUndo(id))
        }
    }
}
