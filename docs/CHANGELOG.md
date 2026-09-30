# Changelog

Notable project changes are recorded here. Dates refer to repository release metadata.

## 1.8.10 - 2026-09-30

### Changed
- Treat a count-limited repeat value as the total number of occurrences, include the final completion in persisted progress, and show progress such as 9/10 on reminders.
- Clarify repeat scheduling by grouping the interval number and unit in the editor, with translated labels across all supported languages.
- Refresh Home from a one-shot Room snapshot and explicitly bind the pull indicator to the refresh state.

### Fixed
- Prevent count-limited reminders from scheduling an extra occurrence and ensure the terminal completed count is retained in backups.
- Prevent pull-to-refresh from waiting indefinitely for a first emission from the reminder flow.

### Tests
- Cover the 9/10 and 10/10 completion boundaries, archive progress round-trip, and Home refresh snapshot behavior.

## 1.8.9 - 2026-09-29

### Added
- Add configurable reminder repetition intervals from minutes through years, with forever, occurrence-count, and end-date duration options.
- Add persistent reminder favorites, display them first on Home, and include their state in portable backups.
- Reorganize reminder creation around title, notes, photo, checklist, schedule, notification style, and category, with a completion confirmation animation.
- Localize the new reminder controls across all supported languages.

### Changed
- Extend the Room reminder schema with a backward-compatible migration for recurrence settings and favorites.

### Tests
- Cover custom minute intervals, recurrence count limits, and Home favorite ordering.

## 1.8.8 - 2026-09-28

### Added
- Rebuild the home-screen widgets with a responsive Today reminders widget and a fixed Quick Add shortcut.
- Add launcher-managed resizing and per-instance transparent-background configuration for the Today widget.
- Localize widget labels and configuration across all supported languages.

### Changed
- Remove the legacy Upcoming and Compact widgets and the in-app widget appearance settings.
- Refresh widgets when reminders, appearance settings, date/time, locale, or system appearance changes.

### Tests
- Make the weekday recurrence test use a date relative to the current day instead of a fixed date.

## 1.8.7 - 2026-09-27

### Added
- Allow manual backups to use a SAF destination independent of automatic backups, with a fallback to the existing destination until one is selected.
- Add destination-folder labels and dedicated automatic-backup encryption password setup, change, and disable controls.

### Changed
- Localize the new backup controls across all supported languages and document the separate destination behavior.

### Tests
- Cover independent manual/automatic destinations and verify manual backups do not read the automatic-backup password.

## 1.8.6 - 2026-09-27

### Added
- Add refreshed Today, Upcoming, and Compact Glance widgets with localized due-date/time details and reminder deep links.
- Add a quick-create action to the compact widget and widget appearance controls for theme colors and background opacity.
- Add widget settings navigation and localized widget labels across all supported languages.

### Changed
- Refresh widgets when reminders or appearance settings change, on relevant system environment changes, and periodically through the launcher.

### Fixed
- Keep undated and overdue active reminders visible in the compact widget, and refresh time-dependent lists as reminders become due.

## 1.8.5 - 2026-09-27

### Added
- Add portable `.reminderbackup` ZIP archives with optional whole-archive password encryption and cross-device restore.
- Add automatic local backups to a user-selected SAF folder with configurable frequency, automatic-only retention, and a device-protected password for unattended encrypted backups.
- Add immediate private photo copying and migration of readable legacy picker URIs to relative attachment paths.

### Changed
- Separate manual backup/restore from automatic backup settings; manual encryption is independently optional per export and never remembered.
- Reprogram imported reminder alarms after restore and report alarm scheduling failures.
- Replace the former JSON backup flow and document archive, SAF, encryption, and retention behavior.

### Tests
- Cover clear and encrypted archive round trips, password/tamper failures, manual and automatic password policies, automatic-only retention naming, and alarm scheduling reports.

## 1.8.4 - 2026-09-27

