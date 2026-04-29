# Bubbles Launcher

Bubbles Launcher is an Android home launcher designed to reduce impulsive app switching and make usage patterns visible. It combines usage-aware app presentation with friction tools that help users pause before opening distracting apps.

## What It Does

- Replaces the default home screen (HOME launcher activity).
- Displays installed apps as "bubbles" in a responsive grid.
- Adapts bubble size based on usage time (optional fixed-size mode available).
- Shows per-app usage badges (time spent).
- Lets users pin important apps, highlight apps, and hide apps.
- Supports a delay gate before opening selected apps ("Mindful Delay").
- Provides a Focus Mode that keeps only essential apps visible.
- Includes metrics tracking to visualize resisted/given impulses.
- Supports personal and work profiles when available.

## Core Features

### 1) Usage-Aware Home Screen
- Apps are loaded and sorted by foreground usage time.
- Pinned apps stay at the top.
- App sections are grouped (favorites, used today, all apps).
- Long-press menu supports:
  - Pin/Unpin
  - Highlight/Unhighlight
  - Add/Remove delay
  - Add/Remove essential (for Focus Mode)
  - Hide app
  - Open app info

### 2) Mindful Delay
- For selected apps, opening is delayed by a short countdown.
- User can cancel (impulse resisted) or continue (impulse given).
- Outcomes are persisted for metrics.

### 3) Focus Mode
- Toggles a reduced launcher surface.
- Only apps marked as essential remain visible.
- Designed to lower cognitive load during focused sessions.

### 4) Metrics Dashboard
- Tracks user behavior around impulse control.
- Intended to show daily and longer-term progress.
- Accessible from Settings.

### 5) Settings
- Show/hide app names.
- Show/hide usage badges.
- Dynamic size vs fixed icon size.
- Icon size control.
- Theme mode (system, light, dark).
- Use system wallpaper.
- Manage hidden apps.

## Permissions

The app requests the following permissions:

- `android.permission.PACKAGE_USAGE_STATS`
  - Needed to read app usage statistics.
  - Used for usage-based sorting, bubble sizing, and usage badges.
  - Granted by the user through system Usage Access settings.

- `android.permission.QUERY_ALL_PACKAGES`
  - Needed so the launcher can discover and list installed apps.

If usage access is missing and usage-dependent features are enabled, the app prompts the user to open system settings and grant access.

## Tech Stack

- Kotlin
- Jetpack Compose (Material 3)
- Hilt (dependency injection)
- Room (local persistence)
- DataStore (user preferences)

## Project Structure

- `app/src/main/java/digital/tonima/bubbleslauncher/MainActivity.kt`
  - Root UI flow, launcher/default-home prompting, settings/metrics routing.
- `app/src/main/java/digital/tonima/bubbleslauncher/ui/MainScreen.kt`
  - Launcher grid, app sections, long-press actions, mindful delay overlay.
- `app/src/main/java/digital/tonima/bubbleslauncher/ui/MainViewModel.kt`
  - UI state, intents, and persistence orchestration.
- `app/src/main/java/digital/tonima/bubbleslauncher/data/`
  - App loading, settings repository, profile support.
- `app/src/main/java/digital/tonima/bubbleslauncher/ui/metrics/`
  - Metrics dashboard and related presentation logic.

## Build and Run

### Requirements
- Android Studio (recent stable)
- Android SDK configured
- A device or emulator (Android 7.0+ / API 24+)

### Build Debug APK
```bash
cd /Users/anthoni/AndroidStudioProjects/BubblesLauncher
./gradlew assembleDebug
```

### Install on Connected Device
```bash
cd /Users/anthoni/AndroidStudioProjects/BubblesLauncher
./gradlew installDebug
```

### Run Tests
```bash
cd /Users/anthoni/AndroidStudioProjects/BubblesLauncher
./gradlew test
```

## First-Time Setup on Device

1. Launch the app.
2. Complete onboarding.
3. Grant Usage Access when prompted (if you want usage-based features).
4. Optionally set Bubbles Launcher as the default Home app.
5. Configure Focus Mode, delay apps, and hidden apps in Settings.

## Current Status

This project is in active iteration. UI, metrics, and settings flows are being refined while preserving the core behavior of focus support and usage-aware launch management.

