# Reminder

Reminder is an offline-first Android app for scheduled reminders and checklists. Reminder data and preferences are stored locally.

## Features

- Create reminders with notes, dates, repeats, categories, photos, and subtasks.
- Complete reminders from the app or from notification actions; snooze an alert for ten minutes.
- Choose a default notification presentation or override it per reminder. Android may limit heads-up and full-screen presentation according to notification channel settings, permissions, and device policy.
- Search and filter reminders, including completed items.
- Soft-delete reminders, restore them from Trash, and permanently remove them after 90 days.
- Choose light, dark, system, or AMOLED appearance. The app supports English and French using Android per-app language settings.

## Requirements

- Android 8.0 (API 26) or later.
- JDK 21.
- Android SDK Platform 36 to build the current target.

## Build

Use Android Studio or a local Android SDK and network access for Gradle dependency resolution.

```sh
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Debug builds are debuggable and use the separate `com.thelastecho.reminder.debug` application ID, so they can be installed alongside a release build. Release builds enable code/resource shrinking and use the production application ID; signing credentials are read from `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` when configured. There is no “Basic” build mode in this repository.

Set `appVersionCode` and `appVersionName` in `gradle.properties` before making a release. The Settings screen reads the installed package `versionName`.

## Android behavior

Notifications and exact alarms require Android permissions or user settings depending on OS version. Full-screen intents are restricted by Android and may require explicit user enablement; this app does not request display-over-other-apps permission. DND bypass is optional and requires Android notification policy access. Notification channels are controlled by users after creation.

On Android 13 and later, choose the app language from Android’s per-app language settings. On older versions the app follows the system language.

## Project layout

- `app/src/main/java/.../domain`: reminder models, repository interfaces, and use cases.
- `app/src/main/java/.../data`: Room database and repository implementation.
- `app/src/main/java/.../core`: alarms, notifications, preferences, and design system.
- `app/src/main/java/.../presentation`: Compose screens and view models.
- `docs/`: architecture, API/data model notes, and changelog.

See [Architecture](docs/ARCHITECTURE.md), [API and data model](docs/API.md), [Changelog](docs/CHANGELOG.md), and [Contributing](CONTRIBUTING.md). Security reports: [SECURITY.md](SECURITY.md).

## License

GNU General Public License v3.0. See [LICENSE](LICENSE).
