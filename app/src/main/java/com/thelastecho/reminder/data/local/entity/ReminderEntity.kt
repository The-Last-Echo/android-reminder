package com.thelastecho.reminder.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.thelastecho.reminder.domain.model.Priority
import com.thelastecho.reminder.domain.model.Reminder
import com.thelastecho.reminder.domain.model.RepeatInterval

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["dueDateTimeEpochMillis"]),
        Index(value = ["isCompleted"])
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val dueDateTimeEpochMillis: Long? = null,
    val isCompleted: Boolean = false,
    val priorityLevel: Int = 0,
    val repeatIntervalId: String = "ONCE",
    val categoryId: Long? = null,
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    fun toDomain(): Reminder = Reminder(
        id = id,
        title = title,
        notes = notes,
        dueDateTimeEpochMillis = dueDateTimeEpochMillis,
        isCompleted = isCompleted,
        priority = Priority.fromLevel(priorityLevel),
        repeatInterval = RepeatInterval.fromId(repeatIntervalId),
        categoryId = categoryId,
        imageUri = imageUri,
        subTasks = emptyList(),
        createdAt = createdAt,
        completedAt = completedAt
    )

    companion object {
        fun fromDomain(domain: Reminder): ReminderEntity = ReminderEntity(
            id = domain.id,
            title = domain.title,
            notes = domain.notes,
            dueDateTimeEpochMillis = domain.dueDateTimeEpochMillis,
            isCompleted = domain.isCompleted,
            priorityLevel = domain.priority.level,
            repeatIntervalId = domain.repeatInterval.id,
            categoryId = domain.categoryId,
            imageUri = domain.imageUri,
            createdAt = domain.createdAt,
            completedAt = domain.completedAt
        )
    }
}
