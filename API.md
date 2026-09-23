# Internal domain API

This Android project does not expose a network or public SDK API. This page documents the principal Kotlin contracts used inside `:app`.

## Reminder model

`domain/model/Reminder.kt` defines reminders. `notificationStyle` is an optional notification-style enum name stored as a nullable string; `null` means use the app-wide preference. `imageUri` is an Android Photo Picker content URI.

## Repository

`domain/repository/ReminderRepository.kt` provides Flow queries and suspend operations for reminders and categories. `data/repository/ReminderRepositoryImpl.kt` maps these calls to Room. `deleteReminder` is a soft delete; `permanentlyDeleteReminder` and `permanentlyDeleteAllDeletedReminders` remove data permanently. Expired items are purged using `permanentlyDeleteExpiredReminders(cutoffMillis)`.

## Use cases

- `GetRemindersUseCase`: streams reminders for ALL, TODAY, SCHEDULED, OVERDUE, or COMPLETED filters.
- `SaveReminderUseCase`: saves a reminder and schedules/cancels its alarm as appropriate.
- `DeleteReminderUseCase`: moves a reminder to Trash and cancels its alarm.
- `ToggleReminderCompleteUseCase`: updates completion and recurrence.
- `SnoozeReminderUseCase`: shifts the due time and reschedules the reminder.

## Notification styles

`NotificationStyle` is declared in `core/preferences/UserPreferencesRepository.kt`: `SIMPLE`, `HEADS_UP`, and `FULL_SCREEN`. Settings define the default; a reminder may override that default. Notification presentation is still constrained by Android notification channels, notification permission, and full-screen intent access.

## Database

Room schema version is 3. Migrations 1→2 add soft-delete fields; migration 2→3 adds a nullable per-reminder notification-style field. See `data/local/ReminderDatabase.kt` and `data/local/dao/ReminderDao.kt` for the concrete schema and queries.
