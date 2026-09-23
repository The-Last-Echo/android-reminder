package com.thelastecho.reminder.core.alarm

import com.thelastecho.reminder.domain.model.Reminder

/**
 * Abstraction for scheduling, rescheduling, and canceling exact reminder alarms.
 */
interface AlarmScheduler {
    fun schedule(reminder: Reminder)
    fun cancel(reminderId: Long)
    fun canScheduleExactAlarms(): Boolean
}
