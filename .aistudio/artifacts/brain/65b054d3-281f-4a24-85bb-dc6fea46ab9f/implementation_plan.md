# AR/OS — Full System Architectural Implementation Plan

Complete offline-first personal operating system for Android, unifying seven local engines: **AR SOS (Personal Safety)**, **Routine Engine**, **Focus Engine**, **Digital Guardian**, **Phone Activity Ledger**, **Discipline Engine**, and **Adaptive Intelligence**.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed during the Phase 1 alignment interview:
> - **Module Scope**: Proceed with complete, functional offline architectures for all six remaining modules now rather than leaving placeholder cards.
> - **Visual Direction**: Premium iOS-inspired dark glassmorphism with crisp typographic hierarchy, high-contrast status pills, semi-transparent frosted cards (`0xFF1E293B` at alpha 70–85%), and subtle ambient borders.
> - **Data & Privacy Policy**: 100% offline-first. Zero internet permissions, no external cloud dependencies, no analytics, and all databases stored in encrypted device storage.

---

## 1. Overview & Core Concept

**AR/OS** transforms a standard Android smartphone into a distraction-resistant, sovereign personal operating system. Rather than acting as a simple utility, it provides an interconnected suite of 7 offline engines:

1. **AR SOS (Safety System)**: Global 5× Volume Down hardware detection (unlocked home/app screens and lock-screen wake), 3-tier dialing fallback (`TelecomManager` → `ACTION_CALL` → retry alert), silent alarm haptics, lock-screen privacy shielding, and local audit logging.
2. **Routine Engine**: Offline recurring daily rhythms and habit architecture (Morning Kickoff, Deep Work Blocks, Evening Wind-Down) with completion streaks, timer tracking, and persistent daily state.
3. **Focus Engine**: Intentional flow sessions, distraction friction, Pomodoro timer cycles, session logging, and notification shielding reminders.
4. **Digital Guardian**: Local privacy and device hygiene scanner inspecting installed apps for high-risk hardware permissions (Camera, Microphone, Background Location) with zero network exposure and an offline Device Sovereignty Score.
5. **Phone Activity Ledger**: Private on-device ledger recording intentional sessions vs. impulsive phone opens, daily screen time buckets, and hourly activity histograms rendered with native Compose Canvas.
6. **Discipline Engine**: Cognitive friction rules, delayed entry challenges (breath timer, pledge typing, arithmetic challenge) to interrupt compulsive phone pick-ups.
7. **Adaptive Intelligence**: Local heuristic intelligence analyzing ledger and routine consistency to generate daily Sovereignty Insights and proactive habit recommendations without cloud AI or API keys.

---

## 2. User Experience & Visual Design

### Aesthetic Direction
- **Theme**: Luxury Dark Glassmorphism (`#090D16` deep navy obsidian background with `#1E293B` layered frosted cards and `#EF4444` / `#38BDF8` neon status accents).
- **Surface Depth**: Layered elevated containers with subtle 1dp hairline borders (`#334155`), tonal elevation, and backdrop blur emulation.
- **Typography Hierarchy**: Distinctive bold display headers, monospace numeric counters, and legible body typography adhering to M3 scaling rules.
- **Navigation Shell**: Top scrollable glass tab bar with status pills and bottom action bar providing instant one-tap transitions between all 7 modules and the central system dashboard.
- **Lock-Screen Privacy**: When the device keyguard is locked, all private routines, contacts, ledger entries, and guardian reports are strictly inaccessible; only the emergency SOS countdown is rendered over the lock screen.

---

## 3. Technical Architecture & Data Strategy

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              AR/OS SHELL (MainActivity)                      │
│   ┌──────────────────────────────────────────────────────────────────────┐   │
│   │ Top Glass Nav Tab Bar: [Dashboard] [SOS] [Routines] [Focus]         │   │
│   │                        [Guardian]  [Ledger] [Discipline] [Adaptive]   │   │
│   └──────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │
     ┌─────────────────────────────────┴─────────────────────────────────┐
     ▼                                                                   ▼
