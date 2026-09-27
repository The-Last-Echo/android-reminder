# Reminder

Reminder is a local-first Android app for scheduled reminders and checklists. The current implementation stores reminder data, categories, subtasks, attachments and preferences locally in Room and DataStore, and it continues to work without a remote server or account.

## What this project does today

Reminder currently focuses on four core responsibilities:

- local reminder creation and editing with scheduling, priority, subtasks, notes, categories, and photo attachments;
- exact or inexact local alarm scheduling with Android `AlarmManager` and local notification delivery;
- local backup/restore of reminders and related preferences in a portable JSON format;
- local settings, theming, retention, widgets, and diagnostics.

This is not a cloud-synced or multi-user application. The app can be built in two distribution variants:

- `offline`: local-only distribution with no network permissions in the manifest;
- `online`: same local reminder engine, with optional public GitHub release checks enabled.

## Offline / Online status

The project is intentionally split into an `offline` flavor and an `online` flavor, but the real online implementation does not add remote reminder synchronization. Today, `online` is a local app with extra GitHub update-check logic, not a sync platform.

## Main features implemented

- reminders with title, notes, due date/time, category, priority, subtasks, and attachments;
- local soft-delete + restore flow with a 90-day retention window and trash purge;
- recurrence semantics through the reminder model and scheduling logic already present in the app;
- multiple notification styles: simple, heads-up, full-screen, and disabled;
- local alarm scheduling and reboot rescheduling;
- local backups with validate-and-restore logic;
- Material 3 theme modes: System, Light, Dark, and AMOLED;
- per-app language support for English, French, Italian, German, Spanish, Japanese, Simplified Chinese, and Arabic;
- Jetpack Glance widgets for Today, Upcoming, and Compact views;
- debug diagnostics limited to debug builds.

## Current architecture

The project is a single Gradle module named `:app`, structured into packages rather than separate Android modules. The main layers are:

- `presentation`: Compose UI and ViewModels;
- `domain`: models, repository contracts, and use cases;
- `data`: Room database, DAOs, repositories, backups, and workers;
- `core`: alarms, notifications, preferences, debug tools, distribution features, and design system.

The app relies on Room, DataStore, WorkManager, Compose, and Android standard platform APIs.

## Tech stack

- Kotlin + Jetpack Compose
- Room + KSP
- DataStore Preferences
- WorkManager
- Material 3
- Android `AlarmManager`, notifications, foreground services, and app widget APIs

## Build and requirements

- Android 8.0+ (API 26)
- JDK 21
- Android SDK Platform 36
- Gradle via the repository wrapper

```sh
./gradlew testOfflineDebugUnitTest testOnlineDebugUnitTest
./gradlew assembleOfflineDebug assembleOnlineDebug
```

For release builds, the project reads signing information from environment variables when present, and `gradle.properties` provides the current `appVersionCode` and `appVersionName`.

## Documentation structure

This repository contains the product documentation in the `docs/` directory.

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — architecture, data flow, and package responsibilities
- [docs/BUILD.md](docs/BUILD.md) — build prerequisites, flavors, commands, and packaging details
- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) — working guide for contributors
- [docs/DEBUGGING.md](docs/DEBUGGING.md) — debug logging and diagnostic workflow
- [docs/NOTIFICATIONS.md](docs/NOTIFICATIONS.md) — notification channels and Android constraints
- [docs/ALARMS.md](docs/ALARMS.md) — scheduling, alarms, receivers, snooze, and boot recovery
- [docs/DATABASE.md](docs/DATABASE.md) — Room schema and persistence model
- [docs/BACKUP_RESTORE.md](docs/BACKUP_RESTORE.md) — backup format, validation, and restore behavior
- [docs/OFFLINE_ONLINE.md](docs/OFFLINE_ONLINE.md) — exact current state of both flavors
- [docs/UI.md](docs/UI.md) — interface and navigation overview
- [docs/THEMING.md](docs/THEMING.md) — theme system and appearance rules
- [docs/SECURITY.md](docs/SECURITY.md) — existing security posture and future recommendations
- [docs/ROADMAP.md](docs/ROADMAP.md) — factual roadmap and future intent
- [docs/ANDROID_BEHAVIOR.md](docs/ANDROID_BEHAVIOR.md) — Android platform constraints affecting reminders
- [docs/API.md](docs/API.md) — internal domain and persistence contracts
- [docs/CHANGELOG.md](docs/CHANGELOG.md) — project changelog

Additional project docs:

- [CONTRIBUTING.md](CONTRIBUTING.md)
- [SECURITY.md](SECURITY.md)
- [LICENSE](LICENSE)

## License

Reminder is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE).

## Current project state

This repository is in active development and documentation is aligned to the implemented codebase, not to future plans. The project is locally centric and intentionally avoids remote synchronization until it is explicitly designed and implemented.
