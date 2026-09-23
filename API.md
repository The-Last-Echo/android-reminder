# API Documentation

This document describes the main use cases and repository interfaces in the Reminder application.

## Domain Models

### Reminder
```kotlin
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
    val subTasks: List<SubTask> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val deletedAt: Long? = null
)
```

### Category
```kotlin
data class Category(
    val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val iconName: String
)
```

### Priority
- `HIGH` (level 3)
- `MEDIUM` (level 2)
- `LOW` (level 1)
- `NONE` (level 0)

### RepeatInterval
- `ONCE` - No repetition
- `DAILY` - Every day
- `WEEKDAYS` - Monday to Friday
- `WEEKLY` - Every week
- `MONTHLY` - Every month
- `YEARLY` - Every year

## Use Cases

### GetRemindersUseCase
Retrieves reminders based on current filter state.

```kotlin
class GetRemindersUseCase(
    private val repository: ReminderRepository
) {
    operator fun invoke(filter: ReminderFilter): Flow<List<Reminder>>
}
```

### SaveReminderUseCase
Saves or updates a reminder, including subtasks and alarm scheduling.

```kotlin
class SaveReminderUseCase(
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(reminder: Reminder): Long
}
```

### DeleteReminderUseCase
Soft deletes a reminder (moves to trash) and cancels its alarm.

```kotlin
class DeleteReminderUseCase(
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(reminderId: Long)
}
```

### ToggleReminderCompleteUseCase
Toggles reminder completion status and manages recurrence.

```kotlin
class ToggleReminderCompleteUseCase(
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(reminderId: Long, isCompleted: Boolean)
}
```

### SnoozeReminderUseCase
Snoozes a reminder by a specified duration.

```kotlin
class SnoozeReminderUseCase(
    private val repository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(reminderId: Long, snoozeDurationMillis: Long)
}
```

## Repository Interface

### ReminderRepository
Main interface for reminder data operations.

```kotlin
interface ReminderRepository {
    // Query operations
    fun getAllReminders(): Flow<List<Reminder>>
    fun getActiveReminders(): Flow<List<Reminder>>
    fun getCompletedReminders(): Flow<List<Reminder>>
    fun getRemindersByCategory(categoryId: Long): Flow<List<Reminder>>
    fun getReminderById(id: Long): Flow<Reminder?>
    suspend fun getReminderByIdOnce(id: Long): Reminder?
    fun getDeletedReminders(): Flow<List<Reminder>>
    
    // Write operations
    suspend fun saveReminder(reminder: Reminder): Long
    suspend fun deleteReminder(reminderId: Long)
    suspend fun softDeleteReminder(reminderId: Long)
    suspend fun restoreReminder(reminderId: Long)
    suspend fun permanentlyDeleteReminder(reminderId: Long)
    suspend fun permanentlyDeleteAllDeletedReminders()
    
    // State operations
    suspend fun toggleReminderComplete(reminderId: Long, isCompleted: Boolean)
    suspend fun snoozeReminder(reminderId: Long, snoozeDurationMillis: Long)
    suspend fun getActiveScheduledReminders(): List<Reminder>
    
    // Category operations
    fun getAllCategories(): Flow<List<Category>>
    suspend fun saveCategory(category: Category): Long
    suspend fun deleteCategory(categoryId: Long)
}
```

## Alarm Scheduler

### AlarmScheduler
Interface for scheduling and canceling alarms.

```kotlin
interface AlarmScheduler {
    fun schedule(reminder: Reminder)
    fun cancel(reminderId: Long)
    fun canScheduleExactAlarms(): Boolean
}
```

### AndroidAlarmScheduler
Implementation using Android AlarmManager.

- Uses `setExactAndAllowWhileIdle` for precise timing
- Falls back to `setAndAllowWhileIdle` if exact alarms not available
- Handles security exceptions gracefully

## User Preferences

### AppThemeSettings
```kotlin
data class AppThemeSettings(
    val darkThemeConfig: DarkThemeConfig,
    val isAmoledMode: Boolean,
    val useDynamicColors: Boolean,
    val notificationStyle: NotificationStyle
)
```

### DarkThemeConfig
- `FOLLOW_SYSTEM` - Use system theme
- `LIGHT` - Force light theme
- `DARK` - Force dark theme

### NotificationStyle
- `SIMPLE` - Standard notification
- `FULL_SCREEN` - Full screen overlay
- `HEADS_UP` - Heads-up notification

## Reminder Filters

### ReminderFilter
Used for filtering reminders in the UI.

- `ALL` - All active reminders
- `TODAY` - Reminders due today
- `SCHEDULED` - Future scheduled reminders
- `COMPLETED` - Completed reminders
- `OVERDUE` - Overdue reminders

## Database Migration

### Migration 1→2
Added soft delete functionality:
- `isDeleted` column (boolean)
- `deletedAt` column (timestamp)

This migration enables the trash feature while preserving existing data.
