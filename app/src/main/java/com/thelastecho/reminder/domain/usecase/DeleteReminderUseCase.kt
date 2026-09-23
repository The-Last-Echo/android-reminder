package com.thelastecho.reminder.domain.usecase

import com.thelastecho.reminder.core.alarm.AlarmScheduler
import com.thelastecho.reminder.domain.repository.ReminderRepository

/**
 * UseCase to remove a reminder and cancel any scheduled alarm.
 */
class DeleteReminderUseCase(
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(reminderId: Long) {
        alarmScheduler.cancel(reminderId)
        repository.deleteReminder(reminderId)
    }
}
