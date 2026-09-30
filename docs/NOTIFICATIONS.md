# Notifications

This document describes the reminder notification flow as it exists in the current codebase.

## Core implementation

The main implementation is in:

- `app/src/main/java/com/thelastecho/reminder/core/notification/ReminderNotificationManager.kt`
- `app/src/main/java/com/thelastecho/reminder/core/notification/AlarmSoundService.kt`
- `app/src/main/java/com/thelastecho/reminder/core/notification/NotificationActionReceiver.kt`

The app creates stable notification channels at startup and preserves any user configuration already on the device.

## Notification levels

`NotificationStyle` is defined in `UserPreferencesRepository` and includes:

- `LIGHT`: high-importance heads-up notification with the system notification sound.
- `MEDIUM`: high-importance notification with a full-screen intent request and the system notification sound.
- `STRONG`: high-importance full-screen intent request, vibration, and looping alarm playback from `AlarmSoundService`.
- `NONE`

A reminder may override the app-wide default. A missing or unknown reminder value inherits the default. Legacy values are interpreted as `SIMPLE`/`HEADS_UP` to `LIGHT` and `FULL_SCREEN` to `STRONG`.

## Channel model

The notification manager creates three versioned channels:

- `reminders_light_v3`
- `reminders_medium_v2`
- `reminders_strong_v2`

Light uses a high-importance channel so it can appear as a heads-up notification. Medium and Strong also use high-importance channels. Light and Medium use the system's default notification sound. Strong is silent at the channel level to prevent overlapping playback; the foreground service owns its alarm sound. New channel IDs are used because Android does not let apps change sound or importance after a channel has been created. Android channel importance and user settings still apply.

## Full-screen behavior

Medium and Strong request `ReminderFullScreenActivity` through a full-screen `PendingIntent` when Android allows it. If either fires while the device is unlocked, the app opens the activity directly when it is foregrounded or the user has granted the optional `SYSTEM_ALERT_WINDOW` special access. The alarm activity hides system bars for an immersive, full-display experience. When locked, the app relies on Android's full-screen intent path. Without overlay access, Android may show a heads-up notification while the device is unlocked. The app:

- checks whether Android permits full-screen intent usage;
- keeps a high-importance notification when access is denied;
- relies on Android to choose between a full-screen activity and a heads-up notification when the device is actively in use and overlay access is not granted.
- never uses overlay access to bypass the secure lock screen; Android's full-screen alarm access handles that case.

The system can revoke full-screen access at any time. The app rechecks before posting. Background activity launch is used while unlocked only when the user explicitly grants Android's overlay special access.

## Foreground service and sound

`AlarmSoundService` is a `Service` with `foregroundServiceType="mediaPlayback"` in the manifest. It is used to play an alarm tone while keeping the reminder visible and actionable.

The service:

- reads the configured alarm sound URI from DataStore or falls back to the system default alarm ringtone;
- sets `AudioAttributes` with `USAGE_ALARM` and loops playback;
- is started for Strong alerts only; Medium and Light rely on their notification channel sound;
- keeps the reminder notification in the foreground while the sound plays;
- reacts to stop, complete, snooze, and delete actions.

## Actions

The reminder notification includes actions for:

- complete
- snooze
- delete
- dismiss (for the full-screen alarm notification)

These actions are routed through `NotificationActionReceiver`, which then calls the domain use case and reschedules or cancels the alarm as needed.

## Permissions and platform constraints

The manifest declares:

- `POST_NOTIFICATIONS`
- `USE_FULL_SCREEN_INTENT`
- `USE_EXACT_ALARM`
- `SCHEDULE_EXACT_ALARM` with `maxSdkVersion=32`
- `RECEIVE_BOOT_COMPLETED`
- `VIBRATE`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`

The app does not request overlay or notification-policy access. It relies on Android’s allowed channels and system policies. If Android denies full-screen intent access or notification permission, the app falls back to a supported presentation instead of performing a bypass.

## Important limitations

- Android can block full-screen intent access, especially on newer versions.
- Notification permission is required on Android 13+.
- The app can only control its own notification channels and behavior; the user and OS still decide channel importance and notification delivery.
- The app does not implement remote notifications, WebDAV, or a server inbox.

See also [docs/ANDROID_BEHAVIOR.md](ANDROID_BEHAVIOR.md), [docs/ALARMS.md](ALARMS.md), and [docs/DEBUGGING.md](DEBUGGING.md).
