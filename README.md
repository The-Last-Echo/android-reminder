# Reminder for Android

Reminder is a local-first Android reminder app built with Kotlin and Jetpack Compose. Reminder data is stored on device in a Room/SQLite database. The app has no account or sync service.

## Features

- Schedule one-time or repeating reminders (daily, weekdays, weekly, monthly, yearly).
- Mark reminders complete, snooze them for ten minutes, and use quick actions from notifications.
- Organize reminders with categories, priorities, and checklists.
- Attach a photo from Android Photo Picker; scheduled alerts can show it as a large notification image.
- Select a notification style globally in Settings or override it on an individual reminder.
- Move deleted reminders to Trash, restore them, search them, or delete them permanently. Trash items are permanently purged after 90 days by a daily background cleanup.
- Choose light, dark, or system theme, with optional AMOLED black and dynamic colors.
- Choose English or French using Android's per-app language settings on Android 13 and later.
- Restore scheduled alarms after device restart.

## Requirements

- Android 8.0 (API 26) or later.
- JDK 21.
- Android SDK Platform 36 to build the current target.

## Build

```sh
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Run unit tests with `./gradlew testDebugUnitTest`.

## Architecture

The Android app is a single Gradle module (`:app`). Source is grouped by responsibility under `app/src/main/java/com/thelastecho/reminder/`:

- `presentation/`: Compose screens, ViewModels, and Navigation Compose graph.
- `domain/`: reminder/category models, repository contract, and use cases.
- `data/`: Room entities, DAOs, database migrations, and repository implementation.
- `core/`: AlarmManager integration, notifications, DataStore preferences, and design system.

The UI uses intent/state/effect contracts and Kotlin Flow. Dependencies are assembled manually in `NavGraph`; there is no dependency injection framework.

## Privacy and permissions

Reminder content and preferences are stored locally. Photo selection uses Android Photo Picker rather than broad gallery access. The manifest declares notification, alarm, reboot, vibration, and full-screen notification permissions as needed for reminder delivery. Android may require the user to allow notifications and exact alarms; full-screen alert availability is controlled by Android and device settings.

## Localization

English and French resource sets are in `app/src/main/res/values/` and `values-fr/`. Android 13+ exposes the app language through the system's per-app language screen. On earlier Android versions, the app follows the system language.

## License

GNU General Public License v3.0. See [LICENSE](LICENSE).
