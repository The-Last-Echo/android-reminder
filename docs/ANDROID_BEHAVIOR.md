# Android behavior and user data

## Reminders and exact alarms

Reminder scheduling continues to use the app's `AlarmManager` implementation. On Android 12 and later, it checks `canScheduleExactAlarms()` and uses an exact alarm when allowed; otherwise it schedules an inexact `setAndAllowWhileIdle` alarm. Android 13+ declares `USE_EXACT_ALARM`; Android 12 uses the compatibility `SCHEDULE_EXACT_ALARM` permission. The app does not open an Alarms & reminders settings page at startup. See Android's [exact alarm guidance](https://developer.android.com/develop/background-work/services/alarms) and [Android 14 exact alarm changes](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms).

## Notification presentation and sound

The selected presentation mode, including a per-reminder override, determines the notification path:

- **Simple** uses a low-importance channel.
- **Heads-up** uses a high-importance channel and remains subject to the user's channel settings and notification permission.
- **Full screen** attempts a full-screen intent only when `NotificationManager.canUseFullScreenIntent()` allows it. Otherwise Android receives the high-importance notification fallback.
- **Disabled** suppresses the reminder notification.

Android 14 restricts full-screen intent access to eligible calling/alarm experiences, and users or the distribution store can deny it. Reminder does not request display-over-other-apps or notification-policy access. See [Android 14 full-screen intent restrictions](https://developer.android.com/about/versions/14/behavior-changes-14#secure-full-screen-intents).

For Full screen mode, Reminder starts a `mediaPlayback` foreground service from the alarm receiver. It loops the selected system alarm sound (or the system default) until the user dismisses, completes, or snoozes the reminder. The service notification uses the same notification ID as the reminder card, so it replaces that card instead of adding a duplicate. Android allows this background-service start when an exact user-requested alarm fires; if Android rejects a start, Reminder falls back to the regular notification. An inexact alarm may be denied that background start. If Android permits the full-screen reminder Activity, it retries playback from that visible Activity; if full-screen presentation is unavailable and the user does not open the notification, the regular notification is the supported fallback. See [foreground-service start exemptions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start) and [media playback service requirements](https://developer.android.com/develop/background-work/services/fgs/service-types#media).

Android 13+ notification permission is requested once. If it is denied, a contextual link to the app's Android notification settings appears in Settings. Notification channels can still be muted or changed by Android or the user.

## Trash and completed reminders

Manually deleted reminders keep their persisted `deletedAt` and `expiresAt` timestamps and are permanently purged 90 days after deletion. The optional completed-reminder setting moves completed reminders to Trash after the selected number of days, measured from `completedAt`; it never deletes active reminders. Daily WorkManager work and an app-start sweep perform the cleanup. A reminder in Trash follows the same 90-day retention after it enters Trash.

## Backup format and restore

Backups use UTF-8 JSON with format name `the-last-echo-reminder-backup` and integer `version: 1`. A backup contains stable reminder/category/subtask IDs, completion/deletion timestamps, reminder notification styles, relevant appearance/notification preferences, and photo bytes embedded as Base64 attachments. Each attachment is limited to 20 MiB and the complete file to 50 MiB.

The complete file is parsed and validated before restore writes. **Merge** keeps existing records with matching IDs, imports new records and their subtasks, remaps a colliding subtask ID when its parent reminder is new, and reports skipped records/conflicts. Existing preferences remain unchanged in Merge. **Replace** requires confirmation and changes Room data inside one Room transaction; reminder-related preferences are restored with rollback if the Room transaction fails. Attachments are staged before the transaction and removed if restore fails. Replace also removes old app-owned attachment files after success; it does not delete files selected from outside the app's private attachment directory.

## Update checks and privacy

Update checks are off by default. A manual check or enabled daily WorkManager check reads the latest public GitHub release metadata, with a 24-hour cache interval. The request contains no reminder data. Network requests still disclose ordinary connection metadata to GitHub. The manifest's `INTERNET` and `ACCESS_NETWORK_STATE` permissions support these checks and their WorkManager network constraint. Backups are written only to the destination the user chooses in Android's file picker.

Reminder rows, categories, subtasks and settings are stored locally in Room and DataStore. Photos are selected using Android's Photo Picker and retained as the selected URI until the user removes or replaces the photo. There is no analytics or cloud sync feature.

## Widgets and themes

Three Jetpack Glance home-screen widgets show today's reminders, upcoming reminders, or a compact active list. Selecting a reminder opens it in the app. Launcher refresh is periodic; visual behavior and refresh timing can vary by launcher. Widget background follows the app's System/Light/Dark/AMOLED preference when rendered.

## Language support

The Android per-app language settings declare English, French, Italian, German, Spanish, Japanese, Simplified Chinese, and Arabic. Android 13+ opens its native per-app language settings; older Android versions follow the system language. Arabic uses Android’s RTL configuration and Compose’s layout-direction support.
