# AR Guard — Technical Architecture Notes & Security Boundaries

This document details the architectural boundaries, security design, and Motorola Moto G57 Power operational characteristics for the **AR Guard** module in AR/OS.

---

## 1. Threat Model & Purpose
- **Primary Goal**: Prevent unauthorized access to personal apps and sensitive records (such as financial ledgers, private routines, guardian phone numbers, and notes) if the device's main screen lock (PIN, pattern, or biometric) has been observed, coerced, or bypassed by an acquaintance.
- **Why No Face Unlock**: Camera-based face recognition is spoofable with photos or 3D masks, battery-heavy when checking on every screen unlock, and fails in low-light environments.
- **Why No Fingerprint in V1**: Android's `BiometricPrompt` authenticates against *any* fingerprint enrolled in the device settings. If an employer, partner, or family member has their fingerprint saved on the device, Android cannot restrict access to "only the owner". The 6–12 digit AR Guard PIN ensures true single-owner access.

---

## 2. Gate Presentation Architecture
- **Separate Service (`ArGuardAccessibilityService`)**: Completely decoupled from `ArSosAccessibilityService`. AR SOS remains safety-critical and exclusively handles 5x Volume Down detection. AR Guard handles gate presentation only.
- **Configuration**:
  - `accessibilityEventTypes=""` (listens to 0 window events)
  - `canRetrieveWindowContent="false"` (zero screen or typing inspection)
  - No key filtering flags. Hardware keys (including Volume Down) are never intercepted by AR Guard.
- **Window Layer**: `WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY`.
- **Window Flags**:
  - `FLAG_NOT_FOCUSABLE`: Hardware buttons (including Volume Down for AR SOS) pass straight through to the system.
  - `FLAG_LAYOUT_IN_SCREEN`: Fully covers the entire viewport including system navigation and status bars.
  - `FLAG_SECURE`: Prevents screenshots and screen recording while the gate is visible.
- **Compose ViewTree Hosting**: The overlay hosts a `ComposeView` with manually attached `ViewTreeLifecycleOwner`, `ViewTreeSavedStateRegistryOwner`, and `ViewTreeViewModelStoreOwner` via `OverlayLifecycleOwner`.

---

## 3. Safety-Critical Non-Interference: SOS & Telephony
- **Active SOS Priority**: `ArGuardAccessibilityService` monitors `SosController.state`. If an emergency trigger occurs (state is not `Idle`), the gate overlay is immediately dismissed so the user can interact with the SOS countdown, cancellation, or call controls.
- **Active Phone Call Priority**: `ArGuardAccessibilityService` monitors telephony call state (`TelephonyManager.CALL_STATE != IDLE`). If a phone call is active or incoming, the gate is dismissed so the user can answer and speak without hindrance.
- **When Both End**: If the session is still locked, the gate re-evaluates and displays cleanly.

---

## 4. Cryptographic PIN Storage & Anti-Brute-Force Ladder
- **Hashing**: PBKDF2WithHmacSHA256 with a 16-byte cryptographically secure salt (`SecureRandom`) and 100,000 iterations.
- **Comparison**: Constant-time `MessageDigest.isEqual` to prevent timing attacks.
- **Storage**: Only the Base64-encoded salt and hash are stored in `ar_os_guard_settings` DataStore. The plaintext PIN is never logged, stored, or surfaced anywhere in the app.
- **Lockout Ladder**:
  - Tries 1–5: 5 free tries.
  - Try 6: 30 seconds lockout.
  - Try 7: 1 minute (60s) lockout.
  - Try 8: 5 minutes (300s) lockout.
  - Try 9: 15 minutes (900s) lockout.
  - Try 10+: 1 hour (3600s, capped) lockout.
  - The attempt counter is reset to 0 upon entering the correct PIN. Data is **never wiped**.

---

## 5. Recovery Code
- A 16-character unambiguous alphanumeric code (omitting `0`, `O`, `1`, `I`) is generated at setup and displayed once.
- Stored as a PBKDF2 hash using the same cryptographic salt.
- Allows resetting the PIN if forgotten, while enforcing the exact same lockout ladder to prevent brute-forcing.

---

## 6. Honest Limits & Failure Modes
- **Hardware Reboot / Safe Mode**: AR Guard cannot prevent an attacker from rebooting the device into safe mode (which disables all third-party accessibility services) or wiping the device from recovery mode.
- **Recommendation**: Keep Google Find Hub (Find My Device) and Android factory reset protection active as the second layer of security.
- **Corrupt Data Policy**: If the guard settings DataStore is corrupted or unreadable, AR Guard defaults to `DISABLED`. It will **never brick or lock the owner out** due to storage corruption.

---

## 7. Lost Guard (Anti-Theft Window Defense)
- **Primary Goal**: Intercept and close the system screens a thief would use to block device tracking before they can switch to airplane mode, power off the phone, or open settings.
- **Execution Rules (when AR Guard is LOCKED)**:
  - **Power Menu**: Intercepts `com.android.systemui` `GlobalActions` / `PowerMenu` and issues `GLOBAL_ACTION_BACK` followed by `GLOBAL_ACTION_HOME`.
  - **Notification Shade & Quick Settings**: Closes the shade via `GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE` (API 31+). On older versions, falls back to `GLOBAL_ACTION_BACK` then `GLOBAL_ACTION_HOME`.
  - **Settings & Package Installers**: Closes `com.android.settings` and `com.android.packageinstaller` / `com.google.android.packageinstaller` via `GLOBAL_ACTION_HOME`.
  - **Throttling**: Maximum 1 defense action per 300ms to avoid UI stutter or action flooding.
  - **Non-Interference**: Lost Guard rules **never run** during an active SOS countdown/call or active phone call.
- **Guard Lab (Learn Mode)**:
  - 10-minute auto-expiring inspector (`learnModeExpiresAtElapsedRealtime`). A reboot cannot leave it on past its deadline.
  - Captures up to 50 foreground window events (package name, class name, 12-hour timestamp).
  - Never inspects, logs, or stores screen contents or typing (`canRetrieveWindowContent="false"`).
  - Allows user to tap "Block this window while locked" to add custom rules.
- **Tamper & Defense Log**:
  - Plain English log recording closed power menus, dismissed shades, blocked targets, airplane mode toggles while locked, and AR Guard service connect/disconnect events with 12-hour timestamps.
- **Uninstall Protection (Device Admin)**:
  - Optional `DeviceAdminReceiver` declaring minimal `<force-lock/>` policy.
  - Prevents uninstallation of AR/OS without first deactivating device admin in Android Security settings.
