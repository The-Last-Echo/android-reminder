# Development guide

This guide explains how the project is organized and where the main engineering work happens today.

## Repository layout

- [app/build.gradle.kts](../app/build.gradle.kts) — app module configuration, SDK, flavors, dependencies, and build settings
- [gradle/libs.versions.toml](../gradle/libs.versions.toml) — version catalog for Android, Compose, Room, DataStore, and tests
- [app/src/main/java/com/thelastecho/reminder](../app/src/main/java/com/thelastecho/reminder) — application code
- [app/src/offline/java/com/thelastecho/reminder](../app/src/offline/java/com/thelastecho/reminder) — offline flavor behavior
- [app/src/online/java/com/thelastecho/reminder](../app/src/online/java/com/thelastecho/reminder) — online flavor behavior
- [app/src/debug/java/com/thelastecho/reminder](../app/src/debug/java/com/thelastecho/reminder) — debug-only UI and diagnostics
- [app/src/release/java/com/thelastecho/reminder](../app/src/release/java/com/thelastecho/reminder) — release override for diagnostics
- [app/src/test/java/com/thelastecho/reminder](../app/src/test/java/com/thelastecho/reminder) — unit tests

## Where to work

### UI and screens

The Compose screens live under:

- `app/src/main/java/com/thelastecho/reminder/presentation/home`
- `app/src/main/java/com/thelastecho/reminder/presentation/editor`
- `app/src/main/java/com/thelastecho/reminder/presentation/settings`
- `app/src/main/java/com/thelastecho/reminder/presentation/navigation`
- `app/src/main/java/com/thelastecho/reminder/presentation/components`

These files define the user interface and UI state contracts.

### Business logic

The domain layer sits under:

- `app/src/main/java/com/thelastecho/reminder/domain/model`
- `app/src/main/java/com/thelastecho/reminder/domain/usecase`
- `app/src/main/java/com/thelastecho/reminder/domain/repository`

Modify a reminder rule or behavior here when it affects the product logic rather than a UI concern.

### Persistence and database

The database and Room logic are under:

- `app/src/main/java/com/thelastecho/reminder/data/local`
- `app/src/main/java/com/thelastecho/reminder/data/local/entity`
- `app/src/main/java/com/thelastecho/reminder/data/local/dao`

This is the place for schema changes, migrations, backup-related queries, and retention work.

### Notifications and alarms

Platform integration is in:

- `app/src/main/java/com/thelastecho/reminder/core/alarm`
- `app/src/main/java/com/thelastecho/reminder/core/notification`
- `app/src/main/java/com/thelastecho/reminder/core/preferences`

These classes handle `AlarmManager`, exact alarm scheduling, notification channels, foreground service playback, and settings persistence.

### Localization

Translations and locale config are under:

- `app/src/main/res/values`
- `app/src/main/res/values-*`
- `app/src/main/res/xml/locales_config.xml`

The application declares eight locales in the manifest configuration.

### Widgets

The Glance widgets live under:

- `app/src/main/java/com/thelastecho/reminder/presentation/widget`

These are Android app widgets, not a separate module.

## Development workflow

1. Keep the local-first behavior intact.
2. Prefer domain/use case changes over direct UI mutation.
3. Reuse existing repository and database flows rather than adding new storage layers.
4. When changing a migration, verify compatibility with existing local data.
5. Preserve the existing separation between offline and online build behavior.

## Testing

The repository includes unit tests in `app/src/test/java`. To run them:

```bash
./gradlew testOfflineDebugUnitTest testOnlineDebugUnitTest
```

The tests cover backup validation, reminder use cases, preferences, and debug tracing.

## Good practices for this project

- do not add network dependencies in the offline path;
- keep reminder scheduling and notification behavior local and deterministic;
- respect Android notification APIs and permission constraints;
- avoid presenting planned sync/cloud features as implemented;
- update docs when changing behavior, build variants, or data persistence contracts.

## Changes that require extra care

- Room schema changes and new migration code
- notification channel defaults and full-screen logic
- alarm scheduling and boot-time rescheduling
- restore logic, attachment handling, and backup validation
- theme preference migration and app language changes

See also [docs/ARCHITECTURE.md](ARCHITECTURE.md), [docs/DATABASE.md](DATABASE.md), [docs/NOTIFICATIONS.md](NOTIFICATIONS.md), and [docs/OFFLINE_ONLINE.md](OFFLINE_ONLINE.md).
