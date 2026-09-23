package com.thelastecho.reminder.domain.model

/**
 * Recurrence intervals for repeating reminders.
 */
enum class RepeatInterval(val id: String, val displayName: String) {
    ONCE("ONCE", "Does not repeat"),
    HOURLY("HOURLY", "Hourly"),
    DAILY("DAILY", "Daily"),
    WEEKDAYS("WEEKDAYS", "Mon to Fri"),
    WEEKLY("WEEKLY", "Weekly"),
    MONTHLY("MONTHLY", "Monthly"),
    YEARLY("YEARLY", "Yearly");

    companion object {
        fun fromId(id: String?): RepeatInterval =
            entries.find { it.id == id } ?: ONCE
    }
}
