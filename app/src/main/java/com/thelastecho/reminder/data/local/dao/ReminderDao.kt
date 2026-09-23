package com.thelastecho.reminder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.thelastecho.reminder.data.local.entity.ReminderEntity
import com.thelastecho.reminder.data.local.entity.ReminderWithSubTasks
import com.thelastecho.reminder.data.local.entity.SubTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Transaction
    @Query("SELECT * FROM reminders WHERE isDeleted = 0 ORDER BY isCompleted ASC, CASE WHEN dueDateTimeEpochMillis IS NULL THEN 1 ELSE 0 END, dueDateTimeEpochMillis ASC, createdAt DESC")
    fun getAllRemindersWithSubTasks(): Flow<List<ReminderWithSubTasks>>

    @Transaction
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND isDeleted = 0 ORDER BY CASE WHEN dueDateTimeEpochMillis IS NULL THEN 1 ELSE 0 END, dueDateTimeEpochMillis ASC, createdAt DESC")
    fun getActiveRemindersWithSubTasks(): Flow<List<ReminderWithSubTasks>>

    @Transaction
    @Query("SELECT * FROM reminders WHERE isCompleted = 1 AND isDeleted = 0 ORDER BY completedAt DESC, createdAt DESC")
    fun getCompletedRemindersWithSubTasks(): Flow<List<ReminderWithSubTasks>>

    @Transaction
    @Query("SELECT * FROM reminders WHERE categoryId = :categoryId AND isDeleted = 0 ORDER BY isCompleted ASC, CASE WHEN dueDateTimeEpochMillis IS NULL THEN 1 ELSE 0 END, dueDateTimeEpochMillis ASC, createdAt DESC")
    fun getRemindersByCategoryWithSubTasks(categoryId: Long): Flow<List<ReminderWithSubTasks>>

    @Transaction
    @Query("SELECT * FROM reminders WHERE id = :id AND isDeleted = 0")
    fun getReminderWithSubTasksById(id: Long): Flow<ReminderWithSubTasks?>

    @Transaction
    @Query("SELECT * FROM reminders WHERE id = :id AND isDeleted = 0")
    suspend fun getReminderWithSubTasksByIdOnce(id: Long): ReminderWithSubTasks?

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND isDeleted = 0 AND dueDateTimeEpochMillis > :currentMillis")
    suspend fun getActiveScheduledReminders(currentMillis: Long): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("UPDATE reminders SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :id")
    suspend fun updateCompletionStatus(id: Long, isCompleted: Boolean, completedAt: Long?)

    @Query("UPDATE reminders SET dueDateTimeEpochMillis = :newDueTime, isCompleted = 0, completedAt = NULL WHERE id = :id")
    suspend fun snoozeReminder(id: Long, newDueTime: Long)

    @Transaction
    @Query("SELECT * FROM reminders WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedRemindersWithSubTasks(): Flow<List<ReminderWithSubTasks>>

    @Query("UPDATE reminders SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteReminder(id: Long, deletedAt: Long)

    @Query("UPDATE reminders SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreReminder(id: Long)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun permanentlyDeleteReminder(id: Long)

    @Query("DELETE FROM reminders WHERE isDeleted = 1")
    suspend fun permanentlyDeleteAllDeletedReminders()

    @Query("DELETE FROM reminders WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt <= :cutoffMillis")
    suspend fun permanentlyDeleteExpiredReminders(cutoffMillis: Long)

    // Subtask management
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTasks(subTasks: List<SubTaskEntity>)

    @Query("DELETE FROM subtasks WHERE reminderId = :reminderId")
    suspend fun deleteSubtasksByReminderId(reminderId: Long)

    @Transaction
    suspend fun insertOrUpdateReminderWithSubTasks(
        reminder: ReminderEntity,
        subTasks: List<SubTaskEntity>
    ): Long {
        val reminderId = insertReminder(reminder)
        deleteSubtasksByReminderId(reminderId)
        if (subTasks.isNotEmpty()) {
            val entitiesWithId = subTasks.map { it.copy(reminderId = reminderId) }
            insertSubTasks(entitiesWithId)
        }
        return reminderId
    }
}
