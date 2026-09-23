# Reminder

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![100% FOSS](https://img.shields.io/badge/FOSS-100%25-brightgreen.svg)](#privacy--foss-commitment)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-purple.svg)](https://kotlinlang.org)
[![Target SDK](https://img.shields.io/badge/Target_SDK-36_(Android_16)-teal.svg)](https://developer.android.com)
[![Build & Release](https://github.com/The-Last-Echo/android-reminder/actions/workflows/build-and-release.yml/badge.svg)](https://github.com/The-Last-Echo/android-reminder/actions/workflows/build-and-release.yml)

**Reminder** is a 100% Free and Open Source (FOSS) Android application written in Kotlin and Jetpack Compose. Designed as a privacy-respecting, lightweight alternative to proprietary solutions like *Samsung Reminder*, it runs completely locally on your device with **zero tracking**, **zero telemetry**, and **zero proprietary Google Play Services dependencies**.

---

## Features

- **Precise Scheduled Alarms**: Exact timing powered by `AlarmManager.setExactAndAllowWhileIdle`, guaranteeing timely alerts even in Doze mode.
- **Interactive Notification Actions**: Mark tasks as complete or snooze for 10 minutes directly from the notification shade without unlocking or opening the app.
- **Smart Recurring Reminders**: Supports Daily, Mon–Fri (skips weekends), Weekly, Monthly, and Yearly recurrences. Automatically computes and schedules the next occurrence upon completion.
- **Subtasks & Checklists**: Break down complex reminders into actionable checklists with individual progress tracking.
- **Categories**: Organize tasks by personal, work, shopping, or custom categories.
- **Priority Indicators**: High, Medium, Low, and Normal priority tags mapped to Material 3 Expressive tokens.
- **Privacy-First Photo Attachments**: Attaches images using Android's native scoped `PhotoPicker` (`PickVisualMedia`), eliminating any need for dangerous full-device storage permissions.
- **True AMOLED Pure Black Theme**: Optional `#000000` deep black background mode designed specifically for OLED/AMOLED battery preservation and high contrast, alongside standard light and dark modes.
- **Dynamic Material You**: Adapts accent colors automatically to system wallpapers on Android 12+.
- **Reboot Resilience**: Uses `RECEIVE_BOOT_COMPLETED` to seamlessly restore all active future alarms after a device restart.
- **Custom Categories**: Create and manage custom categories with colors in settings.
- **Trash & Restore**: Soft delete reminders and restore them from trash.
- **Notification Styles**: Choose between simple, full screen, or heads-up notification styles.
- **Multi-language Support**: Supports English and French, following system language settings.
- **Pull-to-Refresh**: Refresh the reminder list with pull-to-refresh gesture.

---

## Screenshots

| Home (AMOLED Dark) | Reminder Editor | Notification Actions | Settings |
|:---:|:---:|:---:|:---:|
| *(Screenshot Placeholder)* | *(Screenshot Placeholder)* | *(Screenshot Placeholder)* | *(Screenshot Placeholder)* |

---

## Privacy & FOSS Commitment

| Component | Standard / Proprietary App | Reminder (This App) |
| :--- | :--- | :--- |
| **Analytics & Telemetry** | Google Firebase, Crashlytics, Facebook SDK | **None (0% tracking)** |
| **Google Play Services** | Closed proprietary binaries required | **None (100% independent AOSP)** |
| **Data Storage** | Proprietary cloud sync or unencrypted DB | **Local SQLite database (Room)** |
| **Photo Access** | Requests `READ_MEDIA_IMAGES` / full gallery | **Scoped `PhotoPicker` (System API)** |
| **License** | Proprietary EULA / Closed | **GNU GPLv3 (Copyleft Free Software)** |

---

## Android Permissions Breakdown

Reminder strictly follows the **principle of least privilege**:

| Permission | Type | Justification |
| :--- | :--- | :--- |
| `android.permission.POST_NOTIFICATIONS` | Runtime (Dangerous) | Required on Android 13+ (API 33+) to alert you when your reminder is due. Prompted with an in-app explanation. |
| `android.permission.USE_EXACT_ALARM` | Normal / Special | Permitted on Android 13+ (API 33+) for core alarm/reminder applications to ring at the exact minute set by the user. |
| `android.permission.SCHEDULE_EXACT_ALARM` | Backward Compatibility | Constrained to `maxSdkVersion="32"` for backward compatibility on Android 12. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Normal (Install-time) | Restores scheduled alarms across device reboots so you never miss an alert. |
| `android.permission.VIBRATE` | Normal (Install-time) | Vibrates the phone when an alarm triggers. |

---

## Architecture & Tech Stack

- **Language**: Kotlin 2.1.0
- **UI Toolkit**: Jetpack Compose + Material 3 (Material Design 3 Expressive)
- **Architecture**: MVI (Model-View-Intent) with Unidirectional Data Flow (UDF)
  - `Presentation`: Composables, `ViewModel` emitting immutable `UiState`, handling `UiIntent`, streaming `UiEffect`.
  - `Domain`: Pure Kotlin entities (`Reminder`, `Category`, `SubTask`), UseCases (`GetRemindersUseCase`, `SaveReminderUseCase`, `ToggleReminderCompleteUseCase`, etc.).
  - `Data`: Room SQLite database (`ReminderDao`, `CategoryDao`), Repositories.
  - `Core`: Alarm engine (`AndroidAlarmScheduler`, `ReminderAlarmReceiver`, `BootReceiver`), notification manager, DataStore preferences.
- **Async & Reactive**: Kotlinx Coroutines & Flow (`StateFlow`, `SharedFlow`, `Channel`).
- **Testing**: JUnit 4, MockK, Kotlinx Coroutines Test, Turbine.

---

## Building & Compiling

### Prerequisites
- JDK 21 (Temurin or OpenJDK recommended)
- Android SDK with Platform 36 (Android 16) and Build-Tools 35.0.0+

### Clone the repository
```bash
git clone https://github.com/The-Last-Echo/android-reminder.git
cd android-reminder
```

### Run unit tests
```bash
./gradlew testDebugUnitTest
```

### Assemble debug APK
```bash
./gradlew assembleDebug
```
The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Assemble release APK
```bash
./gradlew assembleRelease
```

---

## CI / CD (GitHub Actions)

A preconfigured GitHub Actions workflow (`.github/workflows/build-and-release.yml`) handles:
- Automated compilation and unit testing on every push and pull request.
- Automatic APK builds (Debug & Signed Release) on Git release tags (`v*`).
- Automatic publishing of APK assets to GitHub Releases.

To enable automated release signing, add the following secrets in **GitHub > Settings > Secrets and variables > Actions**:
- `SIGNING_KEYSTORE` (Base64-encoded `.jks` / `.keystore` file)
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

---

## Contributing

Contributions are warmly welcome! Whether you are reporting bugs, proposing features, or submitting code improvements:
1. Fork the repository.
2. Create your feature branch (`git checkout -b feature/my-new-feature`).
3. Commit your changes (`git commit -m 'feat: add custom snooze options'`).
4. Ensure all unit tests pass (`./gradlew testDebugUnitTest`).
5. Push to the branch (`git push origin feature/my-new-feature`).
6. Open a Pull Request.

Please ensure all contributions respect the 100% FOSS ethos (no proprietary libraries or trackers).

---

## License

This project is licensed under the **GNU General Public License v3.0 (GPLv3)**.  
See the [LICENSE](LICENSE) file for details.
