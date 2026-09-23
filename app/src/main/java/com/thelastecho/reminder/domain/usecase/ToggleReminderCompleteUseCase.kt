package com.thelastecho.reminder.domain.usecase

import com.thelastecho.reminder.core.alarm.AlarmScheduler
import com.thelastecho.reminder.domain.model.RepeatInterval
import com.thelastecho.reminder.domain.repository.ReminderRepository
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * UseCase to mark a reminder as completed or uncompleted.
 * Handles automatic calculation and rescheduling of repeating reminders.
 */
class ToggleReminderCompleteUseCase(
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(reminderId: Long, isCompleted: Boolean) {
        val reminder = repository.getReminderByIdOnce(reminderId) ?: return

        if (isCompleted) {
            alarmScheduler.cancel(reminderId)

            if (reminder.repeatInterval != RepeatInterval.ONCE && reminder.dueDateTimeEpochMillis != null) {
                // Compute next occurrence for repeating reminder
                val nextEpochMillis = calculateNextOccurrence(
                    currentEpochMillis = reminder.dueDateTimeEpochMillis,
                    interval = reminder.repeatInterval
                )
                val updatedReminder = reminder.copy(
                    dueDateTimeEpochMillis = nextEpochMillis,
                    isCompleted = false,
                    completedAt = null
                )
                repository.saveReminder(updatedReminder)
                if (nextEpochMillis > System.currentTimeMillis()) {
                    alarmScheduler.schedule(updatedReminder)
                }
            } else {
                repository.toggleReminderComplete(reminderId, true)
            }
        } else {
            // Uncompleting reminder
            repository.toggleReminderComplete(reminderId, false)
            if (reminder.dueDateTimeEpochMillis != null && reminder.dueDateTimeEpochMillis > System.currentTimeMillis()) {
                alarmScheduler.schedule(reminder.copy(isCompleted = false))
            }
        }
    }

    internal fun calculateNextOccurrence(currentEpochMillis: Long, interval: RepeatInterval): Long {
        val zone = ZoneId.systemDefault()
        var zdt = Instant.ofEpochMilli(currentEpochMillis).atZone(zone)
        val now = ZonedDateTime.now(zone)

        do {
            zdt = when (interval) {
                RepeatInterval.HOURLY -> zdt.plusHours(1)
                RepeatInterval.DAILY -> zdt.plusDays(1)
                RepeatInterval.WEEKDAYS -> {
                    var next = zdt.plusDays(1)
                    while (next.dayOfWeek == DayOfWeek.SATURDAY || next.dayOfWeek == DayOfWeek.SUNDAY) {
                        next = next.plusDays(1)
                    }
                    next
                }
                RepeatInterval.WEEKLY -> zdt.plusWeeks(1)
                RepeatInterval.MONTHLY -> zdt.plusMonths(1)
                RepeatInterval.YEARLY -> zdt.plusYears(1)
                RepeatInterval.ONCE -> zdt
            }
        } while (zdt.isBefore(now) && interval != RepeatInterval.ONCE)

        return zdt.toInstant().toEpochMilli()
    }
}
