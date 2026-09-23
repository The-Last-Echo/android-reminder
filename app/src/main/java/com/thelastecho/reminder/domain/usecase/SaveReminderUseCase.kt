package com.thelastecho.reminder.domain.usecase

import com.thelastecho.reminder.core.alarm.AlarmScheduler
import com.thelastecho.reminder.domain.model.Reminder
import com.thelastecho.reminder.domain.repository.ReminderRepository

/**
 * UseCase to save or update a reminder and configure its alarm if applicable.
 */
class SaveReminderUseCase(
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(reminder: Reminder): Result<Long> {
        val trimmedTitle = reminder.title.trim()
        if (trimmedTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Reminder title cannot be empty"))
        }

        val cleanedReminder = reminder.copy(title = trimmedTitle)
        val reminderId = repository.saveReminder(cleanedReminder)
        val updatedReminder = cleanedReminder.copy(id = reminderId)

        if (!updatedReminder.isCompleted &&
            updatedReminder.dueDateTimeEpochMillis != null &&
            updatedReminder.dueDateTimeEpochMillis > System.currentTimeMillis()
        ) {
            alarmScheduler.schedule(updatedReminder)
        } else {
            alarmScheduler.cancel(reminderId)
        }

        return Result.success(reminderId)
    }
}
