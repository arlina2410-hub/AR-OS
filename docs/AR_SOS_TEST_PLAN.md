# AR SOS — Test Plan and Physical Verification Matrix

This document defines the test matrix for the AR SOS module in AR/OS per AR SOS Master Prompt Section 13.
In compliance with the project honesty rule:
- **PASS**: Executed locally in automated JVM test suite and passed.
- **NOT RUN / REQUIRES PHYSICAL DEVICE**: Cannot be simulated or verified in headless container or emulator environments. Requires physical execution on a Motorola Moto G57 Power.
- Never mark physical hardware button, lock-screen wake, or cellular calling tests as passed until verified on the physical hardware.

---

## 1. Automated JVM Unit Tests (Executed)

| ID | Test Case | Target Component | Status | Notes |
|---|---|---|---|---|
| **U1** | 5 rapid Volume Down presses in sliding window triggers SOS | `VolumePressDetector` | **PASS** | Evaluated 5 valid presses within 2500ms sliding window. |
| **U2** | Slow presses exceeding window timeout and drop from buffer | `VolumePressDetector` | **PASS** | Presses spaced > 2500ms apart do not trigger; older presses slide out. |
| **U3** | Long hold (> 600ms) is rejected as hold | `VolumePressDetector` | **PASS** | Spec §6 `maxPressDurationMs = 600ms` enforced. |
| **U4** | Switch chatter/bounce (< 40ms) is rejected | `VolumePressDetector` | **PASS** | Spec §6 `minGapMs = 40ms` debounce threshold verified. |
| **U5** | Volume Up clears detection buffer immediately | `VolumePressDetector` | **PASS** | Cancels sequence and clears buffer to 0. |
| **U6** | Cooldown lockout (8000ms) blocks retrigger | `VolumePressDetector` | **PASS** | Spec §6 `cooldownMs = 8000ms` enforced; presses at 5100ms rejected. |
| **U7** | 5 presses taking 3.5s in Normal mode is rejected | `VolumePressDetector` | **PASS** | Regression test: verifies window enforcement against old per-gap bug. |
| **U8** | Presses held 450ms are accepted | `VolumePressDetector` | **PASS** | Regression test: verified under 600ms spec limit. |
| **U9** | Sensitivity presets modify only `windowMs` | `VolumePressDetector` | **PASS** | Relaxed (3500ms), Normal (2500ms), Strict (1800ms). |
| **U10** | Volume restorer callback hooks fire on start and clear | `VolumePressDetector` | **PASS** | Verified `onSequenceStarted` and `onSequenceCleared` firing. |
| **U11** | SOS Controller state machine and countdown lifecycle | `SosController` | **PASS** | Verified countdown start, cancel during countdown, cancel ignored during dialing, call-now shortcut, 0s direct dial, and privacy rule (zero phone numbers in log details). |
| **U12** | Contact validation rules and emergency blocklist | `ContactValidator` | **PASS** | Blocks emergency numbers (911, 112, 999, 000, 1091, 1098, +91 112), enforces 20 contact cap, trims and normalizes numbers. |
| **U13** | TimeFormatter 12-hour midnight, noon, and clock formatting | `TimeFormatter` | **PASS** | 0m -> 12:00 AM, 720m -> 12:00 PM, 1170m -> 7:30 PM, clock & date formatting. |
| **U14** | TimeFormatter 24-hour and boundary wrap formatting | `TimeFormatter` | **PASS** | 0m -> 00:00, 720m -> 12:00, -60m wraps to 11:00 PM / 23:00, 1440m wraps to 12:00 AM / 00:00. |
| **U15** | SimManager dual-SIM default and stored preference resolution | `SimManager` | **PASS** | Default SIM selection, single SIM resolution, and stored preference override tested. |
| **U16** | Adaptive Sovereignty scoring heuristic engine | `AdaptiveEngine` | **PASS** | Scoring weights and insight rules verified. |

---

## 2. Physical Device Verification Suite (Requires Motorola Moto G57 Power)

*The following physical tests CANNOT be executed in the development environment and are strictly marked **NOT RUN / REQUIRES PHYSICAL DEVICE**.*

