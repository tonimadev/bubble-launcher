Bubbles Launcher
================

A compact Android launcher that displays installed apps as bubbles. Designed with configurable visual preferences, pinning, highlighting, and basic profile support (personal/work). Built with Jetpack Compose and an MVI-style ViewModel.

Key features
------------
- Bubble-style app grid with optional dynamic sizing by usage
- Pin apps so they always appear first
- Highlight apps (full-color vs grayscale) and persist preferences
- Toggle app name visibility, fixed icon sizing, and use of the system wallpaper
- Theme selection: System / Light / Dark
- Settings persisted via DataStore
- Support for personal and work profiles (tab navigation)

Architecture
------------
- MVI-like pattern: intents are submitted to `MainViewModel`, which updates a single UI state and emits events
- Settings persisted in DataStore (`SettingsRepository`)
- App list loaded from `AppRepository` and exposed to the UI as `AppInfo` objects

Quick start
-----------
Requirements:
- Android Studio (recommended) or JDK/Gradle via CLI
- A device or emulator running Android

Build debug APK:

```bash
cd /path/to/BubblesLauncher
./gradlew :app:assembleDebug
```

Install on device/emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Permissions
-----------
The launcher may request:
- Storage/media read permission (to use the system wallpaper)
- Usage access (to compute ordering based on app foreground time)

If the app prompts for a permission rationale, follow the dialog to grant access. Usage access must be enabled in system settings if you want ordering by usage.

Settings
--------
Open the settings from the top-right icon in the app. Available toggles and controls:
- Show/hide app names
- Ignore dynamic bubble size (fixed icon size with slider)
- Use system wallpaper
- Theme (System / Light / Dark)
- Pin or highlight apps via long-press menu on a bubble

Notes and known issues
----------------------
- Icons are cached and resized to reduce scroll jank; if you still see performance issues on low-end devices, report details.
- Pin order is preserved during runtime; persistence preserves the set of pinned apps. If you want a stable pin-order across restarts, that can be implemented (currently stored as a set).

Contributing
------------
Contributions, bug reports and suggestions are welcome. Open an issue or submit a pull request with a concise description of the change.

License
-------
No license file is included. Add one if you intend to open-source the project.

