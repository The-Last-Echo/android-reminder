package com.thelastecho.reminder.domain.model

/**
 * Priority levels for reminders, matching Material 3 color coding conventions.
 */
enum class Priority(val level: Int, val displayName: String) {
    NONE(0, "None"),
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High");

    companion object {
        fun fromLevel(level: Int): Priority =
            entries.find { it.level == level } ?: NONE
    }
}
