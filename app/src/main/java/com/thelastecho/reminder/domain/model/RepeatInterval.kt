package com.thelastecho.reminder.domain.model

/**
 * Recurrence intervals for repeating reminders.
 */
enum class RepeatInterval(val id: String) {
    ONCE("ONCE"),
    HOURLY("HOURLY"),
    DAILY("DAILY"),
    WEEKDAYS("WEEKDAYS"),
    WEEKLY("WEEKLY"),
    MONTHLY("MONTHLY"),
    YEARLY("YEARLY");

    companion object {
        fun fromId(id: String?): RepeatInterval =
            entries.find { it.id == id } ?: ONCE
    }
}