| ID | Test Scenario | Device Requirement | Expected Behavior | Verification Status |
|---|---|---|---|---|
| **T1** | Accessibility service activation | Moto G57 Power running Android 14 (Hello UI) | Service toggle enables without crash. | **NOT RUN** — Needs physical phone |
| **T2** | Restricted settings bypass (Android 13+) | Moto G57 Power | If greyed out, 3-dot "Allow restricted settings" unblocks accessibility toggle. | **NOT RUN** — Needs physical phone |
| **T3** | Trigger from AR/OS foreground | Physical Volume Down button | 5 rapid presses start countdown with alarm haptic vibration. | **NOT RUN** — Needs physical phone |
| **T4** | Trigger from Android Home Screen | Physical Volume Down button | Background accessibility service detects 5 presses and launches SOS screen. | **NOT RUN** — Needs physical phone |
| **T5** | Trigger from third-party app (e.g. Chrome) | Physical Volume Down button | 5 presses detect and wake SOS UI without dropping normal volume events. | **NOT RUN** — Needs physical phone |
| **T6** | Trigger from Lock Screen (Screen Off -> Power pressed once) | Moto G57 Power keyguard | Pressing Power to light screen, followed by 5x Volume Down, launches SOS over keyguard with privacy shield intact. | **NOT RUN** — Needs physical phone |
| **T7** | Normal single Volume Down adjustment | Physical Volume Down button | Single or slow volume presses lower audio smoothly (`return false`). | **NOT RUN** — Needs physical phone |
| **T8** | Volume restoration | Moto G57 Power audio | Volume lowered by trigger sequence is restored to pre-trigger level if toggle enabled. | **NOT RUN** — Needs physical phone |
| **T9** | Lock-screen privacy isolation | Locked Moto G57 Power | When phone is locked, opening app manually shows sovereign shield; personal data, routines, and contacts are inaccessible until unlocked. | **NOT RUN** — Needs physical phone |
| **T10** | Motorola battery optimization exemption | Moto G57 Power Hello UI | App set to "Unrestricted" battery usage survives overnight background deep sleep. | **NOT RUN** — Needs physical phone |
| **T11** | Reboot resilience | Moto G57 Power restart | `BootReceiver` schedules readiness check and persistent status notification after reboot. | **NOT RUN** — Needs physical phone |
| **T12** | Direct Boot encryption barrier | Moto G57 Power after restart before first unlock | Accessibility service remains inactive until first PIN/pattern unlock due to Android Direct Boot security. | **NOT RUN** — Needs physical phone |
| **T13** | Conflicting accessibility key filter detection | Moto G57 Power with second a11y service | Readiness checker detects other services requesting `FLAG_REQUEST_FILTER_KEY_EVENTS`. | **NOT RUN** — Needs physical phone |
| **T14** | Contact selection via system picker | Android Contacts Provider | Native picker returns contact name and phone number cleanly without permission request. | **NOT RUN** — Needs physical phone |
| **T15** | Countdown cancellation via hardware Volume Up | Moto G57 Power | Volume Up clears pending buffer before 5th press. | **NOT RUN** — Needs physical phone |
| **T16** | Notification cancel action on lock screen | Android 14 Notification Shade | Tapping CANCEL on notification dismisses countdown without unlocking device. | **NOT RUN** — Needs physical phone |
| **T17** | Safe TEST mode simulation | Live SIM or No SIM | Full countdown executes, logs `TEST_NO_CALL`, places zero cellular calls. | **NOT RUN** — Needs physical phone |
| **T18** | Live cellular outgoing call (Tier 1) | Active cellular SIM | `TelecomManager.placeCall` initiates live call to primary contact without carrier block. | **NOT RUN** — Needs physical phone |
| **T19** | Live call fallback (Tier 2) | Active cellular SIM | If TelecomManager fails, fallback to `Intent.ACTION_CALL` launches dialer. | **NOT RUN** — Needs physical phone |
| **T20** | Live call fallback (Tier 3 / Fallback 2) | Active cellular SIM | If dialer fails, HIGH-importance notification with RETRY DIAL appears. | **NOT RUN** — Needs physical phone |
| **T21** | Dual SIM prompt avoidance | Dual SIM configuration | Primary SIM confirmed in settings avoids system SIM selection dialog stall. | **NOT RUN** — Needs physical phone |
| **T22** | Built-in Emergency SOS non-interference | Moto G57 Power (5x Power button) | Android's native Emergency SOS (5x Power) functions normally and is never intercepted or disabled by AR SOS. | **NOT RUN** — Needs physical phone |
| **T23** | Guided Setup Wizard first-run auto-advance | Moto G57 Power | Steps auto-advance immediately as permissions are allowed in system dialogs. | **NOT RUN** — Needs physical phone |
| **T24** | Permission Center deep links & live status | Moto G57 Power | Permission Center reflects granted status in real-time when returning from Settings. | **NOT RUN** — Needs physical phone |
| **T25** | EXTRA_PHONE_ACCOUNT_HANDLE routing on dual SIM | Dual active SIMs on Moto G57 Power | Real test call dials directly on the selected SIM without opening system SIM chooser. | **NOT RUN** — Needs physical phone |
| **T26** | One-tap live test call trigger from Safety tab | Active cellular SIM | "Place one real test call now" button dials primary contact immediately in LIVE mode. | **NOT RUN** — Needs physical phone |
