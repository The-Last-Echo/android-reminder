package com.thelastecho.reminder.data.repository

import com.thelastecho.reminder.data.local.dao.CategoryDao
import com.thelastecho.reminder.data.local.dao.ReminderDao
import com.thelastecho.reminder.data.local.entity.CategoryEntity
import com.thelastecho.reminder.data.local.entity.ReminderEntity
import com.thelastecho.reminder.data.local.entity.SubTaskEntity
import com.thelastecho.reminder.domain.model.Category
import com.thelastecho.reminder.domain.model.Reminder
import com.thelastecho.reminder.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReminderRepositoryImpl(
    private val reminderDao: ReminderDao,
    private val categoryDao: CategoryDao
) : ReminderRepository {

    override fun getAllReminders(): Flow<List<Reminder>> {
        return reminderDao.getAllRemindersWithSubTasks().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getActiveReminders(): Flow<List<Reminder>> {
        return reminderDao.getActiveRemindersWithSubTasks().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getCompletedReminders(): Flow<List<Reminder>> {
        return reminderDao.getCompletedRemindersWithSubTasks().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getRemindersByCategory(categoryId: Long): Flow<List<Reminder>> {
        return reminderDao.getRemindersByCategoryWithSubTasks(categoryId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getReminderById(id: Long): Flow<Reminder?> {
        return reminderDao.getReminderWithSubTasksById(id).map { it?.toDomain() }
    }

    override suspend fun getReminderByIdOnce(id: Long): Reminder? {
        return reminderDao.getReminderWithSubTasksByIdOnce(id)?.toDomain()
    }

    override suspend fun saveReminder(reminder: Reminder): Long {
        val reminderEntity = ReminderEntity.fromDomain(reminder)
        val subTaskEntities = reminder.subTasks.mapIndexed { index, subTask ->
            SubTaskEntity.fromDomain(subTask.copy(orderIndex = index), reminderId = reminder.id)
        }
        return reminderDao.insertOrUpdateReminderWithSubTasks(reminderEntity, subTaskEntities)
    }

    override suspend fun deleteReminder(reminderId: Long) {
        reminderDao.softDeleteReminder(reminderId, System.currentTimeMillis())
    }

    override suspend fun softDeleteReminder(reminderId: Long) {
        reminderDao.softDeleteReminder(reminderId, System.currentTimeMillis())
    }

    override suspend fun restoreReminder(reminderId: Long) {
        reminderDao.restoreReminder(reminderId)
    }

    override suspend fun permanentlyDeleteReminder(reminderId: Long) {
        reminderDao.permanentlyDeleteReminder(reminderId)
    }

    override suspend fun permanentlyDeleteAllDeletedReminders() {
        reminderDao.permanentlyDeleteAllDeletedReminders()
    }

    override fun getDeletedReminders(): Flow<List<Reminder>> {
        return reminderDao.getDeletedRemindersWithSubTasks().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun toggleReminderComplete(reminderId: Long, isCompleted: Boolean) {
        val completedAt = if (isCompleted) System.currentTimeMillis() else null
        reminderDao.updateCompletionStatus(reminderId, isCompleted, completedAt)
    }

    override suspend fun snoozeReminder(reminderId: Long, snoozeDurationMillis: Long) {
        val newDueTime = System.currentTimeMillis() + snoozeDurationMillis
        reminderDao.snoozeReminder(reminderId, newDueTime)
    }

    override suspend fun getActiveScheduledReminders(): List<Reminder> {
        val currentMillis = System.currentTimeMillis()
        return reminderDao.getActiveScheduledReminders(currentMillis).map { it.toDomain() }
    }

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveCategory(category: Category): Long {
        return categoryDao.insertCategory(CategoryEntity.fromDomain(category))
    }

    override suspend fun deleteCategory(categoryId: Long) {
        categoryDao.deleteCategoryById(categoryId)
    }
}
