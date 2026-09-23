package com.thelastecho.reminder.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thelastecho.reminder.domain.model.Reminder
import com.thelastecho.reminder.domain.repository.ReminderRepository
import com.thelastecho.reminder.domain.usecase.DeleteReminderUseCase
import com.thelastecho.reminder.domain.usecase.GetRemindersUseCase
import com.thelastecho.reminder.domain.usecase.ReminderFilter
import com.thelastecho.reminder.domain.usecase.ToggleReminderCompleteUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HomeViewModel(
    private val getRemindersUseCase: GetRemindersUseCase,
    private val toggleReminderCompleteUseCase: ToggleReminderCompleteUseCase,
    private val deleteReminderUseCase: DeleteReminderUseCase,
    private val repository: ReminderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observeRemindersAndCategories()
    }

    private fun observeRemindersAndCategories() {
        combine(
            repository.getAllReminders(),
            repository.getAllCategories()
        ) { allReminders, categories ->
            val nowMillis = System.currentTimeMillis()
            val today = LocalDate.now()
            val zone = ZoneId.systemDefault()

            val allCount = allReminders.count { !it.isCompleted }
            val completedCount = allReminders.count { it.isCompleted }
            val scheduledCount = allReminders.count {
                !it.isCompleted && it.dueDateTimeEpochMillis != null && it.dueDateTimeEpochMillis >= nowMillis
            }
            val todayCount = allReminders.count { reminder ->
                if (reminder.isCompleted || reminder.dueDateTimeEpochMillis == null) false
                else {
                    val date = Instant.ofEpochMilli(reminder.dueDateTimeEpochMillis).atZone(zone).toLocalDate()
                    date.isEqual(today)
                }
            }

            _uiState.update { current ->
                val filtered = applyFilterAndSearch(
                    allReminders = allReminders,
                    filter = current.selectedFilter,
                    categoryId = current.selectedCategoryId,
                    query = current.searchQuery
                )

                current.copy(
                    reminders = filtered,
                    categories = categories,
                    isLoading = false,
                    allCount = allCount,
                    todayCount = todayCount,
                    scheduledCount = scheduledCount,
                    completedCount = completedCount
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SelectFilter -> {
                _uiState.update { it.copy(selectedFilter = intent.filter) }
                refreshFilteredList()
            }
            is HomeIntent.SelectCategory -> {
                _uiState.update { it.copy(selectedCategoryId = intent.categoryId) }
                refreshFilteredList()
            }
            is HomeIntent.UpdateSearch -> {
                _uiState.update { it.copy(searchQuery = intent.query) }
                refreshFilteredList()
            }
            is HomeIntent.ToggleComplete -> {
                viewModelScope.launch {
                    toggleReminderCompleteUseCase(intent.reminderId, intent.isCompleted)
                }
            }
            is HomeIntent.DeleteReminder -> {
                viewModelScope.launch {
                    deleteReminderUseCase(intent.reminderId)
                    _effect.send(HomeEffect.ShowSnackbar("Reminder deleted"))
                }
            }
            is HomeIntent.CreateNewReminder -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToEditor())
                }
            }
            is HomeIntent.EditReminder -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToEditor(intent.reminderId))
                }
            }
            is HomeIntent.OpenSettings -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToSettings)
                }
            }
        }
    }

    private fun refreshFilteredList() {
        viewModelScope.launch {
            val allReminders = repository.getActiveReminders()
            // The observation in combine already auto-updates when state changes
            _uiState.update { current ->
                val updated = applyFilterAndSearch(
                    allReminders = current.reminders,
                    filter = current.selectedFilter,
                    categoryId = current.selectedCategoryId,
                    query = current.searchQuery
                )
                current.copy(reminders = updated)
            }
        }
    }

    private fun applyFilterAndSearch(
        allReminders: List<Reminder>,
        filter: ReminderFilter,
        categoryId: Long?,
        query: String
    ): List<Reminder> {
        val nowMillis = System.currentTimeMillis()
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()

        return allReminders.filter { reminder ->
            val matchesCategory = categoryId == null || reminder.categoryId == categoryId
            val matchesSearch = query.isBlank() ||
                    reminder.title.contains(query, ignoreCase = true) ||
                    reminder.notes.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                ReminderFilter.ALL -> !reminder.isCompleted
                ReminderFilter.COMPLETED -> reminder.isCompleted
                ReminderFilter.OVERDUE -> !reminder.isCompleted && reminder.dueDateTimeEpochMillis != null && reminder.dueDateTimeEpochMillis < nowMillis
                ReminderFilter.TODAY -> {
                    if (reminder.isCompleted || reminder.dueDateTimeEpochMillis == null) false
                    else {
                        val date = Instant.ofEpochMilli(reminder.dueDateTimeEpochMillis).atZone(zone).toLocalDate()
                        date.isEqual(today)
                    }
                }
                ReminderFilter.SCHEDULED -> !reminder.isCompleted && reminder.dueDateTimeEpochMillis != null && reminder.dueDateTimeEpochMillis >= nowMillis
            }

            matchesCategory && matchesSearch && matchesFilter
        }
    }
}
