# Internal domain API

This Android project does not expose a network or public SDK API. This page documents the principal Kotlin contracts used inside `:app`.

## Reminder model

`domain/model/Reminder.kt` defines reminders. `notificationStyle` is an optional notification-style enum name stored as a nullable string; `null` means use the app-wide preference. `imageUri` is an Android Photo Picker content URI.

## Repository

`domain/repository/ReminderRepository.kt` provides Flow queries and suspend operations for reminders and categories. `data/repository/ReminderRepositoryImpl.kt` maps these calls to Room. `deleteReminder` is a soft delete; `permanentlyDeleteReminder` and `permanentlyDeleteAllDeletedReminders` remove data permanently. Expired items are purged using `permanentlyDeleteExpiredReminders(nowMillis)`, which compares the supplied current time with each row’s stored expiration timestamp.

## Use cases

- `GetRemindersUseCase`: streams reminders for ALL, TODAY, SCHEDULED, OVERDUE, or COMPLETED filters.
- `SaveReminderUseCase`: saves a reminder and schedules/cancels its alarm as appropriate.
- `DeleteReminderUseCase`: moves a reminder to Trash and cancels its alarm.
- `RestoreReminderUseCase`: restores the same record and reschedules its future alarm.
- `ToggleReminderCompleteUseCase`: updates completion and recurrence.
- `SnoozeReminderUseCase`: shifts the due time and reschedules the reminder.

## Notification styles

`NotificationStyle` is declared in `core/preferences/UserPreferencesRepository.kt`: `SIMPLE`, `HEADS_UP`, `FULL_SCREEN`, and `NONE`. Settings define the default; a reminder may override that default. Notification presentation is still constrained by Android notification channels, notification permission, and full-screen intent access. Full screen uses a `mediaPlayback` foreground service and falls back to a regular notification when Android rejects the service or full-screen intent.

## Backup

`data/backup/ReminderBackupRepository.kt` reads and writes format `the-last-echo-reminder-backup`, version 1. It validates all records and attachments before mutation; Replace uses a Room transaction, while Merge inserts missing IDs and retains existing rows. The JSON stores photo bytes as Base64, not temporary picker URIs.

## Database

Room schema version is 4. Migrations 1→2 add soft-delete fields; migration 2→3 adds the per-reminder notification style; migration 3→4 adds the expiration timestamp used by Trash cleanup. See `data/local/ReminderDatabase.kt` and `data/local/dao/ReminderDao.kt` for the concrete schema and queries.
