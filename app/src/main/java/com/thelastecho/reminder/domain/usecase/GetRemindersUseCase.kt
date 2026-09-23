package com.thelastecho.reminder.domain.usecase

import com.thelastecho.reminder.domain.model.Reminder
import com.thelastecho.reminder.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class ReminderFilter {
    ALL,
    TODAY,
    SCHEDULED,
    OVERDUE,
    COMPLETED
}

/**
 * UseCase to retrieve reminders filtered by category, status, or date range.
 */
class GetRemindersUseCase(
    private val repository: ReminderRepository
) {
    operator fun invoke(
        filter: ReminderFilter = ReminderFilter.ALL,
        categoryId: Long? = null
    ): Flow<List<Reminder>> {
        val flow = if (categoryId != null) {
            repository.getRemindersByCategory(categoryId)
        } else {
            repository.getAllReminders()
        }

        return flow.map { reminders ->
            val nowMillis = System.currentTimeMillis()
            val today = LocalDate.now()
            val zoneId = ZoneId.systemDefault()

            when (filter) {
                ReminderFilter.ALL -> reminders.filter { !it.isCompleted }
                ReminderFilter.COMPLETED -> reminders.filter { it.isCompleted }
                ReminderFilter.OVERDUE -> reminders.filter { !it.isCompleted && it.dueDateTimeEpochMillis != null && it.dueDateTimeEpochMillis < nowMillis }
                ReminderFilter.TODAY -> reminders.filter { reminder ->
                    if (reminder.isCompleted || reminder.dueDateTimeEpochMillis == null) return@filter false
                    val reminderDate = Instant.ofEpochMilli(reminder.dueDateTimeEpochMillis)
                        .atZone(zoneId)
                        .toLocalDate()
                    reminderDate.isEqual(today)
                }
                ReminderFilter.SCHEDULED -> reminders.filter {
                    !it.isCompleted && it.dueDateTimeEpochMillis != null && it.dueDateTimeEpochMillis >= nowMillis
                }
            }
        }
    }
}
