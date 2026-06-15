# Dynamic Island for Android (MIUI Style)

A sophisticated implementation of the "Dynamic Island" feature for Android devices, inspired by iOS and MIUI. This project provides a versatile, interactive overlay that adapts to various system events and application notifications, offering a unified multitasking experience.

## 🌟 Features

The application monitors system-wide events and provides real-time interactive widgets for:

### 1. Media Control
- Real-time playback status and track information.
- Album art extraction with dynamic color themes using the Palette API.
- Quick controls for play/pause and track navigation.

### 2. Smart Notifications
- Intercepts incoming notifications and displays them in a non-intrusive island.
- Support for "Live Activities" such as messaging and quick replies.
- Categorized display for different app types.

### 3. Navigation Support
- Integrated with Google Maps and other navigation providers.
- Real-time turn-by-turn directions, distances, and exit instructions.
- Visual map snippets and direction icons.

### 4. Communication
- **Incoming & Ongoing Calls:** Caller ID, photo, and duration tracking.
- **Call Controls:** Quick toggles for Mute, Speaker, and Ending calls.

### 5. System Status & Events
- **Charging:** Detailed battery level, charging wattage, and estimated completion time.
- **Bluetooth:** Connection status and battery levels for connected peripherals (Earbuds, Case, etc.).
- **Volume & Ringer:** Visual feedback for volume changes and silent/vibrate mode toggling.

### 6. Utility Widgets
- **Weather:** Real-time temperature, conditions, and hourly/daily forecasts.
- **Timer & Stopwatch:** Persistent countdowns and elapsed time tracking.
- **Progress Tracking:** Visual progress bars for active downloads or background tasks.
- **Clipboard:** Quick access to recently copied text snippets.

---

## 🛠 Tech Stack

The project leverages modern Android development tools and libraries:

### Core Frameworks
- **Kotlin:** 100% Kotlin-based project using modern language features like Coroutines and Flow.
- **Jetpack Compose:** Used exclusively for building the entire UI, including the dynamic overlay and settings.
- **Material 3:** Implements the latest Material Design guidelines for a modern look and feel.

### Architecture & State Management
- **Clean Architecture:** Separation of concerns between Data, Domain, and UI layers.
- **MVI (Model-View-Intent) / State-Driven UI:** Using Kotlin Sealed Classes (`IslandState`) to manage complex UI transitions and priorities.
- **ViewModel:** Lifecycle-aware state management for the UI components.

### Services & System Integration
- **Accessibility Service:** Used to monitor system events and manage the overlay window across all apps.
- **Notification Listener Service:** Captures and processes incoming notifications and media metadata.
- **Foreground Service:** Ensures the Dynamic Island remains active and responsive in the background.
- **WorkManager:** Handles periodic background tasks like weather synchronization.

### Data & Networking
- **Retrofit 2 & OkHttp 3:** Handles API communication for weather data.
- **Gson:** Used for JSON serialization/deserialization.
- **Jetpack DataStore (Preferences):** Modern replacement for SharedPreferences to store user settings.

### Image & UI Utilities
- **Coil:** Efficient image loading for album art, app icons, and weather symbols.
- **Palette API:** Extracts dominant colors from images to dynamically theme the island based on the active content.
- **Vector Graphics:** Extensive use of Material Icons for a crisp UI at any scale.

---

## 🚀 Getting Started

### Prerequisites
- Android 8.0 (API level 26) or higher.
- Android Studio Iguana or newer.

### Permissions Required
To function correctly, the app requires several high-level permissions:
1. **Accessibility Service:** To draw the overlay and detect UI changes.
2. **Notification Access:** To read and display incoming notifications.
3. **Location Permission:** Required for accurate weather updates.
4. **Phone State:** To monitor and display call information.

### Configuration
1. Clone the repository.
2. Add your `WEATHER_API_KEY` in the `app/build.gradle.kts` file or as an environment variable.
3. Build and run the `:app` module.

---

## 📂 Project Structure

- `com.miui.dynamicisland.service`: Core background services (Accessibility, Notification Listener).
- `com.miui.dynamicisland.ui.components`: Modularized Compose widgets for each island state.
- `com.miui.dynamicisland.ui.states`: Sealed class definitions for the Island's reactive states.
- `com.miui.dynamicisland.manager`: Business logic for prioritizing and queuing island events.
- `com.miui.dynamicisland.data`: Models and repositories for weather and system data.

---

## 📝 License
This project is for educational purposes and follows MIUI/Android design guidelines.
