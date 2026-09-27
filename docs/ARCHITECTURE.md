# Architecture

Reminder is a single-module Android app. The project is not split into separate Gradle modules for business logic; instead, Kotlin package boundaries separate the responsibilities inside `:app`.

## High-level runtime flow

```text
Compose screen
  -> ViewModel/UI state
  -> use case
  -> repository
  -> Room DAO / DataStore
  -> alarm, notification, or backup handling
```

A typical reminder lifecycle is:

- the user creates or edits a reminder in a Compose screen;
- the ViewModel dispatches an intent to a domain use case;
- the use case calls the repository API;
- the repository updates the Room database and possibly schedules or cancels an alarm;
- the notification manager posts a reminder notification when Android delivers the alarm.

## Main package responsibilities

- `presentation`: screens, navigation, ViewModels, widgets, and shared UI components
- `domain`: reminder models, repository interfaces, and use cases that encapsulate business rules
- `data`: Room database, DAO access, repository implementations, backup logic, and WorkManager jobs
- `core`: platform concerns such as alarms, notifications, preferences, debug logging, distribution flavor logic, and theming

## Data flow and persistence

The source of truth for reminders is Room. DataStore stores app-level preferences such as theme mode, notification default style, alarm sound, retention preference, and update-check settings.

The repository layer converts Room entities into domain models and exposes read/update operations used by the UI and use cases. Deletion is a soft delete flow: reminders are marked as deleted, given `deletedAt` and `expiresAt`, and purged later by WorkManager and app-start cleanup.

## Alarm and notification path

The reminder scheduling flow is:

1. `SaveReminderUseCase` persists the reminder and calls the alarm scheduler.
2. `AndroidAlarmScheduler` creates a pending broadcast using `AlarmManager`.
3. `ReminderAlarmReceiver` handles the alarm and posts a notification.
4. `ReminderNotificationManager` chooses the correct channel, optional full-screen activity, and action buttons.
5. `AlarmSoundService` manages the foreground media-playback service used by full-screen reminders.

This path is intentionally local-only. There is no remote sync engine in the current code. The only non-local feature in `online` is GitHub release check logic.

## Distribution flavors

The build defines the `offline` and `online` flavors. The selection happens at build time via `DistributionFeaturesFactory`:

- `offline`: `UpdateCheckController` is unavailable; `syncEngine` reports `Unavailable`.
- `online`: update checks are enabled and scheduled via WorkManager, but the sync engine is still `NotConfigured`.

This separation exists for distribution and optional update checks, not for online reminder synchronization.

## Build and platform constraints

The app targets Android 8.0+ and compiles with JDK 21 and Android SDK 36. The manifest declares exact-alarm permissions, notification permission, foreground media-playback service permission, boot completion receiver, and local app widget entries.

## Important architectural choices

- the project keeps a single codebase and selects flavor-specific behavior at compile time;
- the app prioritizes local-first operation and offline resilience;
- the notification system is intentionally platform-aware and may fall back when Android blocks full-screen access;
- backup and restore use a user-selected SAF destination and remain explicit local data portability features, not remote sync;
- debug instrumentation is present only in debug builds.

## Notable files

- `app/build.gradle.kts` — Gradle configuration, SDK levels, flavors, signing, and dependencies
- `app/src/main/java/com/thelastecho/reminder/data/local/ReminderDatabase.kt` — Room database definition and migrations
- `app/src/main/java/com/thelastecho/reminder/core/alarm/AndroidAlarmScheduler.kt` — exact alarm scheduling
- `app/src/main/java/com/thelastecho/reminder/core/notification/ReminderNotificationManager.kt` — notification and channel management
- `app/src/main/java/com/thelastecho/reminder/data/backup/ReminderArchiveRepository.kt` — archive export/import and validation
- `app/src/main/java/com/thelastecho/reminder/data/backup/ReminderBackupWorker.kt` — offline periodic backup scheduling
- `app/src/main/java/com/thelastecho/reminder/data/attachments/AttachmentStore.kt` — app-private photo storage and legacy URI migration
- `app/src/offline/java/.../DistributionFeaturesFactory.kt` and `app/src/online/java/.../DistributionFeaturesFactory.kt` — variant-specific behavior

See also [docs/OFFLINE_ONLINE.md](OFFLINE_ONLINE.md), [docs/DATABASE.md](DATABASE.md), [docs/NOTIFICATIONS.md](NOTIFICATIONS.md), and [docs/ALARMS.md](ALARMS.md).
