# Roadmap

This roadmap reflects only the current repository reality and the directions already visible in the code or project discussion, without turning speculation into a commitment.

## Done

The project already includes the following work:

- local reminder and checklist management
- Room-backed persistence with soft deletion and retention cleanup
- Android alarm scheduling and reboot recovery
- local notifications with multiple styles and full-screen fallback
- backup and restore with validation and local attachment handling
- Material 3 theme support with System/Light/Dark/AMOLED modes
- Android localization for eight languages
- app widgets for Today, Upcoming, and Compact views
- debug diagnostics for notification and alarm issues
- offline/online build split with offline local-only manifest constraints

## In progress

The repository does not show a formal active roadmap item beyond current maintenance and bug fixes, but the codebase clearly has some architecture-level preparation for future extension:

- online flavor architecture hooks
- optional GitHub release-check logic
- distribution abstraction for future features

These are not full remote features yet.

## Planned

The following ideas are visible as future work, but they are not implemented today:

- remote reminder synchronization
- private/self-hosted backend planning
- WebDAV compatibility exploration
- conflict management and deletion propagation
- attachment synchronization and deduplication strategy
- stable IDs across devices
- secure transport and encryption decisions for future online features

## Ideas / future

Possible future directions include:

- richer sync architecture after a clear product decision
- a more explicit online service model if the project adopts one
- stronger server-side conflict resolution and metadata management
- optional cloud or self-hosted storage depending on project goals

None of these ideas should be considered implemented or committed today.

## Offline / Online conclusion

The current state is clear:

- the app is local-first and can operate without network connectivity;
- the `online` flavor is not a remote synchronization platform;
- any future sync design must preserve the local reminder core as the primary operating model.

See also [docs/OFFLINE_ONLINE.md](OFFLINE_ONLINE.md), [docs/ARCHITECTURE.md](ARCHITECTURE.md), and [docs/BUILD.md](BUILD.md).
