# PIN Alarm Lock implementation plan

## Layout

Standard Android app under `app/`:

- `MainActivity`
- `data/PinHasher.kt`, `data/PinRepository.kt` (`Context.pinDataStore`)
- `alarm/AlarmDuration.kt`, `alarm/AlarmPlayer.kt`
- `ui/LockUiState.kt`, `ui/LockViewModel.kt`
- `ui/navigation/AppNavHost.kt`
- `ui/setup/SetupScreen.kt`
- `ui/lock/PinPad.kt`, `ui/lock/LockScreen.kt`
- `ui/unlocked/UnlockedScreen.kt`
- `ui/theme/*`
- JVM tests under `app/src/test/...`

## Commit chunks

1. Scaffold (Gradle, manifest, empty Activity, theme, docs)
2. PinHasher + tests
3. PinRepository / PinRepositoryLogic + in-memory fake tests
4. AlarmDuration + AlarmPlayer
5. LockViewModel + tests (lambda fakes)
6. Setup UI
7. Lock UI (pad, shake, unlocked)
8. Wire navigation + MainActivity bootstrap / `ON_STOP`
9. Polish (README, strings, permissions)

## Test plan

`./gradlew :app:testDebugUnitTest :app:assembleDebug`

Cover `PinHasher`, `PinRepositoryLogic`, `AlarmDuration`, and `LockViewModel` on the JVM (no Robolectric).
