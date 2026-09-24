# Architecture

Reminder is a single-module Android application. Package boundaries organize the code into presentation, domain, data, and platform infrastructure; these are Kotlin packages, not separate Gradle modules.

## Runtime flow

```text
Compose screen -> ViewModel intent -> use case -> repository -> Room DAO
       ^                                                    |
       +------------- Flow-backed UI state -----------------+
```

One-off navigation and snackbar events are emitted separately from persistent UI state. Screens collect ViewModel state with Compose. The Home screen filters the Room-backed reminder stream by date, category, completion, and search query.

## Packages

- `presentation/home`, `editor`, `settings`: Compose UI and screen state contracts.
- `presentation/navigation`: Navigation Compose routes and manual object construction.
- `domain/model`: Reminder, category, priority, repeat interval, and checklist models.
- `domain/usecase`: Save, delete, query, completion, snooze, and restore operations.
- `domain/repository`: Repository interface used by the domain and presentation layers.
- `data/local`: Room database, DAOs, persistence entities, Trash retention worker, and optional release-check worker.
- `data/backup`: validated versioned JSON export/restore with Room transactions and attachment staging.
- `presentation/widget`: Jetpack Glance home-screen widgets.
- `data/repository`: maps entities to domain models and implements repository operations.
- `core/alarm`: schedules exact alarms when Android permits them and restores alarms on boot/package replacement.
- `core/notification`: style-specific channels, photo previews, notification actions, and the optional `mediaPlayback` foreground service for Full screen mode.
- `core/preferences`: DataStore preferences for appearance, notification style/sound, retention, button position, and optional update checks.
- `core/designsystem`: Compose Material 3 colors, typography, and theme.

## Persistence

Room database schema version is declared in `ReminderDatabase`. Migrations preserve existing data when schema fields change. Reminders and subtasks are stored locally. A soft delete sets `isDeleted`, `deletedAt`, and `expiresAt` (90 days after deletion); expired trash rows are purged by a daily WorkManager job, with an additional sweep at application startup. Completed reminders are moved into Trash only when the configured retention period elapses.

## Notifications

`SaveReminderUseCase` passes scheduled reminders to `AndroidAlarmScheduler`. The alarm intent carries the reminder text, image URI, priority, and optional per-reminder notification style. If no reminder-level style is selected, the notification manager uses the DataStore default. Android notification channel importance and full-screen access remain subject to OS/user settings.

## Theme and language

`MainActivity` keeps the Android splash screen visible until DataStore emits appearance preferences, so the first Compose frame uses the saved theme. Light/dark and AMOLED appearance are applied by `ReminderTheme`. English, French, Italian, German, Spanish, Japanese, Simplified Chinese, and Arabic are declared in the app locale configuration; Android 13+ provides the native per-app language settings UI. Arabic follows the system RTL layout direction.

See [Android behavior and user data](ANDROID_BEHAVIOR.md) for notification restrictions, backup semantics, optional network behavior, Trash retention, and widget details.

## Build configuration

The project uses Kotlin 2.1, AGP 8.7, Compose, Room/KSP, Coroutines/Flow, and DataStore. SDK levels, dependencies, and versions are configured in `app/build.gradle.kts` and `gradle/libs.versions.toml`.
