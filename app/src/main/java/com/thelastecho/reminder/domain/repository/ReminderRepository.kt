package com.thelastecho.reminder.domain.repository

import com.thelastecho.reminder.domain.model.Category
import com.thelastecho.reminder.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing reminders, subtasks, and categories.
 */
interface ReminderRepository {
    fun getAllReminders(): Flow<List<Reminder>>
    fun getActiveReminders(): Flow<List<Reminder>>
    fun getCompletedReminders(): Flow<List<Reminder>>
    fun getRemindersByCategory(categoryId: Long): Flow<List<Reminder>>
    fun getReminderById(id: Long): Flow<Reminder?>
    suspend fun getReminderByIdOnce(id: Long): Reminder?
    suspend fun saveReminder(reminder: Reminder): Long
    suspend fun deleteReminder(reminderId: Long)
    suspend fun softDeleteReminder(reminderId: Long)
    suspend fun restoreReminder(reminderId: Long)
    suspend fun getDeletedReminderByIdOnce(reminderId: Long): Reminder?
    suspend fun permanentlyDeleteReminder(reminderId: Long)
    suspend fun permanentlyDeleteAllDeletedReminders()
    suspend fun permanentlyDeleteExpiredReminders(nowMillis: Long)
    fun getDeletedReminders(): Flow<List<Reminder>>
    suspend fun toggleSubTaskCompletion(reminderId: Long, subTaskId: Long, isCompleted: Boolean)
    suspend fun toggleReminderComplete(reminderId: Long, isCompleted: Boolean)
    suspend fun snoozeReminder(reminderId: Long, snoozeDurationMillis: Long)
    suspend fun getActiveScheduledReminders(): List<Reminder>

    fun getAllCategories(): Flow<List<Category>>
    suspend fun saveCategory(category: Category): Long
    suspend fun deleteCategory(categoryId: Long)
}
