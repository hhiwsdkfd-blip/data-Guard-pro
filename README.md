# Ultimate Data Monitor

A world-class, privacy-first mobile data monitoring and telemetry application for Android, built with Kotlin, Jetpack Compose, Material Design 3, Room, and WorkManager.

---

## 🌟 Key Features

1. **Hardware-Accurate Network Telemetry**:
   - Live real-time traffic speed meter (measuring instantaneous download and upload deltas via `TrafficStats`).
   - Deep carrier, Wi-Fi SSID, connection capabilities, downstream/upstream bandwidth, and metered network detection via `ConnectivityManager.NetworkCallback`.
   - Aggregated system counters queried directly from the Android operating system using `NetworkStatsManager`.

2. **Per-Application Usage Tracking**:
   - Detailed breakdown of network traffic per app (Mobile vs. Wi-Fi, Rx vs. Tx, percentage share).
   - Filter by timeframe: Today, Yesterday, This Week, This Month, Billing Cycle.
   - Filter by traffic type: All, Mobile Only, Wi-Fi Only.
   - Search by application name or package identifier.
   - Deep integration with `PackageManager` to identify user applications vs. system services.
   - System application settings launcher.

3. **Smart Data Allowance & Limits**:
   - Monthly mobile quota configuration (GB) with visual arc gauge.
   - Configurable billing cycle start day (1st - 31st of month) with automatic month-end boundary clamping (handling 28, 29, 30, and 31-day months).
   - Warning thresholds at 50%, 75%, 90%, and 100% of plan allowance.
   - Intelligent deduplication engine preventing repetitive alert spam within the same cycle.

4. **Intelligent Statistical Analytics & Forecasting**:
   - Moving average pace calculations (daily average, 7-day average, monthly average).
   - Burn-rate forecasting estimating days remaining until quota exhaustion.
   - Spike & anomaly surge detection (alerts when daily consumption exceeds 150% of the 7-day average).
   - Identification of peak usage days.
   - Transparent mathematical models clearly marked as statistical projections.

5. **Local Persistence & Privacy (100% On-Device)**:
   - Built with Room Database (SQLite) for historical snapshots, per-app records, and notification history.
   - Zero telemetry, zero cloud leak, zero third-party trackers.
   - User control to reset all local usage records at any time.

6. **Periodic Background Monitoring**:
   - Scheduled background tasks via `WorkManager` with battery-friendly constraints.
   - Evaluates quota thresholds and dispatches high-priority system notifications.

7. **Bilingual Localization & Design Polish**:
   - Complete localized strings for both **English** and **Arabic (العربية)**.
   - Full Right-to-Left (RTL) layout support.
   - Modern telemetry palette with adaptive Light and Dark modes.

---

## 🏗️ Architecture & Technology Stack

- **UI Layer**: Jetpack Compose, Material 3, Edge-to-Edge (`enableEdgeToEdge`), M3 Navigation.
- **Architecture**: MVVM with reactive Kotlin Coroutines and `StateFlow`.
- **Database**: Android Jetpack Room with Kotlin Symbol Processing (`KSP`).
- **Preferences**: SharedPreferences / DataStore reactive bridge.
- **Background Engine**: AndroidX WorkManager (`PeriodicWorkRequestBuilder`).
- **Telemetry APIs**: `android.app.usage.NetworkStatsManager`, `android.net.ConnectivityManager`, `android.net.TrafficStats`.

---

## 🔒 Permissions & Security Model

| Permission | Purpose |
|---|---|
| `android.permission.INTERNET` | General network communication capability. |
| `android.permission.ACCESS_NETWORK_STATE` | Inspect network state, metered connection status, link bandwidths, and Wi-Fi vs Cellular transports. |
| `android.permission.ACCESS_WIFI_STATE` | Inspect Wi-Fi connectivity details. |
| `android.permission.READ_PHONE_STATE` | Retrieve cellular carrier and network operator info. |
| `android.permission.PACKAGE_USAGE_STATS` | Special system access granted via Android Settings to query exact per-app network consumption through `NetworkStatsManager`. |
| `android.permission.POST_NOTIFICATIONS` | Required on Android 13+ (API 33+) to dispatch data allowance warning notifications. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Reschedules WorkManager monitoring after device reboots. |

---

## 🚀 Building & Testing

### Prerequisites
- JDK 17 or higher
- Android SDK 36 (compileSdk)

### Local Build Commands
```bash
# Run Unit and Robolectric Tests
gradle :app:testDebugUnitTest

# Assemble Debug APK
gradle assembleDebug

# Assemble Release APK
gradle assembleRelease
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## ⚙️ Automated CI / CD Workflow

A production GitHub Actions workflow is located at:
`.github/workflows/android-ci.yml`

This workflow triggers on `push` and `pull_request` to `main`/`master`, sets up JDK 17, executes unit and Robolectric tests, compiles the debug APK, and uploads the APK and test results as downloadable artifacts.
