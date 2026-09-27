# Offline / Online status

This project intentionally ships two distribution variants, but the current implementation is still primarily local-first.

## IMPLEMENTED

### Offline flavor

The `offline` flavor is the local-only distribution. It is the most representative steady-state build of the app.

Implemented behavior:

- local reminder creation and scheduling
- local database and preferences
- local notifications
- local `AlarmManager` scheduling and reboot recovery
- local backup and restore
- local widgets
- local theme and language settings
- app-local data retention and trash cleanup

The `offline` build omits network permissions in the merged manifest and does not declare the app’s online update-check features.

### Online flavor

The `online` flavor keeps the same local reminder engine but adds optional update-check behavior for public GitHub release metadata.

Current online features:

- periodic and manual GitHub release checks via WorkManager
- network constraint (`CONNECTED`) for release checks
- app update metadata caching in DataStore
- same local reminder system as `offline`

Important: this is not remote synchronization or online reminder storage. Reminder data remains local.

## PARTIAL

The project has a partial online architecture layer, but it does not yet provide a functional distributed service.

Current partial elements:

- `DistributionFeatures` interface with separate offline and online implementations
- `UpdateCheckController` for network-based app release metadata
- `SyncEngine` placeholder with `Unavailable` and `NotConfigured` results

This indicates a future design direction, but it is not an implemented remote sync system.

## PLANNED / FUTURE

The repository contains the following ideas and structural hooks for future work, but they are not implemented today:

- remote synchronization of reminder data
- self-hosted server or private server backend
- WebDAV or other remote storage protocol
- conflict resolution logic for multi-device sync
- deletion propagation between devices
- attachment synchronization and storage policy
- stable device identifiers or remote synchronization tokens
- deduplication and merge strategy for remote records
- encryption and secure transport for online data

These items must be considered future work and must not be described as current features.

## NOT IMPLEMENTED

The following are not present in the codebase today:

- user accounts
- login or authentication system
- cloud storage integration
- WebDAV client
- remote reminder synchronization
- collaborative editing
- online backup service
- remote data residency or server-side reminder processing

## Local-first principle

The project’s actual architecture is local-first: the reminder system is designed to operate without a network connection. The fundamental reminder flow is local, and alarm delivery does not depend on a remote server being available.

That means any future synchronization model should be designed to be additive and non-essential for core reminder delivery.

## Practical consequence

The distinction is explicit:

- `offline` = the actual local-only build
- `online` = a distribution variant for optional update checks, not a server-backed reminder system

This is a factual status, not a future assumption.

## Summary

```text
IMPLEMENTED:
- local reminders
- local notifications
- local alarms
- local backup/restore
- offline-first behavior
- optional GitHub release checks in online flavor

PARTIAL:
- online flavor shell and distribution abstraction
- sync placeholder interfaces

PLANNED:
- sync engine, remote backend, conflict handling, server storage, attachments sync

NOT IMPLEMENTED:
- user account system
- WebDAV or server sync
- cloud reminder repository
```

See also [docs/ARCHITECTURE.md](ARCHITECTURE.md), [docs/BUILD.md](BUILD.md), and [docs/SECURITY.md](SECURITY.md).
