package com.thelastecho.reminder.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.thelastecho.reminder.domain.model.Priority
import com.thelastecho.reminder.domain.model.Reminder
import com.thelastecho.reminder.domain.model.RepeatInterval

data class ReminderWithSubTasks(
    @Embedded val reminder: ReminderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "reminderId"
    )
    val subTasks: List<SubTaskEntity>
) {
    fun toDomain(): Reminder = Reminder(
        id = reminder.id,
        title = reminder.title,
        notes = reminder.notes,
        dueDateTimeEpochMillis = reminder.dueDateTimeEpochMillis,
        isCompleted = reminder.isCompleted,
        priority = Priority.fromLevel(reminder.priorityLevel),
        repeatInterval = RepeatInterval.fromId(reminder.repeatIntervalId),
        categoryId = reminder.categoryId,
        imageUri = reminder.imageUri,
        subTasks = subTasks.sortedBy { it.orderIndex }.map { it.toDomain() },
        createdAt = reminder.createdAt,
        completedAt = reminder.completedAt
    )
}
