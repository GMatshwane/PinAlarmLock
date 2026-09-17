# App lock (protect other apps)

PinAlarmLock enrols installed apps and gates them with the existing PIN until the screen turns off. Other apps are unchanged. This spec supersedes the 2026-09-15 PIN spec on **unlocked UI** and **when the session re-locks**. PIN setup, hashing, DataStore PIN keys, pad, and alarm stay as specified there.

Not a system lock screen, Device Owner, Accessibility service, biometric login, or vault.

## Goals

- User picks launchable apps from a list and enrols them.
- Opening an enrolled app while the session is locked brings PinAlarmLock’s PIN pad in front of it.
- One correct PIN unlocks **all** enrolled apps until the screen turns off or the device is locked.
- Wrong PIN: existing shake, ~10s `ToneGenerator` alarm on `STREAM_ALARM`, optional vibration; stay locked; enrolled app stays covered.
- Opening PinAlarmLock from the launcher while the session is locked still requires the PIN, then shows enrolment (not a blank unlocked page).
- Backgrounding PinAlarmLock does **not** re-lock the session.
- Play-realistic later: Usage Access + overlay + foreground service. No Accessibility, Device Admin, Device Owner, or `INTERNET`.

## Out of scope

- Accessibility-based detection
- Device Admin / Device Owner / work profile / kiosk
- Biometrics
- Per-app PINs or per-app sessions
- Play Console listing, Data safety form, or `QUERY_ALL_PACKAGES` declaration submission (see `docs/play/` for the distribution checklist; the app itself does not request `QUERY_ALL_PACKAGES`)
- Locking notifications, Recents previews, or work profiles
- “Lock now” control (session ends only on screen off, device lock, or process death)

## Package and toolchain

Unchanged: `com.pinalarmlock.app`, minSdk 26, targetSdk 35, compileSdk 35, AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, Gradle 8.11.1.

## Session

`LockSession` is **in-memory in this process**, shared by the activity and the watcher service (application-scoped holder).

| Event | Session |
| --- | --- |
| Process start | Locked |
| Correct PIN (home or gate) | Unlocked |
| `ACTION_SCREEN_OFF` (power button / timeout; this is also how device lock is detected) | Locked |
| Process death | Locked (memory gone) |
| Home / Recents / switching apps | Unchanged |
| Backgrounding PinAlarmLock | Unchanged |

Wrong PIN does not change the session.

## Gate policy

`shouldGate(packageName)` is true only when **all** of:

1. Session is locked
2. `packageName` is in the enrolled set
3. `packageName` is not PinAlarmLock (`com.pinalarmlock.app`)

The enrolment list and watcher never include:

- PinAlarmLock (`com.pinalarmlock.app`)
- Any package that handles `ACTION_MAIN` + `CATEGORY_HOME` (the launcher)
- `com.android.settings`
- `com.android.systemui`
- `com.android.packageinstaller`, `com.google.android.packageinstaller`
- `com.android.permissioncontroller`

If the current app is PinAlarmLock (gate or home), `shouldGate` is false so the pad does not re-open itself. Opening PinAlarmLock from the launcher still shows the PIN when the **session** is locked; that is home navigation, not `shouldGate`.

## Storage

Same Preferences DataStore (`pin_prefs`):

| Key | Type | Meaning |
| --- | --- | --- |
| `pin_salt` | string | Existing PIN salt (URL-safe Base64, no padding) |
| `pin_hash` | string | Existing salted SHA-256 |
| `protected_packages` | string set | Enrolled application IDs |

`ProtectedAppsRepository` add/remove/contains/list. Never store a plaintext PIN.

## Components

```
MainActivity (home | gate)
    LockViewModel          PIN pad, setup, existing alarm
    Home / enrolment UI    permissions + app toggles
ProtectedAppsRepository    enrolled packages
LockSession                locked/unlocked for this screen-on
LockWatchService           FGS, UsageEvents poll, starts gate
BootReceiver               start service after boot when eligible
```

**LockWatchService** is a foreground service with type `specialUse` (Android 14+). Subtype: app lock. Ongoing notification: “PinAlarmLock is protecting apps”; tap opens home (PIN first if the session is locked).

