# Notifications

This document describes the reminder notification flow as it exists in the current codebase.

## Core implementation

The main implementation is in:

- `app/src/main/java/com/thelastecho/reminder/core/notification/ReminderNotificationManager.kt`
- `app/src/main/java/com/thelastecho/reminder/core/notification/AlarmSoundService.kt`
- `app/src/main/java/com/thelastecho/reminder/core/notification/NotificationActionReceiver.kt`

The app creates stable notification channels at startup and preserves any user configuration already on the device.

## Notification styles

`NotificationStyle` is defined in `UserPreferencesRepository` and currently includes:

- `SIMPLE`
- `HEADS_UP`
- `FULL_SCREEN`
- `NONE`

A reminder may override the default notification style. If a reminder does not provide one, the app-wide preference is used.

## Channel model

The notification manager creates three channels:

- `reminders_simple`
- `reminders_heads_up`
- `reminders_full_screen`

The effective channel depends on the chosen style. Android channel importance and user settings still apply. Notification channels are not a custom app-owned transport mechanism; they are Android OS channels.

## Full-screen behavior

When a reminder uses `FULL_SCREEN`, the code does the following:

- checks whether Android permits full-screen intent usage;
- starts the alarm foreground service;
- posts a high-priority notification or regular fallback if the system blocks full-screen access;
- opens `ReminderFullScreenActivity` if allowed.

The system can revoke full-screen access at any time. The app rechecks before posting and falls back to the Heads-up path when needed.

## Foreground service and sound

`AlarmSoundService` is a `Service` with `foregroundServiceType="mediaPlayback"` in the manifest. It is used to play an alarm tone while keeping the reminder visible and actionable.

The service:

- reads the configured alarm sound URI from DataStore or falls back to the system default alarm ringtone;
- sets `AudioAttributes` with `USAGE_ALARM` and loops playback;
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
