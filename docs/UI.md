# User interface overview

This page describes the current UI structure and key interaction patterns present in the app.

## General app structure

The application uses Jetpack Compose and a navigation graph built with `Navigation Compose`. Screens are assembled in the presentation layer and state is owned by ViewModels.

Key presentation areas:

- Home screen
- Editor screen
- Categories screen
- Settings screens
- Trash screen
- Backup/restore screen
- privacy and app-information screens
- full-screen alarm activity

## Main navigation flow

The app is organized around a home-first flow:

- home lists reminders and filters them;
- users open editor screens to create or modify reminders;
- settings screens adjust app behavior and theme preferences;
- the full-screen alarm activity appears when Android allows full-screen presentation.

## Reminder list and filtering

The home screen manages reminder filtering and display state for:

- all active reminders
- today
- scheduled
- overdue
- completed

It also supports search and category-based filtering and keeps a local stream from Room as its main data source.

## Reminder editor

The editor screen allows the user to modify:

- title and notes
- due date/time
- category
- priority
- repeat interval
- subtasks
- photo attachment
- notification style override

The editor also supports a preview of the selected reminder image and the user-facing state transitions for completion and deletion flows.

## Settings and configuration

Settings are grouped into themed cards and dedicated screens for:

- general behavior
- reminders
- notifications and alarms
- appearance
- data / backup and restore
- privacy
- app information

## Themes and colors

The design system uses Material 3 and a custom `ReminderTheme` implementation with:

- Light mode
- Dark mode
- AMOLED mode
- dynamic colors when supported
- accent color selection and custom accent support

These settings are persisted via DataStore.

## Home-screen widgets

The Today widget shows active reminders scheduled for the current day. It can be resized on the home screen and its background can be toggled between opaque and transparent from Android's widget reconfiguration screen. Quick add is a compact fixed-size action that opens the new reminder editor.

## Accessibility and localization

The project includes:

- per-app language support for eight locales;
- RTL support via Android config and Compose layout direction;
- Material 3 accessibility-friendly controls and sufficiently large touch targets;
- descriptive app and action labels in the localized resources.

## Interaction constraints

The UI must remain compatible with:

- Android 8.0+
- Android notification permission and full-screen permission restrictions
- exact alarm scheduling limits
- device-specific restrictions on full-screen alarm presentation

## Notable UI components

Shared visual components include:

- reminder cards
- badges for priority and counters
- category icon and color handling
- section headings
- custom color picker dialog
- settings group rows and selectors

See also [docs/THEMING.md](THEMING.md), [docs/NOTIFICATIONS.md](NOTIFICATIONS.md), and [docs/ARCHITECTURE.md](ARCHITECTURE.md).
