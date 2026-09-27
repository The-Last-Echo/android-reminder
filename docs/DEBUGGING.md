# Debugging and diagnostics

Reminder includes a small debug-tracing system that logs alarm and notification events without exposing user text content.

## Debug trace mechanism

The main debug logger is `ReminderDebugTrace` in `app/src/main/java/com/thelastecho/reminder/core/debug/ReminderDebugTrace.kt`.

Behavior:

- it emits logs only when `BuildConfig.DEBUG` is true;
- it writes to the `ReminderDebug` log tag;
- it records a step name, optional reminder ID, optional state, and minimal extra context.

The logger intentionally avoids writing reminder titles, notes, or user content. This keeps diagnostics focused on scheduling and platform behavior instead of personal data.

## Relevant classes

The diagnostic flow is mainly attached to:

- `AndroidAlarmScheduler`
- `ReminderAlarmReceiver`
- `ReminderNotificationManager`
- `AlarmSoundService`
- `BootReceiver`

The debug logs cover:

- schedule request and result
- receiver delivery
- notification start and posting
- full-screen fallback decisions
- foreground service start and playback
- sound playback preparation and success/failure

## Developer diagnostic UI

The debug build includes a developer diagnostics view that reads notification state and Android permission/access results. This UI is defined under:

- `app/src/debug/java/com/thelastecho/reminder/presentation/settings/DeveloperNotificationDiagnostics.kt`

The release variant replaces it with a no-op implementation:

- `app/src/release/java/com/thelastecho/reminder/presentation/settings/DeveloperNotificationDiagnostics.kt`

This means the diagnostic panel is only available in debug builds.

## Main diagnostic path

The project documents a practical reproduction flow in [docs/DEBUG_REMINDER_DIAGNOSTICS.md](DEBUG_REMINDER_DIAGNOSTICS.md). A short summary is:

1. run the app in debug mode;
2. filter logcat with `adb logcat -s ReminderDebug`;
3. create a future reminder;
4. confirm the expected scheduling, delivery, notification, and playback events appear in order.

## What the diagnostics can reveal

The logs are helpful to answer questions such as:

- was the alarm scheduled?
- did the `AlarmManager` deliver it?
- was the notification posted?
- did Android block full-screen intent access?
- did the foreground media service start and play sound?

## Limits

These diagnostics are not a full application telemetry system. They are intentionally limited to:

- debug-only operational logs;
- Android scheduling and permission state;
- foreground service and notification lifecycle events;
- no user-content export beyond local debugging.

They are useful for troubleshooting reminders, but they are not a substitute for a real remote monitoring or sync observability layer.

See also [docs/ANDROID_BEHAVIOR.md](ANDROID_BEHAVIOR.md) and [docs/DEBUG_REMINDER_DIAGNOSTICS.md](DEBUG_REMINDER_DIAGNOSTICS.md).
