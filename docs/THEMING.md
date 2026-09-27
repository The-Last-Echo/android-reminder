# Theming and appearance

Reminder uses Material 3 and a theme system defined in the design-system package.

## Theme modes

The implemented theme modes are:

- System
- Light
- Dark
- AMOLED

The app stores the selection in DataStore and applies it when the app starts. The theme logic is in `ReminderTheme`.

## Dynamic colors and accent color

The design system supports:

- Material You dynamic colors on API 31+
- a set of accent presets
- optional custom accent color selection
- dynamic color toggle

The code also exposes an AMOLED variant that darkens surfaces and background colors while preserving the same color engine.

## Design system structure

Files in `app/src/main/java/com/thelastecho/reminder/core/designsystem` define:

- color palettes
- shape definitions
- typography
- the composable `ReminderTheme`

## Design guidance

The current project uses consistent Material 3 surfaces and containers, with card, outline, and container colors adapted to the chosen theme.

## Not a finished glass design

The repository contains no implemented glass-style theme or full glassmorphism system as a shipped feature. Any such idea should be treated as a future concept rather than an existing UI mode.

## Accessibility and contrast

Theme work should preserve readability and maintain strong contrast across light, dark, and AMOLED modes. The app also supports RTL and localization-aware layouts.

See also [docs/UI.md](UI.md) and [docs/OFFLINE_ONLINE.md](OFFLINE_ONLINE.md).