### Changed
- Standardize shared spacing, content-width, typography, and shape tokens across the main Compose UI.
- Center and constrain Home, reminder editing, and Settings content on wide screens while preserving the existing phone layout and interactions.
- Refine reminder-card density and settings surface contrast while retaining the 48 dp completion action target.

## 1.8.3 - 2026-09-27

### Changed
- Run Offline and Online Release unit tests for tag builds while retaining Debug tests for branches and pull requests.
- Select the previous changelog tag by creator date and package release APKs only for tag builds.

### Fixed
- Add the missing `many` plural form for retention-day strings in Spanish, French, and Italian.

## 1.8.2 - 2026-09-27

### Added
- Add content-free Debug logging across reminder alarm scheduling, receiver delivery, notification posting, Full-Screen fallback, foreground service startup, and audio playback.
- Document a device-based reminder diagnostic procedure and recommended reproduction matrix.

### Tests
- Add unit coverage for reminder diagnostic event formatting.

## 1.8.1 - 2026-09-27

### Fixed
- Avoid invoking Flow operators during composition when loading saved theme settings, and ensure alarm receivers finish pending asynchronous work if coroutine launch fails.
- Remove unused localized strings and colors, and reject backup subtasks with non-positive IDs during restore.

### Tests
- Add unit coverage for backup export limits and restore validation/merge/replace behavior, plus reminder deletion retention and snooze scheduling.

## 1.8.0 - 2026-09-26

### Architecture
- Split Android distribution into `offline` and `online` Gradle flavors, each with Debug and Release variants, while keeping the existing production application ID on `onlineRelease`.
- Both flavors continue to provide the same local reminder, alarm, notification, widget, and backup/restore behavior. No remote synchronization or WebDAV functionality is included yet.
- `online` retains the existing optional GitHub release checks. `offline` omits those controls and worker and declares neither `INTERNET` nor `ACCESS_NETWORK_STATE` in its merged manifest.
- CI now tests and builds both flavors and checks the packaged Offline manifests for network permissions.

## 1.7.0 - 2026-09-26

### Added
- Added a custom accent color picker alongside seven named accent presets, with Material You dynamic colors remaining an independent preference.
- Added explicit System, Light, Dark, and AMOLED theme modes, plus persisted appearance preferences.
- Added grouped Settings home cards and dedicated screens for general, appearance, reminders, notifications and alarms, data, privacy, and app information.
- Added an animated reminder-count badge shared by Home and Settings, including the Trash count on the Data card.
- Added a redesigned category editor with a broader icon selection and custom color selection.

### Changed
- Refined Home navigation, search dismissal, filter surfaces, reminder creation entry animation, and shared visual spacing and card treatments.
- Improved Settings navigation and transitions, appearance controls, notification and data screens, and Privacy readability.
- Improved touch target sizes and card outlines in reminder, editor, category, and Settings interactions.
- Updated translations for all eight supported languages.

## 1.6.0 - 2026-09-26

### Added
- Added an in-app Debug diagnostics panel for notification permission, effective reminder channels, channel settings, and full-screen intent access. Diagnostic UI and translations are excluded from Release builds.
- Added a direct Settings shortcut for granting Android full-screen notification access when the system reports it is unavailable.
- Added Delete to the active full-screen alarm controls and localized the alarm status, stop action, and permission guidance across supported languages.

### Changed
- Register all stable reminder notification channels at application startup while preserving existing user-configured channel settings.
- Use the full-screen reminder channel for the actionable alarm notification and keep the foreground-service channel separate.
- Move full-screen alarm screen state and actions into an MVI ViewModel, with reminder actions handled through the existing notification receiver flow.
- Harden alarm playback and notification handling for concurrent reminders, stale actions, asynchronous sound preparation, and service lifecycle changes.
- Ensure alarm delivery and reboot rescheduling receivers finish asynchronous work reliably, including initialization failures.

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
