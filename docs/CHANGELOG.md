# Changelog

Notable project changes are recorded here. Dates refer to repository release metadata.

## 1.3.0 - 2026-09-24

### Added
- Undo recently deleted reminders from a notification; restored scheduled reminders are rescheduled.
- Notification styles and priority levels now use separate Android channels with behavior matched to the selected settings.
- Optional Do Not Disturb bypass for high-priority non-simple reminders through Android Notification Policy Access.
- Settings shortcuts for notification, exact alarm, full-screen intent, DND, and native app-language settings.
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
- Android per-app language configuration for English and French, opened through system settings.
- Database migration 2→3 for reminder-level notification style.

## 1.1.0 - 2026-09-23

- Added category and Trash screens, recurring reminders, notification actions, themes, and French resources.
- Added Room soft deletion and migration 1→2.
- Added notification-style preference and Home pull-to-refresh UI.
