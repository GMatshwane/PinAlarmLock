# PIN reset

A user can replace the existing 4–6 digit PIN. Salted SHA-256 storage, alarm, enrolment, and the lock session stay as specified in the app-lock design. No `INTERNET`. No plaintext PIN.

## Change PIN (knows the current PIN)

On **Protected apps** only (unlocked home, never the gate overlay):

1. Enter current PIN.
2. Enter a new 4–6 digit PIN.
3. Confirm it.

Wrong current PIN: existing shake + ~10s alarm; stay on the current-PIN step. Confirm mismatch: existing “PINs do not match”; no alarm; back to the new-PIN step. Success: `PinRepository.setPin` overwrites `pin_salt` / `pin_hash`; stay unlocked on Protected apps. Enrolled packages unchanged.

## Forgot PIN (cannot unlock)

On the **lock pad** (home and gate): **Forgot PIN?**

If the phone has a screen lock, confirm it with `KeyguardManager.createConfirmDeviceCredentialIntent`. Success unlocks the in-memory session and opens the existing new-PIN setup screens. After confirm: `setPin`; if gate, finish so the enrolled app is visible; otherwise Protected apps.

If the phone has no screen lock (or the confirm intent is unavailable): stay locked and show that a screen lock is required. Clearing app storage remains the last resort.

Device-lock confirmation is recovery, not a replacement for the app PIN.

## Destinations

| Dest | Role |
| --- | --- |
| `ChangeCurrent` | Current PIN before choosing a new one |
| `SetupEnter` / `SetupConfirm` | New PIN (first launch, change, or forgot) |
| `Locked` | Unlock pad; Forgot PIN action |
| `Unlocked` | Protected apps; Change PIN action |

`ON_START` bootstrap still only runs when the session is locked, so a device-lock success that unlocks the session does not wipe setup.
