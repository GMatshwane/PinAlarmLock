# PinAlarmLock

App-owned PIN gate with an on-device alarm on failed unlock attempts.

This is **not** a system lock screen, Device Owner/kiosk mode, biometric login, or encrypted vault. The PIN only gates this app.

## Behavior

- First launch: set a 4–6 digit PIN and confirm it.
- Later launches: unlock with the numeric PIN pad.
- Wrong PIN (including the first attempt): shake/error UI, a ~10 second `ToneGenerator` alarm on `STREAM_ALARM`, and optional vibration. The app stays locked.
- Correct PIN: Unlocked screen with **Lock again**.
- Leaving or backgrounding the app re-locks it.

The plaintext PIN is never stored. A salted SHA-256 hash is written to Preferences DataStore (`pin_salt`, `pin_hash`) using URL-safe Base64 without padding.

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

`VIBRATE` only. No `INTERNET`.
