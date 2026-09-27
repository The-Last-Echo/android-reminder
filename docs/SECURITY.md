# Security and privacy

This document describes only the security and privacy mechanisms that are truly present in the repository.

## Measures currently present

### Local storage

- Reminder data is stored locally in Room.
- App preferences are stored locally via DataStore.
- Attachments are stored in app-private files, not in a server or cloud workspace.

### Permissions

The app declares Android permissions required for:

- notifications
- exact alarms and boot rescheduling
- foreground media playback
- vibration

It does not request broader privileged access such as overlay or notification-policy permission bypasses.

### Data minimization

- update checks use public GitHub release metadata only;
- these checks do not include reminder content or user data;
- backups are user-generated and written to a destination selected via Android's file picker.

### Debug logging

Debug logging is limited to scheduling, notification, and alarm lifecycle events. It intentionally avoids user content such as reminder titles and notes.

## Separation between offline and online

The offline flavor does not declare network permissions. The online flavor only adds optional public release metadata checks and network constraints for update checks. There is no online data sync or user account system.

## Good practices and recommendations

These are not implemented as guarantees, but they are reasonable guardrails for future work:

- keep local-only data in private app storage;
- validate backup files before restore;
- avoid writing user content to debug logs;
- prefer privacy-preserving release checks when adding online features;
- document any future sync or server model before implementing it.

## Future security work

Any future remote sync or self-hosted server design would need explicit decisions around:

- auth and identity
- transport security
- conflict handling
- deletion propagation
- encryption at rest
- secure attachment handling
- remote backup validation

These are future concerns and not implemented features.

See also [docs/OFFLINE_ONLINE.md](OFFLINE_ONLINE.md), [docs/BUILD.md](BUILD.md), and [docs/BACKUP_RESTORE.md](BACKUP_RESTORE.md).
