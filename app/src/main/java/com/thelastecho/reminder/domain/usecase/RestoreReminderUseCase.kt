package com.thelastecho.reminder.domain.usecase

import com.thelastecho.reminder.core.alarm.AlarmScheduler
import com.thelastecho.reminder.domain.repository.ReminderRepository

class RestoreReminderUseCase(
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(reminderId: Long): Boolean {
        val reminder = repository.getDeletedReminderByIdOnce(reminderId) ?: return false
        repository.restoreReminder(reminderId)
        if (!reminder.isCompleted && (reminder.dueDateTimeEpochMillis ?: Long.MAX_VALUE) > System.currentTimeMillis()) {
            alarmScheduler.schedule(reminder.copy(deletedAt = null, expiresAt = null))
        }
        return true
    }
}
