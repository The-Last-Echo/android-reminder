# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-09-23

### Added
- **Custom Categories Management**: Added ability to create, edit, and delete custom categories with color selection in settings
- **Trash & Restore System**: Implemented soft delete functionality with trash bin to view and restore deleted reminders
- **Notification Styles**: Added three notification style options (Simple, Full Screen, Heads-up) configurable in settings
- **Multi-language Support**: Added French language support alongside English, following system language settings
- **Pull-to-Refresh**: Added pull-to-refresh gesture on home screen to manually refresh the reminder list
- **Category Management Screen**: New dedicated screen for managing categories with color picker
- **Trash Screen**: New dedicated screen for viewing and managing deleted reminders
- **Database Migration**: Implemented database migration (v1 to v2) to support soft delete functionality

### Fixed
- **Filter/Search Bug**: Fixed issue where selecting a category or performing search would prevent seeing other reminders even when starting a new search
- **Refresh Logic**: Corrected refresh mechanism to properly fetch all reminders instead of using filtered list

### Changed
- **Delete Behavior**: Changed delete operation from permanent deletion to soft delete (moves to trash)
- **Notification Manager**: Updated to support configurable notification styles based on user preferences
- **Database Schema**: Added `isDeleted` and `deletedAt` columns to reminders table
- **Query Filters**: Updated all database queries to exclude soft-deleted reminders by default
- **String Resources**: Moved hardcoded strings to resource files for proper internationalization

### Technical
- **Documentation**: Added comprehensive project documentation (ARCHITECTURE.md, CONTRIBUTING.md, API.md)
- **README**: Cleaned up README by removing emojis and updating feature list
- **Database Migration**: Added Room migration strategy for version 1 to 2
- **Repository Interface**: Extended with soft delete, restore, and permanent delete methods
- **Preference System**: Added notification style preference to user settings

### UI/UX
- **Settings Reorganization**: Reorganized settings screen with new sections for Categories, Data Management, and Notifications
- **Improved Navigation**: Added navigation to new category management and trash screens
- **Better Visual Feedback**: Added confirmation dialogs for destructive actions
- **Enhanced Empty States**: Improved empty state messages across different screens

## [1.0.0] - Initial Release

### Features
- Precise scheduled alarms with AlarmManager
- Interactive notification actions (Complete, Snooze)
- Smart recurring reminders (Daily, Mon-Fri, Weekly, Monthly, Yearly)
- Subtasks and checklists
- Default categories (Personal, Work, Shopping)
- Priority indicators (High, Medium, Low, Normal)
- Privacy-first photo attachments via PhotoPicker
- AMOLED pure black theme
- Dynamic Material You colors
- Reboot resilience for alarms
- Material 3 design
- MVI architecture pattern
- Local SQLite database with Room

### Technical
- Kotlin 2.1.0
- Jetpack Compose + Material 3
- Target SDK 36 (Android 16)
- Min SDK 26 (Android 8.0)
- Room database
- Kotlin Coroutines & Flow
- DataStore preferences
- 100% FOSS (GNU GPLv3)