┌───────────────────────────┐                       ┌───────────────────────────┐
│     PLATFORM SERVICES     │                       │     DOMAIN & ENGINES      │
│  - ArSosAccessibility     │  Key Event Filtering  │  - VolumePressDetector    │
│  - VolumeRestorer         │ ────────────────────► │  - SosController          │
│  - AndroidHaptics         │                       │  - RoutineController      │
│  - TelecomCallRequester   │                       │  - FocusController        │
│  - ReadinessWorker        │                       │  - GuardianScanner        │
│  - BootReceiver           │                       │  - DisciplineGatekeeper   │
│  - NotificationManager    │                       │  - AdaptiveHeuristics     │
└─────────────┬─────────────┘                       └─────────────┬─────────────┘
              │                                                   │
              └─────────────────────────┬─────────────────────────┘
                                        ▼
                      ┌───────────────────────────────────┐
                      │    LOCAL REPOSITORIES & STORAGE   │
                      │  - ContactRepository (Room)       │
                      │  - SosEventLog (Room)             │
                      │  - RoutineRepository (Room)       │
                      │  - FocusRepository (Room)         │
                      │  - LedgerRepository (Room)        │
                      │  - DisciplineRepository (Room)    │
                      │  - GuardianRepository (Room)      │
                      │  - SettingsRepository (DataStore) │
                      └─────────────────┬─────────────────┘
                                        ▼
                      ┌───────────────────────────────────┐
                      │    ROOM DATABASE (ar_os.db)       │
                      │  sos_contacts | sos_events        │
                      │  routines | focus_sessions        │
                      │  activity_ledger | discipline_rules│
                      │  guardian_scans | adaptive_insights│
                      └───────────────────────────────────┘
