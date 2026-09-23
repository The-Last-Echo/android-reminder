package com.thelastecho.reminder.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.thelastecho.reminder.domain.model.SubTask

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["reminderId"])]
)
data class SubTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reminderId: Long,
    val title: String,
    val isCompleted: Boolean,
    val orderIndex: Int
) {
    fun toDomain(): SubTask = SubTask(
        id = id,
        reminderId = reminderId,
        title = title,
        isCompleted = isCompleted,
        orderIndex = orderIndex
    )

    companion object {
        fun fromDomain(domain: SubTask, reminderId: Long): SubTaskEntity = SubTaskEntity(
            id = domain.id,
            reminderId = reminderId,
            title = domain.title,
            isCompleted = domain.isCompleted,
            orderIndex = domain.orderIndex
        )
    }
}
