# Changelog

Notable project changes are recorded here. Dates refer to repository release metadata.

## 1.5.0 - 2026-09-25

### Changed
- Redesigned Home with clearer reminder filters, category context, and improved empty and refresh states.
- Reorganized Add/Edit Reminder into clearer sections for information, scheduling, categories, subtasks, attachments, and advanced options.
- Reorganized Settings into grouped sections for general behavior, reminders, backup, notifications, appearance, privacy, and app information.
- Refined category management with a curated color palette, improved icon contrast, and clearer editing controls.
- Improved reminder items with clearer hierarchy for titles, dates, categories, notes, priority, and completion actions.
- Improved Trash with clearer retention guidance and urgency for reminders nearing permanent deletion.
- Grouped Backup/Restore and Privacy content into more readable, scrollable layouts while retaining existing actions and restore modes.
- Introduced shared design-system card/input shapes and section headings across redesigned screens.
- Applied saved Light, Dark, AMOLED, and dynamic color preferences to the full-screen alarm; refined its actions and long-text handling.
- Improved localization across all supported languages, including Arabic RTL layouts and newly translated redesign labels.
- Improved usability and accessibility with clearer action labels, larger reminder-item actions, selectable controls, and more consistent spacing and contrast.

## 1.4.0 - 2026-09-24

### Added
- Italian, German, Spanish, Japanese, Simplified Chinese, and Arabic app languages, including translated UI strings and plurals, native per-app language settings, and RTL support.
- Looping user-selected system alarm audio for Full screen reminders using an Android `mediaPlayback` foreground service, with complete, snooze, and dismiss actions.
- Versioned backup and restore for reminders, categories, subtasks, attachments, and included preferences, with merge/replace handling and validation.
- Three Home screen widgets for compact, today's, and upcoming reminders.
- Optional release update checks and a dedicated Privacy page.
- Expanded category colors and monochrome icon editing, with category icons in filters.
- Configurable completed-reminder retention, undo for deleted reminders, and left/right placement for the Home Add button.

### Changed
- Notification presentation now supports per-reminder style overrides, photo previews, and platform-aware full-screen fallback. Full screen alarm audio starts a typed foreground service and shares the reminder notification ID to avoid a duplicate card.
- Notification permission recovery, full-screen intent access, exact-alarm scheduling, and system notification controls follow Android APIs without DND or overlay access.
- Completed reminders can be retained for a configurable duration; trashed reminders are permanently purged after 90 days using persisted expiration timestamps.
- Home retains pull-to-refresh, adds search/undo and completed subtasks, and uses the configured Add button position.
- README and technical documentation describe build variants, Android behavior, privacy, backup/restore, widgets, and security reporting.

### Fixed
- Prevent notification audio foreground services from being restarted automatically after process death; alarm scheduling is restored independently after reboot.
- Keep notification and app-language settings aligned with native Android controls.

## 1.3.0 - 2026-09-24

### Added
- In-app snackbar undo for recently deleted reminders, restoring their prior record and rescheduling future alarms; restored scheduled reminders are rescheduled.
- Notification styles map to Android channels; priority continues to affect notification appearance and vibration.
- Settings shortcuts for notification, full-screen intent, and native app-language settings.
- Search focus and keyboard opening, a visible refresh control, subtle list-item transitions, and tappable subtasks in reminder cards.
- Photo preview while editing a reminder.
- Expiration timestamps in Room and a live Trash countdown based on each stored timestamp.
- A root security reporting policy and reorganized project documentation.

### Changed
- Read the displayed app version from installed package metadata.
- Selecting AMOLED switches to dark mode; selecting Light disables AMOLED.
- Explain Debug and Release build behavior in the README.

### Fixed
- Backfill existing deleted reminders with a 90-day expiration during migration 3→4.

## 1.2.0 - 2026-09-23

### Fixed
- Refresh the complete Room reminder stream so completed reminders remain visible after filter changes and navigation.
- Keep the saved theme applied before the first app frame to avoid a light-mode flash.
- Use a separate lower-importance notification channel for simple notifications.

### Added
- Trash search and a 90-day deletion countdown; expired entries are purged by daily background work.
- A per-reminder notification-style override, with the settings value used as its default.
- Photo previews in scheduled reminder notifications and a dedicated full-screen alert activity.
- Android per-app language configuration for English, French, Italian, German, Spanish, Japanese, Simplified Chinese, and Arabic, opened through system settings.
- Database migration 2→3 for reminder-level notification style.

## 1.1.0 - 2026-09-23

- Added category and Trash screens, recurring reminders, notification actions, themes, and French resources.
- Added Room soft deletion and migration 1→2.
- Added notification-style preference and Home pull-to-refresh UI.
