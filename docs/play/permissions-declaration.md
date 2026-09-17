# Play permission declarations

The app does **not** include `QUERY_ALL_PACKAGES`. Enrolment and home-app exclusion use the manifest `<queries>` intents for `MAIN` + `LAUNCHER` and `MAIN` + `HOME`.

Declare the restricted permissions Play still asks about.

## Usage access (`PACKAGE_USAGE_STATS`)

- Core functionality: detect when an enrolled app comes to the foreground so the PIN pad can cover it.
- Without it the lock cannot see app switches and enrolled apps open unlocked.
- Video: enrol an app → switch to it → PIN pad appears.
- Data: usage events stay on device; not sent anywhere.

## Display over other apps (`SYSTEM_ALERT_WINDOW`)

- Core functionality: draw the PIN pad in front of an enrolled app.
- Without it the pad cannot cover the enrolled app.
- Not used for ads, phishing, or blocking system UI.
- Video: same as usage access; show the pad sitting on top of the enrolled app.

## Special-use foreground service (`FOREGROUND_SERVICE_SPECIAL_USE`)

- Subtype already in the manifest: `app lock`.
- Why special-use: watching foreground-app changes to present a PIN gate is not camera/microphone/location/playback.
- Visible notification: “PinAlarmLock is protecting apps”.
- Stops when no PIN is set, no apps are enrolled, or required permissions are missing (`WatchEligibility`).

## Other permissions (no extra Play form beyond Data safety)

| Permission | Why |
| --- | --- |
| `POST_NOTIFICATIONS` | Ongoing watcher notification on Android 13+ |
| `VIBRATE` | Wrong-PIN haptic |
| `RECEIVE_BOOT_COMPLETED` | Restart the watcher after reboot |
| `FOREGROUND_SERVICE` | Required parent of the special-use service |

## `QUERY_ALL_PACKAGES`

Do not add this permission back for Play. Broad app inventory is not an approved use for this app locker; targeted `<queries>` is enough to list launchable apps.
