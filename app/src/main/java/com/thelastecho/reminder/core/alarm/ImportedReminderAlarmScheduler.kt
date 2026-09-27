package com.thelastecho.reminder.core.alarm

import com.thelastecho.reminder.domain.model.Reminder

class ImportedReminderAlarmScheduler(
    private val alarmScheduler: AlarmScheduler,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    data class ApplyReport(
        val scheduledCount: Int,
        val failedReminderIds: Set<Long>
    )

    fun apply(importedReminders: List<Reminder>, replacedReminderIds: Set<Long> = emptySet()): ApplyReport {
        val failedIds = linkedSetOf<Long>()
        replacedReminderIds.forEach { reminderId ->
            runCatching { alarmScheduler.cancel(reminderId) }
                .onSuccess { failedIds.remove(reminderId) }
                .onFailure { failedIds += reminderId }
        }
        val now = nowMillis()
        var scheduled = 0
        importedReminders.forEach { reminder ->
            val dueTime = reminder.dueDateTimeEpochMillis
            if (!reminder.isCompleted && reminder.deletedAt == null && dueTime != null && dueTime > now) {
                runCatching { alarmScheduler.schedule(reminder) }
                    .onSuccess { scheduled++; failedIds.remove(reminder.id) }
                    .onFailure { failedIds += reminder.id }
            } else {
                runCatching { alarmScheduler.cancel(reminder.id) }
                    .onSuccess { failedIds.remove(reminder.id) }
                    .onFailure { failedIds += reminder.id }
            }
        }
        return ApplyReport(scheduled, failedIds)
    }
}
