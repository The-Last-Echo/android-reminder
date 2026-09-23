# Changelog

Notable project changes are recorded here. Dates refer to repository release metadata.

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
