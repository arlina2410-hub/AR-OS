# AR/OS UI Verification & Test Plan

**Device Target:** Motorola Moto G57 Power (Android 14 / Hello UI)  
**Execution Environment:** Native Android JVM Tests & Physical Device Run  

---

## 1. Visual & Token Conformance Verification

| Check ID | Component / Area | Acceptance Criteria | Status |
|---|---|---|---|
| VIS-01 | Design Tokens | Colors resolve strictly to semantic `ArOsTheme.colors` tokens without hardcoded hex values in composables. | VERIFIED |
| VIS-02 | Inset Groups | `ArOsGroup` clips content with `ArOsTheme.shapes.lg`, renders 1dp hairline border, and insets dividers by 56dp when leading icons are present. | VERIFIED |
| VIS-03 | Tabular Figures | Clocks, intervals, timers, and metrics format with `fontFeatureSettings = "tnum"`. Character widths for `00:00:00` and `11:11:11` are identical. | VERIFIED (in `ArOsDesignGallery`) |
| VIS-04 | Status Indicators | `ArOsStatusPill` renders crisp status dots and high-contrast labels across Success, Warning, Danger, and Accent tones. | VERIFIED |
| VIS-05 | Design Gallery | Debug-only `ArOsDesignGallery` screen renders all tokens, buttons, segments, and row states without crashing. | VERIFIED |

---

## 2. Touch Targets & Accessibility Conformance

| Check ID | Requirement | Standard | Status |
|---|---|---|---|
| ACC-01 | General Touch Targets | All interactive buttons, chips, icons, and tab items meet or exceed 48dp x 48dp. | VERIFIED |
| ACC-02 | AR SOS Cancel Button | Safety-critical cancel button strictly enforces 64dp vertical height with full-width horizontal span. | VERIFIED |
| ACC-03 | Contrast Ratios | Primary text on dark surfaces exceeds 7:1; secondary text exceeds 4.5:1. | VERIFIED |
| ACC-04 | Reduced Motion | `rememberReducedMotion()` observes system animator duration scale; disabling animations removes scale springs. | VERIFIED |
| ACC-05 | TalkBack Semantics | All icon buttons declare meaningful `contentDescription` or are explicitly marked null for decorative glyphs. | VERIFIED |

---

## 3. Shell & Navigation Hierarchy

| Check ID | Transition | Expected Behavior | Status |
|---|---|---|---|
| NAV-01 | Bottom Navigation | 5 persistent tabs: Core (Home), Rhythms, Focus, Ledger, Safety. Tapping switches tabs instantly with haptic feedback. | VERIFIED |
| NAV-02 | Sub-Screen Pushes | Guardian, Friction, Adaptive Insights, Gallery, and SOS Setup open as detail views. | VERIFIED |
| NAV-03 | Back Handling | Pressing system Back or top-bar back arrow on any secondary screen returns to `DASHBOARD` rather than exiting. | VERIFIED |
| NAV-04 | Lock Screen Privacy | When `isKeyguardLocked == true`, `LockedPrivacyShieldScreen` obscures all routine, contact, focus, and ledger data. | NOT VERIFIED — needs physical device |

---

## 4. Safety-Critical AR SOS Regression Protocol

| Check ID | Trigger / Action | Expected Result | Verification Level |
|---|---|---|---|
| SOS-01 | Countdown Screen | Prominent pulsing countdown with 64dp Cancel button and Call Now override button. | VERIFIED in Compose compilation & preview |
| SOS-02 | Cancel Action | Touching Cancel immediately aborts countdown and returns controller to Idle state. | VERIFIED in unit tests |
| SOS-03 | Live Mode Gate | Switching to LIVE mode requires explicit two-step user confirmation dialog. | VERIFIED |
| SOS-04 | Emergency Number Block | Numbers matching 911, 112, 999, or 000 are rejected with validation error. | VERIFIED in `ContactValidatorTest` |
| SOS-05 | 5x Volume Down Key Detection | Sliding window accepts 5 presses within 65ms–850ms interval constraints. | VERIFIED in `VolumePressDetectorTest` |
| SOS-06 | Lock Screen Trigger | Triggering from lock screen wakes device and launches `SosCountdownScreen`. | NOT VERIFIED — needs physical device |
| SOS-07 | Real Cellular Dialing | `ACTION_CALL` launches active SIM call on Moto G57 Power in LIVE mode. | NOT VERIFIED — needs physical device |

---

## 5. Moto G57 Power Performance Protocol

To verify 60 FPS / 120 FPS frame rate stability on device:
1. Connect Motorola Moto G57 Power via USB with Developer Options enabled.
2. Run frame rendering profiling:
   ```bash
   adb shell dumpsys gfxinfo com.aistudio.aros.kpxwzm reset
   ```
3. Scroll through `ActivityLedgerScreen`, `RoutineScreen`, and `DashboardScreen`.
4. Run:
   ```bash
   adb shell dumpsys gfxinfo com.aistudio.aros.kpxwzm framestats
   ```
5. Confirm 99th percentile frame completion time is `< 16.6ms` (zero dropped frames).
6. Verify battery optimization whitelist under Settings > Apps > AR/OS > Battery > Unrestricted.
