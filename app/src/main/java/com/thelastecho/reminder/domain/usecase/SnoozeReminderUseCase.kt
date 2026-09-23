package com.thelastecho.reminder.domain.usecase

import com.thelastecho.reminder.core.alarm.AlarmScheduler
import com.thelastecho.reminder.domain.repository.ReminderRepository

/**
 * UseCase to postpone a reminder by a given duration (e.g. 10 minutes).
 */
class SnoozeReminderUseCase(
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(reminderId: Long, snoozeDurationMillis: Long = 10 * 60 * 1000L) {
        val reminder = repository.getReminderByIdOnce(reminderId) ?: return
        val newDueTime = System.currentTimeMillis() + snoozeDurationMillis
        val snoozedReminder = reminder.copy(
            dueDateTimeEpochMillis = newDueTime,
            isCompleted = false
        )
        repository.saveReminder(snoozedReminder)
        alarmScheduler.schedule(snoozedReminder)
    }
}
