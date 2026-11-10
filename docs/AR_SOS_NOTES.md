# AR SOS — Technical Architecture Notes & Design Decisions

This document details the architectural boundaries, system decisions, and safety considerations for the AR SOS module in AR/OS.

---

## 1. Safety-Critical Boundary & Non-Interference
- **Android Emergency SOS Non-Interference**: AR/OS listens only to `KEYCODE_VOLUME_DOWN` key events via an `AccessibilityService` (`ArSosAccessibilityService`) configured with `accessibilityFlags="flagDefault|flagRequestFilterKeyEvents"`. It never registers for or intercepts `KEYCODE_POWER`. The Android OS native 5x Power button Emergency SOS remains completely untouched and functional at all times.
- **Non-Consuming Key Flow (`return false`)**: `onKeyEvent` in `ArSosAccessibilityService` always returns `false`. This ensures that normal system volume adjustment is never blocked or consumed. If the user presses Volume Down once or twice, the OS handles it normally.
- **Emergency Number Safety Blocklist**: AR SOS is strictly designed to contact personal guardians, family, or friends. Official emergency dispatch services (such as 911, 112, 999, 000, 100, 101, 102, 108, 1091, 1098, and international variations like +91 112) are explicitly blocked from being saved as contacts. If the user attempts to add an official emergency number, the app explains: *"Emergency-service numbers can't be saved here — the phone's own Emergency SOS handles those."*

---

## 2. Hardware Trigger Algorithm (Spec §6)
- **Parameters**:
  - `requiredPresses`: 5
  - `windowMs`: 2500 ms (Normal), 3500 ms (Relaxed), 1800 ms (Strict)
  - `maxPressDurationMs`: 600 ms
  - `minGapMs`: 40 ms (switch bounce debounce threshold)
  - `cooldownMs`: 8000 ms
- **Pure Domain Engine**: `VolumePressDetector` has zero `android.*` imports. Android `KeyEvent` is mapped to `SosKeyCode` and `SosKeyAction` enums at the service boundary.
- **Sliding Window**: Presses are tracked by their `ACTION_DOWN` timestamps. Each valid `ACTION_UP` appends the down time, drops entries older than `currentDownTime - windowMs`, and triggers only when 5 valid presses fall within the window.
- **Hold and Abort Handling**: Long holds (> 600ms) or down repeats (`repeatCount > 0`) clear the sequence buffer. Any press of `KEYCODE_VOLUME_UP` immediately aborts the active sequence.

---

## 3. Lock-Screen Privacy & Keyguard Integration
- **Lock-Screen Wake**: When a valid 5x trigger occurs while the phone is locked, the app sets `setShowWhenLocked(true)` and `setTurnScreenOn(true)` on `MainActivity` to display `SosCountdownScreen`.
- **Sovereign Privacy Shield**: Under normal circumstances when the phone is locked and AR/OS is opened, `MainActivity` displays `LockedPrivacyShieldScreen`. All personal data—routines, focus sessions, contact details, financial ledgers, and discipline rules—are completely shielded from view.
- **Task Cleanup**: When an SOS sequence completes or is cancelled after being triggered from the lock screen, `MainActivity` clears window flags and calls `finishAndRemoveTask()` to leave the phone securely locked.
- **Zero Full-Screen Intent Abuses**: `USE_FULL_SCREEN_INTENT` permission was removed. Normal notifications never launch disruptive full-screen overlays.

---

## 4. 3-Tier Calling Strategy
1. **Tier 1 (Direct Telephony)**: `TelecomManager.placeCall(Uri, Bundle)` with speakerphone request if enabled in settings (`startCallOnSpeaker`).
2. **Tier 2 (Fallback 1)**: `Intent.ACTION_CALL` initiated with `FLAG_ACTIVITY_NEW_TASK`.
3. **Tier 3 (Fallback 2)**: If live dialing fails, a `HIGH`-importance persistent notification is posted with a `RETRY DIAL` action and tap-to-open AR SOS.
4. **Backup Guardians**: After dialing the primary contact, a notification with one-tap dial actions for up to three backup contacts is posted.
5. **Privacy in Telephony Logs**: Actual phone numbers are never recorded in `SosEvent.detail` or exposed in public notifications. Only contact names are logged.

---

## 5. Offline-First & No Cloud Dependencies
- Zero Firebase, zero network permissions (`android.permission.INTERNET` is omitted from `AndroidManifest.xml`).
- All state, events, contacts, and preferences reside in local SQLite databases (Room) and encrypted preferences.

---

## 6. V2 Platform & Guided Setup Enhancements
- **Guided Setup Wizard (`SetupWizardScreen`)**: Multi-step onboarding automatically presented on first launch or when essential permissions are missing. Auto-advances immediately as permissions are granted in the system dialog.
- **Permission Center (`PermissionCenterScreen`)**: Centralized status dashboard mapping Android technical permissions to plain human concepts (Emergency Calling, Phone State / SIM, Notifications, Battery Unrestricted, SOS Button Listener). Each item displays "Allowed" or "Needs action" with a one-tap deep link to the relevant system dialog or Settings screen.
- **Motorola Android 13+ Restricted Settings Flow**: Sideloaded APKs on Android 13+ (including Moto G57 Power Hello UI) disable accessibility services by default. The wizard and permission center detect this condition, deep-link to App Info (`ACTION_APPLICATION_DETAILS_SETTINGS`), and instruct the user in plain terms: *Tap the 3 dots (⋮) in the top-right corner, select "Allow restricted settings", authenticate with PIN/fingerprint, then return to enable the service.*
- **Direct Calling SIM Selection without Global Settings (`EXTRA_PHONE_ACCOUNT_HANDLE`)**: For dual-SIM phones like the Moto G57 Power, `TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE` is attached to `telecomManager.placeCall(uri, extras)` and `ACTION_CALL` fallback intent. This directs the carrier call directly to the user-confirmed SIM without requiring global `WRITE_SECURE_SETTINGS` or showing the system dual-SIM picker dialog.
- **Battery Optimization Direct Intent (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)**: Launches the system exemption prompt directly targeting the app package, falling back to general battery optimization settings if unsupported.
- **12-Hour System Time Default**: All clock displays, audit logs, and routine pickers default to 12-hour format with AM/PM indicators ("7:30 PM", "12:00 AM", "12:00 PM"), with a 12-hour/24-hour preference available in Settings.