```

### Data Entities & Schema Strategy

1. **`sos_contacts`**: `id`, `name`, `number`, `role` (PRIMARY/BACKUP), `sortOrder`, `createdAt`
2. **`sos_events`**: `id`, `timestamp`, `type`, `mode`, `contactName`, `deviceLocked`, `screenInteractive`, `detail` (auto-pruned to 500)
3. **`routines`**: `id`, `title`, `description`, `timeMinutesOfDay`, `daysMask`, `durationMinutes`, `completedToday`, `streakCount`, `category` (MORNING, FOCUS, EVENING, HEALTH)
4. **`focus_sessions`**: `id`, `startTime`, `endTime`, `durationMinutes`, `sessionType` (POMODORO, DEEP_WORK, BREATH), `completed`, `interruptions`
5. **`activity_ledger`**: `id`, `timestamp`, `eventCategory` (UNLOCK, FOCUS_START, SOS_TRIGGER, DISCIPLINE_PASS), `durationSeconds`, `appLabel`, `isIntentional`
6. **`discipline_rules`**: `id`, `name`, `challengeType` (BREATH_GATE, PLEDGE_TYPING, ARITHMETIC), `frictionSeconds`, `targetAppOrHabit`, `isActive`, `passCount`
7. **`guardian_scans`**: `id`, `timestamp`, `sovereigntyScore` (0–100), `cameraAppsCount`, `micAppsCount`, `locationAppsCount`, `highRiskAppsCount`
8. **`adaptive_insights`**: `id`, `timestamp`, `headline`, `body`, `scoreImpact`, `isDismissed`

---

## 4. Implementation Phasing & Execution Plan

### Stage 1: Foundation & Project Config
- Update `app/build.gradle.kts`: verify dependencies for Compose M3, Room 2.7, DataStore Preferences, WorkManager 2.10, Coroutines Test.
- Update `AndroidManifest.xml`: retain strictly the 4 necessary permissions (`CALL_PHONE`, `POST_NOTIFICATIONS`, `VIBRATE`, `RECEIVE_BOOT_COMPLETED`, plus service `BIND_ACCESSIBILITY_SERVICE`). Ensure zero network permissions.
- Custom adaptive icon & non-adaptive raster icons configuration with deep obsidian & crimson shield assets.

### Stage 2: Database & Core Domain Layer
- Implement Room entities and DAOs for all 7 modules:
  - `SosContactDao`, `SosEventDao`
  - `RoutineDao`
  - `FocusDao`
  - `ActivityLedgerDao`
  - `DisciplineDao`
  - `GuardianDao`
  - `AdaptiveInsightDao`
- Assemble `AppDatabase` (version 2 with fallbackToDestructiveMigration for initial development).
- Implement pure domain controllers and repositories:
  - `VolumePressDetector` (sliding-window 5-press logic, bounce/hold rejection)
  - `SosController` (state machine, countdown, emergency calling)
  - `RoutineController` (habit tracking, streaks, completion toggles)
  - `FocusController` (Pomodoro timer, break intervals, session logging)
  - `GuardianScanner` (package manager permissions analysis)
  - `DisciplineGatekeeper` (friction challenge validation)
  - `AdaptiveEngine` (sovereignty score calculation & local insights)

### Stage 3: Platform Integrations & Background Workers
- `ArSosAccessibilityService` with `ar_sos_accessibility_config.xml`: global hardware key filtering returning `false`, snapshotting volume with `VolumeRestorer`, passing trigger to `SosController`.
- `TelecomCallRequester`: 3-tier dialing fallback with speakerphone and airplane-mode checks.
- `AndroidHaptics`: silent alarm-usage vibration patterns.
- `SosNotifications`: channels `sos_status`, `sos_alert`, `sos_warning` with lock-screen action bypass.
- `ReadinessChecker` & `ReadinessWorker`: 8-point system audit and conflicting key-filter accessibility service detection.
- `BootReceiver`: rescheduling background audits and logging reboot status.

### Stage 4: UI/UX Layer (Dark Glassmorphic Jetpack Compose)
- **Central Navigation & Shell**: `AppNav` supporting top glass tab routing across all destinations with predictive BackHandler.
- **Dashboard Screen**: Unified sovereignty overview card, AR SOS readiness banner, quick routine progress, active focus launcher, and disclaimer notice.
- **AR SOS Suite**: Setup checklist (with restricted-settings unlock guide), Trusted Contacts manager (with native zero-permission picker), Settings, and Trigger Lab in-memory telemetry visualizer.
- **Routine Engine Screen**: Active routine schedule, streak counters, check-off cards, and routine creator modal.
- **Focus Engine Screen**: Minimalist circular countdown timer, mode selector (Deep Work / Pomodoro / Reset), ambient soundless visuals, and session history.
- **Digital Guardian Screen**: Device sovereignty dial (0–100), permission risk breakdown (Microphone, Camera, Background Location), and system audit recommendations.
- **Phone Activity Ledger Screen**: Hourly activity distribution histogram (Canvas-drawn), daily unlocks summary, and intentionality ratio metrics.
- **Discipline Engine Screen**: Active friction gates, interactive challenge simulator (e.g. 10-second breath gate and deliberate intention pledge typing), and bypass resistance logs.
- **Adaptive Intelligence Screen**: Daily sovereignty analysis, streak insights, and localized algorithmic suggestions.
- **Lock-Screen Privacy Shield**: Enforces strict privacy when `KeyguardManager.isKeyguardLocked == true`, displaying only the `SosCountdownScreen` during active triggers and shielding all personal data otherwise.

### Stage 5: Verification & Automated Testing
- Implement automated tests for:
  - U1–U10: Hardware volume press detector (5 fast presses, slow presses, holds, bounces, cooldown, sensitivity presets).
  - U11: SOS controller state machine (countdown, cancel, callNow, TEST mode vs LIVE mode).
  - U12: Contact validator (number normalization, emergency number block, 20-cap).
  - U13: Routine completion & streak calculation logic.
  - U14: Focus timer state machine and session persistence.
  - U15: Discipline friction gate challenge validation.
  - U16: Adaptive sovereignty score calculation heuristics.
- Compile and verify via `compile_applet`.
- Update `docs/AR_SOS_TEST_PLAN.md` and create `docs/AR_OS_COMPLETION_TRACKER.md`.

---

## 5. Next Step

Awaiting your review and approval. Once approved, I will begin implementing the database schemas, domain engines, and glassmorphic UI across all seven modules.
