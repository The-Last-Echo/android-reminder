package com.thelastecho.reminder.domain.usecase

import com.thelastecho.reminder.core.alarm.AlarmScheduler
import com.thelastecho.reminder.domain.model.RepeatInterval
import com.thelastecho.reminder.domain.model.RepeatDuration
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
                val nextCompletedCount = reminder.repeatCompletedCount + 1
                val nextEpochMillis = calculateNextOccurrence(
                    currentEpochMillis = reminder.dueDateTimeEpochMillis,
                    interval = reminder.repeatInterval,
                    repeatEvery = reminder.repeatEvery
                )
                val canRepeat = when (reminder.repeatDuration) {
                    RepeatDuration.FOREVER -> true
                    RepeatDuration.COUNT -> nextCompletedCount <= reminder.repeatCount
                    RepeatDuration.UNTIL -> reminder.repeatUntilEpochMillis?.let { nextEpochMillis <= it } ?: true
                }
                if (!canRepeat) {
                    repository.toggleReminderComplete(reminderId, true)
                    return
                }
                val updatedReminder = reminder.copy(
                    dueDateTimeEpochMillis = nextEpochMillis,
                    repeatCompletedCount = nextCompletedCount,
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

    internal fun calculateNextOccurrence(
        currentEpochMillis: Long,
        interval: RepeatInterval,
        repeatEvery: Int = 1
    ): Long {
        val zone = ZoneId.systemDefault()
        var zdt = Instant.ofEpochMilli(currentEpochMillis).atZone(zone)
        val now = ZonedDateTime.now(zone)

        do {
            zdt = when (interval) {
                RepeatInterval.MINUTELY -> zdt.plusMinutes(repeatEvery.coerceAtLeast(1).toLong())
                RepeatInterval.HOURLY -> zdt.plusHours(repeatEvery.coerceAtLeast(1).toLong())
                RepeatInterval.DAILY -> zdt.plusDays(repeatEvery.coerceAtLeast(1).toLong())
                RepeatInterval.WEEKDAYS -> {
                    var next = zdt.plusDays(1)
                    while (next.dayOfWeek == DayOfWeek.SATURDAY || next.dayOfWeek == DayOfWeek.SUNDAY) {
                        next = next.plusDays(1)
                    }
                    next
                }
                RepeatInterval.WEEKLY -> zdt.plusWeeks(repeatEvery.coerceAtLeast(1).toLong())
                RepeatInterval.MONTHLY -> zdt.plusMonths(repeatEvery.coerceAtLeast(1).toLong())
                RepeatInterval.YEARLY -> zdt.plusYears(repeatEvery.coerceAtLeast(1).toLong())
                RepeatInterval.ONCE -> zdt
            }
        } while (zdt.isBefore(now) && interval != RepeatInterval.ONCE)

        return zdt.toInstant().toEpochMilli()
    }
}
