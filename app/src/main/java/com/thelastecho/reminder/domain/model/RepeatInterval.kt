package com.thelastecho.reminder.domain.model

/**
 * Recurrence intervals for repeating reminders.
 */
enum class RepeatInterval(val id: String) {
    ONCE("ONCE"),
    MINUTELY("MINUTELY"),
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

enum class RepeatDuration(val id: String) {
    FOREVER("FOREVER"),
    COUNT("COUNT"),
    UNTIL("UNTIL");

    companion object {
        fun fromId(id: String?): RepeatDuration = entries.find { it.id == id } ?: FOREVER
    }
}
