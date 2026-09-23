package com.thelastecho.reminder.domain.model

/**
 * Domain model representing a reminder item.
 */
data class Reminder(
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val dueDateTimeEpochMillis: Long? = null,
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.NONE,
    val repeatInterval: RepeatInterval = RepeatInterval.ONCE,
    val categoryId: Long? = null,
    val imageUri: String? = null,
    val notificationStyle: String? = null,
    val subTasks: List<SubTask> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val deletedAt: Long? = null
) {
    val isOverdue: Boolean
        get() = !isCompleted && dueDateTimeEpochMillis != null && dueDateTimeEpochMillis < System.currentTimeMillis()

    val hasTime: Boolean
        get() = dueDateTimeEpochMillis != null
}
