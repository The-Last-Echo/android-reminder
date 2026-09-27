# Contributing

Reminder is a local-first Android project published under the GPLv3 license. The current contribution model is intentionally lightweight and aligned with the repository’s existing workflow.

## Project workflow

- keep changes focused on a single issue or improvement;
- work from a branch created from the current main branch;
- prefer small, reviewable pull requests;
- validate the relevant tests before submitting.

## Branch and commit guidelines

- use descriptive branch names, for example `fix/alarm-fallback` or `docs/architecture-audit`;
- keep commit messages clear and conventional when possible;
- avoid mixing documentation changes, refactors, and feature work in the same commit unless the change is inseparable.

## Before submitting a PR

Run the relevant checks:

```bash
./gradlew testOfflineDebugUnitTest testOnlineDebugUnitTest
```

If the change affects build or distribution configuration, also validate:

```bash
./gradlew assembleOfflineDebug assembleOnlineDebug
```

## Code expectations

- maintain the existing local-first architecture;
- keep alarm, notification, and persistence logic consistent with the current Android platform constraints;
- respect the separation between offline and online build behavior;
- do not describe future sync or remote features as already implemented;
- preserve compatibility with the current Room schema and migration strategy.

## Documentation expectations

When behavior changes, update the relevant docs in `docs/` and keep the README aligned with the real codebase.

## License and FOSS expectations

This repository is a FOSS project and the contribution model assumes:

- code remains compatible with the GPLv3 license;
- no proprietary or closed-source addition is introduced silently;
- no hidden analytics or remote tracking is added without explicit project approval.

See also [README.md](../README.md), [docs/ARCHITECTURE.md](ARCHITECTURE.md), and [docs/ROADMAP.md](ROADMAP.md).
