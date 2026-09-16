# PinAlarmLock

PIN gate for **other apps** on the phone, with an on-device alarm on failed unlock attempts.

This is **not** a system lock screen, Device Owner/kiosk mode, Accessibility locker, biometric login, or encrypted vault. Enrolled apps are unchanged; PinAlarmLock sits in front of them.

## Behavior

- First launch: set a 4–6 digit PIN and confirm it.
- Home: grant Usage access and Display over other apps, then enrol launchable apps.
- Opening an enrolled app while the session is locked shows the PIN pad in front of it.
- Wrong PIN (including the first attempt): shake/error UI, a ~10 second `ToneGenerator` alarm on `STREAM_ALARM`, and optional vibration. The app stays covered.
- Correct PIN: all enrolled apps stay accessible until the screen turns off (or the process dies).
- Opening PinAlarmLock from the launcher while locked still requires the PIN, then shows the enrolment list.
- Backgrounding PinAlarmLock does not re-lock the session.

The plaintext PIN is never stored. A salted SHA-256 hash is written to Preferences DataStore (`pin_salt`, `pin_hash`) using URL-safe Base64 without padding. Enrolled packages are stored in `protected_packages`.

## Build

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Requirements: JDK 17+, Android SDK with `compileSdk` / `targetSdk` 35.

## Toolchain

- Package: `com.example.pinalarmlock`
- minSdk 26, targetSdk 35, compileSdk 35
- AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, Gradle 8.11.1

## Permissions

`VIBRATE`, `PACKAGE_USAGE_STATS`, `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE`, `QUERY_ALL_PACKAGES`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`. No `INTERNET`.
