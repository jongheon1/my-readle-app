# my-readle (Android)

Personal Readle-clone Android app. Pulls daily leveled English articles from
[my-readle-content](https://github.com/jongheon1/my-readle-content) and shows
them with tappable vocabulary.

> Status: MVP scaffold. Single-user, sideload-only — not headed to the Play Store.

## Stack

- Kotlin 2.0 + Jetpack Compose (Material 3)
- Navigation Compose
- Ktor Client (OkHttp engine) + Kotlinx Serialization
- Room 2.6 (KSP) + DataStore Preferences
- Coil 2 (for future image support)
- Manual DI via `ServiceLocator` — no Hilt/Koin

See [docs/part2-app-plan.md](docs/part2-app-plan.md) for the full design notes.

## Requirements

- **JDK 17** (Android Studio bundles one)
- **Android Studio Ladybug (2024.2)** or newer — needs Kotlin 2.0 plugin support
- **Android SDK 34** (compileSdk)
- Min device API 26 (Android 8.0)

## First-time setup

The repo intentionally does not commit `gradle-wrapper.jar`. Generate it once:

### Option 1 — Android Studio (recommended)

1. `File → Open` and point at this directory.
2. Android Studio downloads Gradle and writes the wrapper jar automatically.
3. Sync, then `Build → Make Project`.

### Option 2 — Command line (if you have a system Gradle)

```powershell
gradle wrapper --gradle-version 8.10.2
./gradlew assembleDebug
```

## Build & run

```powershell
# Debug APK
./gradlew :app:assembleDebug

# Install on connected device (USB debugging on)
./gradlew :app:installDebug
```

Debug builds use `applicationId` `com.jongheon.myreadle.debug` so they coexist
with release builds.

## Release build (sideload)

Release is currently signed with the debug key (fine for personal sideload).
When you want a stable key:

```powershell
# Generate once
keytool -genkey -v -keystore my-readle.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-readle

# Then point app/build.gradle.kts release signingConfig at it
./gradlew :app:assembleRelease
```

The APK lands in `app/build/outputs/apk/release/`. Copy it to your phone (USB,
Drive, or `adb install`) and tap to install — Android will ask to allow
sideloading from your browser/file manager.

## Project layout

```
app/src/main/java/com/jongheon/myreadle/
├── MainActivity.kt              # NavHost host + theme
├── MyReadleApp.kt               # Application — wires ServiceLocator
├── core/
│   └── ServiceLocator.kt        # manual DI
├── data/
│   ├── local/                   # Room + DataStore
│   ├── remote/                  # Ktor + DTOs
│   └── repository/              # ArticleRepository
├── domain/model/                # Article, Level, Vocab, KeyPhrase
└── ui/
    ├── home/                    # HomeScreen + ViewModel
    ├── article/                 # ArticleScreen + ClickableArticleText + BottomSheet
    ├── settings/                # SettingsScreen + ViewModel
    ├── common/                  # category icons, date labels
    ├── nav/                     # routes + NavHost
    └── theme/                   # Material 3 color scheme + typography
```

## Data flow

1. App start → `HomeViewModel.refresh(force = false)`
2. Repository checks DataStore `last_index_update`. If older than 1h, hits
   `raw.githubusercontent.com/.../articles/index.json`.
3. For each new date in the index, fetches `YYYY-MM-DD.json` and upserts into
   Room.
4. `HomeScreen` observes `dao.observeAll()` — UI updates automatically.

Schema mismatches (newer `schema_version` than the app supports) surface as a
banner instead of silently corrupting the cache.

## Known gaps (v0.1)

- No image rendering — `image_url` is captured but unused.
- No vocabulary review / spaced repetition.
- No TTS, no progress tracking.
- Release builds use the debug keystore.

See [docs/part2-app-plan.md §10](docs/part2-app-plan.md) for the v2 wishlist.
