# Project Overview
Android HUD Speedometer app built in Kotlin using Jetpack Compose (Material 3). Converts the device into a windshield-projected display with high AMOLED power efficiency.

# Tech Stack & Architecture
- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Location Engine:** Google `FusedLocationProviderClient`
- **State Management:** `ViewModel` with `StateFlow`
- **Architecture:** MVVM with clean separation (UI -> ViewModel -> LocationRepository)

# Development Rules
- **UI & Theme:** Always enforce pure black background (`#000000`) for AMOLED energy saving. Do NOT use dark grays (`#121212`).
- **HUD Mirroring:** Implement mirroring using layout-level graphic transforms (`scaleX = -1f`).
- **Code Style:** Keep composables small and stateless where possible. Pass state down, elevate events up.
- **Gradle & Dependencies:** Do NOT add heavy third-party UI libraries unless requested; stick to native Compose APIs (`animateFloatAsState`, `AnimatedContent`).
- **Location:** Handle `ACCESS_FINE_LOCATION` checks gracefully. Keep location updates lifecycle-aware.

# Common Commands
- Build app: `./gradlew assembleDebug`
- Run lint/checks: `./gradlew lint`

# Documentation & Vault Sync
- Obsidian Vault Path: `C:\Users\l7afzoli9\Documents\SecondBrain`
- When requested to log dev updates or architecture notes, export them as Markdown files into this vault.