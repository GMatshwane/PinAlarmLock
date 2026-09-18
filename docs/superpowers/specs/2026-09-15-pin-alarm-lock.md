# PIN Alarm Lock

App-owned PIN gate for a single-activity Jetpack Compose Android app. Not a system lock, Device Owner/kiosk hook, biometric login, or vault.

## Goals

- First launch: set a 4–6 digit PIN with confirmation.
- Later launches: numeric PIN pad to unlock.
- Wrong PIN (including the first attempt): shake/error UI, ~10s alarm on `STREAM_ALARM`, optional vibration; remain locked.
- Correct PIN: Unlocked screen with Lock again.
- Re-lock when the app is backgrounded or left.
- Persist only a salted SHA-256 hash in Preferences DataStore (never plaintext).

## Out of scope

- `INTERNET` permission
- Biometrics
- Encrypted file vault / secret notes
- System lock-screen replacement, Device Admin, Device Owner, or kiosk APIs
- `MediaPlayer` or `res/raw` alarm assets

## Package and toolchain

- Application ID / namespace: `com.pinalarmlock.app`
- minSdk 26, targetSdk 36, compileSdk 36
- AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, Gradle wrapper 8.11.1

## Destinations

`Loading` → `SetupEnter` / `SetupConfirm` (no saved PIN) or `Locked` (PIN exists) → `Unlocked`.

## Storage

Preferences DataStore keys:

- `pin_salt`
- `pin_hash`

Encoding: `java.util.Base64` URL-safe without padding. Verify with `MessageDigest.isEqual`.

## Alarm MVP

`ToneGenerator` only: `TONE_CDMA_EMERGENCY_RINGBACK`, volume 100, `STREAM_ALARM`, auto-stop after `AlarmDuration.ALARM_DURATION_MS` (10_000L). Optional vibration. No `MediaPlayer`.
