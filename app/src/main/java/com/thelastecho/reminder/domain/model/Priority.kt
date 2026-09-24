package com.thelastecho.reminder.domain.model

/**
 * Priority levels for reminders, matching Material 3 color coding conventions.
 */
enum class Priority(val level: Int) {
    NONE(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    companion object {
        fun fromLevel(level: Int): Priority =
            entries.find { it.level == level } ?: NONE
    }
}
