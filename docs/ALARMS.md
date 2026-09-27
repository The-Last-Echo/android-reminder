# Alarm and reminder scheduling

This page documents the current local alarm model used by Reminder.

## Scheduler implementation

The scheduler abstraction is defined in `app/src/main/java/com/thelastecho/reminder/core/alarm/AlarmScheduler.kt` and implemented by:

- `AndroidAlarmScheduler`

The scheduler:

- accepts a `Reminder` and schedules it if it is due in the future and not already completed;
- cancels a reminder when it is deleted, completed, or no longer due;
- checks whether Android permits exact alarms.

## Exact alarm behavior

`AndroidAlarmScheduler` uses `AlarmManager.setExactAndAllowWhileIdle` when Android allows exact alarms. Otherwise it uses `setAndAllowWhileIdle`.

The app declares the necessary Android permission:

- `USE_EXACT_ALARM` on Android 13+
- `SCHEDULE_EXACT_ALARM` for Android 12 compatibility

The code wraps the scheduling call in a `try/catch` and falls back gracefully if a `SecurityException` occurs.

## Broadcast receiver flow

The alarm receiver is:

- `ReminderAlarmReceiver`

The boot receiver is:

- `BootReceiver`

On app or device startup, `BootReceiver` rehydrates scheduled alarms by loading active reminders from Room and re-scheduling them through the same local scheduler.

## Trigger and notification result

When the alarm fires, the app:

1. receives the local broadcast;
2. reads the reminder ID and reminder metadata from the intent extras;
3. resolves the notification style and default app preferences;
4. posts the reminder notification via `ReminderNotificationManager`;
5. starts the foreground alarm sound service when needed.

## Snooze

The current implementation of snoozing is intentionally simple and local:

- `SnoozeReminderUseCase` reads the existing reminder;
- it adds a fixed default of 10 minutes;
- it updates `dueDateTimeEpochMillis` and reschedules the reminder.

There is no cross-device or cloud snooze synchronization in the current code.

## Recurrence and completion

The reminder model includes a `RepeatInterval`, and the domain flow handles completion and rescheduling in the same local logic. The project does not currently provide a remote sync engine or a distributed recurrence state model.

## Audio and foreground service

Full-screen reminders use `AlarmSoundService` for playback. The service is a foreground `mediaPlayback` service and loops the configured or default alarm sound until the user completes, dismisses, or snoozes the reminder.

The service also keeps the reminder notification in the foreground so that the audible alert remains active while the user can act on it.

## Android-specific limitations

The current implementation is subject to platform constraints:

- exact alarms may be denied by the OS or permission state;
- full-screen intent access may be unavailable or revoked;
- high-priority or foreground alarm actions may be suppressed under device policy or user settings.

This is handled by the app via fallback notifications and safe scheduling logic rather than by a different remote infrastructure.

See also [docs/ANDROID_BEHAVIOR.md](ANDROID_BEHAVIOR.md), [docs/NOTIFICATIONS.md](NOTIFICATIONS.md), and [docs/DEBUGGING.md](DEBUGGING.md).
