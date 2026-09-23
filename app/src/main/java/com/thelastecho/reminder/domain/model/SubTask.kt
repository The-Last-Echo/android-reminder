package com.thelastecho.reminder.domain.model

/**
 * Checklist sub-item belonging to a reminder.
 */
data class SubTask(
    val id: Long = 0,
    val reminderId: Long = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0
)
