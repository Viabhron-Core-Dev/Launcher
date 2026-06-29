# BLUEPRINT: Template Android Launcher

This document contains the structural blueprint, design details, development phases, and change ledger for the Template Android Launcher application, adhering to the strict guidelines and mandates specified.

## Core Architectural Overview

### 1. Hard Constraints Validation & Strategy
- **Package**: `com.example.templatelauncher` (All code will reside in packages nested under `com.example.templatelauncher` inside `app/src/main/java/com/example/templatelauncher`).
- **Language**: Kotlin-only. Zero Jetpack Compose dependencies, components, or files. Zero Java files.
- **Base Activity**: All activities extend `ComponentActivity` instead of `AppCompatActivity`.
- **Theme**: `android:Theme.Black.NoTitleBar.Fullscreen` is used as the application theme in `AndroidManifest.xml` (no AppCompat, no Material components theme).
- **Target SDK**: Min SDK 35, Target SDK 35.
- **Gradle & Kotlin Versions**: Gradle 8.9, Kotlin 2.0.21, KSP 2.0.21-1.0.27.
- **Dependency Management**: Centralized version control via `libs.versions.toml`. No hardcoded versions.
- **No Jetpack Compose / Unused Dependencies**: All existing Jetpack Compose dependencies, Hilt, Navigation, and Material3 will be removed from `build.gradle.kts` and `libs.versions.toml` or commented out.
- **Color Attributes**: No `?attr/...` reference. All colors hardcoded as hex values in XML/Kotlin.
- **Git & Build files**: `*.jar binary` inside `.gitattributes`, committed `gradle-wrapper.jar` pointing to Gradle 8.9, GitHub Actions workflow using JDK 17 Temurin, executable permissions, and artifact uploads.

### 2. Core Functional Components
- **Crash Logger / Diagnostics**:
  - `LauncherApplication` monitors unhandled exceptions.
  - Logs app lifecycle and crashes to external Download directory files: `Downloads/launcher_crash_[timestamp].txt`, `launcher_crash_latest.txt`, and `Downloads/launcher_log.txt` (rolling log, size limit 1MB).
  - `LogViewerActivity` exposes ScrollView, Clear and Share actions using a safe FileProvider.
- **Home Screen & Grid Layout**:
  - `HomeActivity` manages desktop grids.
  - Bitmap caching for wallpaper (background thread) and icons (`LruCache<String, Bitmap>`).
  - Native XML `ViewPager2` handles horizontal grid paging.
  - Configurable column/row dimensions (SharedPreferences, default 4x5).
  - Stands-alone custom adapter (`AppGridAdapter`) dynamically positions installed apps to exact cellular points `(page, col, row)` stored in Room.
  - Broadcast receiver dynamically processes `PACKAGE_ADDED`, `PACKAGE_REMOVED`, and `PACKAGE_CHANGED` intents.
- **Dock & Navigation**:
  - 5 bottom slots: 4 app targets (defaults: first 4 alphabetical apps) + central toggle drawer launcher.
  - Re-drawn programmatically via `removeAllViews()` on preference modification.
- **App Drawer Overlay**:
  - Full-screen XML layout.
  - Opens exclusively from center button.
  - Back-press handled via `OnBackPressedCallback`.
  - Filter search bar matching alphabetically-sorted non-hidden apps.
- **Settings Dashboard**:
  - `SettingsActivity` offers controls for grid row/col bounds (3-6, 4-8), icon sizes (Small, Medium, Large), and system details. Saved in `launcher_prefs`.
- **Persistence Engine (Room)**:
  - Entities: `WorkspaceItem` and `AppPreference` processed via Kotlin KSP.
  - CRUD operations mapped inside `WorkspaceDao` through `LauncherDatabase`.
  - UI long-press triggers app assignment popup to bind launcher targets into Room.

---

## Planned Development Phases

### Phase 1: Dependency Purification & Workspace Restructuring
1. Purge Jetpack Compose and AI Studio template dependencies from `libs.versions.toml` and `app/build.gradle.kts`.
2. Restructure files from package `com.example` to `com.example.templatelauncher`.
3. Set minSdk, targetSdk, and compileSdk to 35. Configured KSP 2.0.21-1.0.27.
4. Establish `.gitattributes` and the GitHub Actions workflows.

### Phase 2: Diagnostics & Application Framework (The Foundation)
1. Write `LauncherApplication` with uncaught exception handler + rolling diagnostics file log.
2. Develop `LogViewerActivity` using ScrollView with Clear / Share.
3. Configure `FileProvider` in `AndroidManifest.xml` for sharing log files.

### Phase 3: Room Database Integration
1. Write Room entities `WorkspaceItem` and `AppPreference`.
2. Implement `WorkspaceDao` and `LauncherDatabase` with standard KSP configuration.
3. Keep database code compiled and ready as infrastructure.

### Phase 4: Core User Interfaces & View Layouts (Home Screen, Grid, and Dock)
1. Write modern XML layouts with custom color styling (no Material/AppCompat themes).
2. Code `HomeActivity` managing background wallpaper cache, programmatic grid sizes, paging, and icon loading via coroutines.
3. Implement `AppGridAdapter` (standalone) using a custom grid view pattern (TableLayout/GridLayout dynamically computed to match custom constraints).
4. Code dynamic dock slots with custom layout.

### Phase 5: Drawer & App Search Engine
1. Configure drawer overlay in Home screen.
2. Build search filter with alphabetical ordering and text changes.
3. Setup `OnBackPressedCallback` for drawer closing.

### Phase 6: Customization Panel
1. Code `SettingsActivity` with standard UI controls.
2. Store settings in `launcher_prefs` SharedPreferences.

---

## Running Ledger of Changes
- **2026-06-29 12:12**: Created `BLUEPRINT.md` outlining constraints and implementation design.
- **2026-06-29 12:22**: Purged legacy Compose modules, set up version catalogs, restructured codebase under `com.example.templatelauncher` package, configured diagnostic file logger, Room persistence entities/DAO, custom standalone adapter, desktop ViewPager2, bottom dock layout, App Drawer overlay, and settings customization panel. Added `.gitattributes` and `.github/workflows/build.yml`. Successfully compiled and verified debug APK packaging using Gradle.
- **2026-06-29 12:32**: Generated and added the standard Gradle wrapper files (`gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar`) to resolve the GitHub Actions checkout failure and verify the project configuration. Tested compilation and confirmed everything is fully stable.
- **2026-06-29 12:42**: Cleaned and regenerated `gradle-wrapper.jar` natively via the Gradle Wrapper task after removing any corrupted jar states, and confirmed that `.gitattributes` enforces binary mode for JARs.