Every **250ms**, query `UsageStatsManager` events since the last timestamp. On `ACTIVITY_RESUMED` / `MOVE_TO_FOREGROUND`, if `shouldGate(packageName)`, start `MainActivity` in **gate** mode:

- `FLAG_ACTIVITY_NEW_TASK`
- `FLAG_ACTIVITY_NO_USER_ACTION`
- extra `extra_gate=true`

Starting that activity from the background requires `SYSTEM_ALERT_WINDOW`.

Register `ACTION_SCREEN_OFF` in the service (or a receiver the service owns) and call `LockSession.lock()`.

**Start the service** when a PIN exists, at least one package is enrolled, and both Usage access and overlay are granted. **Stop the service** when the enrolled set becomes empty. **BootReceiver** (`RECEIVE_BOOT_COMPLETED`) starts it again when those same conditions hold.

## MainActivity modes

**Home** (launcher, notification tap, default):

`Loading` → `SetupEnter` / `SetupConfirm` if no PIN, else `Locked` if session locked, else enrolment.

Enrolment is the unlocked destination: permission steps + launchable-app list.

**Gate** (`extra_gate=true`, from the watcher):

PIN pad only (setup is impossible here; if no PIN, finish immediately). Correct PIN: `LockSession.unlock()`, `finish()` so the enrolled app is visible. Do not navigate to enrolment. Wrong PIN: stay on the pad; do not `finish()`.

Remove `ON_STOP` → re-lock. `onAppBackgrounded()` must not lock the session.

## Enrolment UI

Launchable apps: `ACTION_MAIN` + `CATEGORY_LAUNCHER`, excluding the never-enrol list above, sorted by label.

Each row: icon, label, switch. Switch on writes the package into `protected_packages`.

Switches are **disabled** until Usage access and Display over other apps are granted. If either is missing, show two explicit actions that open:

1. `Settings.ACTION_USAGE_ACCESS_SETTINGS`
2. `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` for this package

Android 13+: request `POST_NOTIFICATIONS` so the FGS notification can post. Denial does not block enrolment; the system may still show a silent FGS indicator.

## Permissions

| Permission | Why |
| --- | --- |
| `VIBRATE` | Existing alarm haptic |
| `PACKAGE_USAGE_STATS` | Foreground app (AppOps / Usage access screen) |
| `SYSTEM_ALERT_WINDOW` | Start gate activity from the background |
| `FOREGROUND_SERVICE` | Watcher |
| `FOREGROUND_SERVICE_SPECIAL_USE` | FGS type on API 34+ |
| `POST_NOTIFICATIONS` | FGS notification on API 33+ |
| `RECEIVE_BOOT_COMPLETED` | Restart watcher |

No `INTERNET`. No `QUERY_ALL_PACKAGES` (launcher/home `<queries>` list enrolable apps). Manifest includes a `specialUse` FGS property. Play Console forms and listing copy live in `docs/play/`.

## Failures

| Situation | Behavior |
| --- | --- |
| Usage access or overlay revoked | Stop gating; home shows permission steps; no crash |
| Force-stop | Protection stops until the user opens PinAlarmLock or reboots (if still eligible) |
| Gate started with no PIN | `finish()` immediately |
| Detection lag | Up to one 250ms poll plus UsageEvents delay; accepted |
| User leaves gate with Back | Session stays locked; next poll shows the pad again if the enrolled app is still in front |

## Tests (JVM only)

No Robolectric for UsageStats.

- Existing: `PinHasher`, `PinRepositoryLogic`, `AlarmDuration`, `LockViewModel` PIN/alarm paths.
- `ProtectedAppsRepository` logic: add, remove, contains.
- `LockSession` + `shouldGate`: locked+enrolled → true; unlocked → false; own package → false; screen-off → locked again.
- `LockViewModel`: backgrounding does not change destination from enrolment to locked; gate success is a callback (`unlockAndFinish`) rather than `Dest.Unlocked`.

## Manual checks

1. Set PIN, grant Usage access + overlay, enrol a third-party app (not Settings).
2. Open that app → PIN pad. Wrong PIN → alarm, still covered. Correct PIN → app visible.
3. Open another enrolled app without screen off → no PIN.
4. Screen off, then open an enrolled app → PIN again.
5. Open PinAlarmLock from launcher while locked → PIN, then enrolment list.
6. Revoke Usage access → gating stops; home asks for the permission again.
