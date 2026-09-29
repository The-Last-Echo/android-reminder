package com.thelastecho.reminder.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.thelastecho.reminder.domain.model.Priority
import com.thelastecho.reminder.domain.model.Reminder
import com.thelastecho.reminder.domain.model.RepeatDuration
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
        repeatEvery = reminder.repeatEvery,
        repeatDuration = RepeatDuration.fromId(reminder.repeatDurationId),
        repeatCount = reminder.repeatCount,
        repeatUntilEpochMillis = reminder.repeatUntilEpochMillis,
        repeatCompletedCount = reminder.repeatCompletedCount,
        categoryId = reminder.categoryId,
        isFavorite = reminder.isFavorite,
        legacyImageUri = reminder.imageUri,
        notificationStyle = reminder.notificationStyleId,
        subTasks = subTasks.sortedBy { it.orderIndex }.map { it.toDomain() },
        createdAt = reminder.createdAt,
        completedAt = reminder.completedAt,
        deletedAt = reminder.deletedAt,
        expiresAt = reminder.expiresAt,
        imagePath = reminder.imagePath
    )
}
