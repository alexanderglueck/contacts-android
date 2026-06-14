# Contacts

Android client for a contacts-management backend. Built with Jetpack Compose, Hilt, Retrofit, Room and WorkManager.

## Features

- Email/password login with optional two-factor authentication (TOTP or recovery code)
- Account registration
- Browse, search, view, create and edit contacts (synced from the backend, cached locally in Room)
- Calendar view of contact-related events
- Caller ID: incoming PSTN calls are matched against the cached directory and surfaced via a heads-up notification (uses `CallScreeningService` and a contacts directory `ContentProvider`)
- Multi-team aware — the active team's UUID is sent on every authenticated request

## Requirements

- Android Studio (Ladybug or newer)
- JDK 11+ (Android Studio's bundled JBR is fine)
- Android SDK 36 (`compileSdk` / `minSdk` / `targetSdk` are all 36)
- A reachable instance of the companion backend (Laravel-style REST API)

## Backend configuration

The base URL lives in [`app/src/main/java/at/gdev/contacts/data/network/ApiConfig.kt`](app/src/main/java/at/gdev/contacts/data/network/ApiConfig.kt):

```kotlin
const val ORIGIN = "https://contacts.alexanderglueck.at"
const val BASE_URL = "$ORIGIN/api/v1/"
```

- The shipped default points at the maintainer's hosted instance. Point `ORIGIN` at your own backend to run the app against your own data.
- For **local development**, the Android emulator reaches the host machine's `localhost` via `http://10.0.2.2:8080`; on a **physical device** use the host's LAN IP (e.g. `http://192.168.1.42:8080`) and make sure the backend listens on that interface.
- Cleartext HTTP is allowed only for the loopback domains declared in `app/src/main/res/xml/network_security_config.xml` (`localhost`, `10.0.2.2`). If you point at a non-loopback host over plain HTTP, add it there too — or, better, use HTTPS.

The API is expected to conform to the OpenAPI spec served by the backend at `/docs/api.json` (Laravel + Sanctum-style: Bearer tokens, ULID/UUID identifiers, snake_case JSON, `422` validation envelopes).

## Build & run

From the project root:

```bash
# Debug build (APK in app/build/outputs/apk/debug/)
./gradlew assembleDebug

# Install onto a running emulator/device
./gradlew installDebug
```

Or open the project in Android Studio and use the **Run** action.

## Project layout

```
app/src/main/java/at/gdev/contacts/
├── calls/         # CallScreeningService, caller-ID notification, directory ContentProvider
├── data/
│   ├── auth/      # Token storage (DataStore)
│   ├── local/     # Room entities, DAO, database
│   ├── network/   # Retrofit APIs, DTOs, AuthInterceptor, error mapping
│   ├── repository # AuthRepository / ContactsRepository / etc. implementations
│   └── sync/      # WorkManager sync workers
├── di/            # Hilt modules
├── domain/        # Pure-Kotlin models and repository interfaces
└── ui/            # Compose screens & view models (auth, contacts, calendar, settings)
```

## Caller ID

The caller-ID feature requires the user to grant the app the **Call Screening** role at runtime (handled by `CallerIdRoleHelper` / the settings screen). Without that role, incoming-call lookups are skipped silently.

## Tech stack

| Concern              | Library                                                 |
|----------------------|---------------------------------------------------------|
| UI                   | Jetpack Compose + Material 3                            |
| DI                   | Hilt                                                    |
| Networking           | Retrofit + OkHttp + kotlinx.serialization               |
| Local storage        | Room, DataStore Preferences                             |
| Background work      | WorkManager (Hilt-injected workers)                     |
| Images               | Coil                                                    |
| Calendar UI          | `kizitonwose/Calendar`                                  |
| Markdown rendering   | `mikepenz/multiplatform-markdown-renderer` (M3)         |

## License

Copyright © 2026 Alexander Glück.

Licensed under the **GNU Affero General Public License v3.0** (AGPL-3.0) — see [LICENSE](LICENSE) for the full text. In short: you are free to use, study, modify and self-host this software, but if you distribute it or run a modified version as a network service, you must make the corresponding source available under the same license.

A separate **commercial license**, without the AGPL copyleft obligations, is available from the copyright holder. For commercial-licensing terms, contact github@alexanderglueck.at.
