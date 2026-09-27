# Build and release notes

This document records the build configuration that is actually present in the repository.

## Requirements

- Android Studio or a local Android SDK
- JDK 21
- Android SDK Platform 36
- Gradle wrapper in the repository

## Version values

The app version is defined in [gradle.properties](../gradle.properties):

- `appVersionCode=10804`
- `appVersionName=1.8.4`

These values must stay aligned with the Git tag and the CI checks.

## Gradle configuration

The root project declares:

- Android application plugin
- Kotlin Android plugin
- Kotlin Compose plugin
- KSP plugin

The app module in [app/build.gradle.kts](../app/build.gradle.kts) sets:

- `namespace = "com.thelastecho.reminder"`
- `compileSdk = 36`
- `minSdk = 26`
- `targetSdk = 36`
- `applicationId = "com.thelastecho.reminder"`

The project targets Android 8.0+ and compiles with Java 21 and Kotlin 2.1.

## Flavors and build variants

The app defines a flavor dimension named `distribution` with two product flavors:

- `offline`
- `online`

Each flavor has both Debug and Release variants. The build also uses the default `debug` and `release` build types.

### Flavor behavior

- `offline`: local-only distribution; no network permission is included in the merged manifest.
- `online`: same local app, plus optional GitHub release update checks and a network-aware WorkManager job.

There is no separate cloud-synchronized build variant today.

## Important build behavior

- Debug builds add `.debug` to the app ID.
- Release builds enable resource shrinking and minification.
- Signing is optional locally; if environment variables are present, the release build uses them.
- The environment variables are:
  - `KEYSTORE_PATH`
  - `KEYSTORE_PASSWORD`
  - `KEY_ALIAS`
  - `KEY_PASSWORD`

## Useful commands

From the repository root:

```bash
./gradlew testOfflineDebugUnitTest testOnlineDebugUnitTest
./gradlew assembleOfflineDebug assembleOnlineDebug
./gradlew assembleOfflineRelease assembleOnlineRelease
```

For a single debug build:

```bash
./gradlew assembleDebug
```

## Output locations

Typical APK output directories are:

- `app/build/outputs/apk/offline/debug/`
- `app/build/outputs/apk/online/debug/`
- `app/build/outputs/apk/offline/release/`
- `app/build/outputs/apk/online/release/`

## CI behavior

The GitHub Actions workflow in [.github/workflows/build-and-release.yml](../.github/workflows/build-and-release.yml) verifies both offline and online unit tests, builds both debug APKs, and checks that the offline manifest does not contain network permissions.

For tag builds, it validates that the tag matches the project version and then signs and publishes release APKs.

## Constraints and notes

- Debug and release variants are intentionally different only in build/configuration behavior.
- The project does not currently ship a separate “basic” or “server-backed” build.
- Network access is only used for optional update checks in the online flavor, not for reminder data storage or sync.

See also [docs/OFFLINE_ONLINE.md](OFFLINE_ONLINE.md), [docs/ARCHITECTURE.md](ARCHITECTURE.md), and [docs/SECURITY.md](SECURITY.md).
