# AR/OS Design System Specification

## 1. Vision & Identity

AR/OS is a sovereign, offline-first personal operating system built natively in Kotlin and Jetpack Compose for the **Motorola Moto G57 Power**. Its aesthetic reflects a premium, iOS-inspired dark glassmorphism system characterized by:
- Deep obsidian and slate surface hierarchies.
- Laser-focused neon accents (Sky Cyan `#38BDF8` and Sovereign Green `#10B981`).
- Clean hairline surface borders (1dp, `ArOsTheme.colors.hairline`).
- Inset-grouped list styling (`ArOsGroup`, `ArOsRow`) with rounded container clipping.
- Monospaced tabular numeral figures (`fontFeatureSettings = "tnum"`) across all clocks, timers, interval graphs, and metric readouts.
- Organic tactile motion curves and haptic feedback.

---

## 2. Color Tokens (`ArOsColors.kt`)

| Token | Dark Palette (Default) | Light Palette | Semantic Purpose |
|---|---|---|---|
| `background` | `#090D16` | `#F8FAFC` | Primary viewport backdrop |
| `surface` | `#0F172A` | `#FFFFFF` | Group card and container base |
| `surfaceRaised` | `#1E293B` | `#F1F5F9` | Secondary cards, pill backgrounds, badges |
| `surfaceOverlay` | `#26334D` | `#FFFFFF` | Dialogs, action sheets, floating modals |
| `contentPrimary` | `#F8FAFC` | `#0F172A` | High-contrast primary text & labels |
| `contentSecondary`| `#94A3B8` | `#475569` | Subordinate descriptions & subtitles |
| `contentTertiary` | `#64748B` | `#94A3B8` | Section headers, captions, metadata |
| `accent` | `#38BDF8` | `#0284C7` | Interactive controls, primary focus |
| `accentPressed` | `#0284C7` | `#0369A1` | Damped active press state |
| `separator` | `#1E293B` | `#E2E8F0` | Inset row divider lines (56dp start inset) |
| `hairline` | `#334155` | `#CBD5E1` | 1dp outer border for cards & groups |
| `success` | `#10B981` | `#059669` | Sovereignty passing, checkmarks, completed |
| `warning` | `#F59E0B` | `#D97706` | Test mode, cooldown warnings, alarms |
| `danger` / `sosCritical` | `#EF4444` | `#DC2626` | Live mode, countdown emergency trigger |

---

## 3. Typography Scale & Tabular Numerals (`ArOsTypography.kt`)

Typography scale strictly follows typographic hierarchy:
- `largeTitle` (32sp / 38sp lineHeight / -0.5sp letterSpacing / Bold)
- `title1` (24sp / 30sp lineHeight / -0.3sp letterSpacing / SemiBold)
- `title2` (20sp / 26sp lineHeight / -0.2sp letterSpacing / SemiBold)
- `headline` (17sp / 22sp lineHeight / -0.1sp letterSpacing / SemiBold)
- `body` (16sp / 22sp lineHeight / Regular)
- `callout` (15sp / 20sp lineHeight / Medium)
- `subhead` (14sp / 18sp lineHeight / Regular)
- `footnote` (13sp / 17sp lineHeight / Regular)
- `caption` (11sp / 14sp lineHeight / Regular)
- `numericDisplay` (36sp / 42sp lineHeight / Bold / `tnum`)
- `numericTitle` (22sp / 28sp lineHeight / SemiBold / `tnum`)
- `numericBody` (16sp / 22sp lineHeight / Medium / `tnum`)

### Font Installation Steps (Satoshi Variable)
AR/OS is strictly offline-first and connects to zero font CDNs or web services at runtime. To replace system fonts with Satoshi Variable:
1. Download `Satoshi-Variable.ttf` from Fontshare (free commercial license).
2. Rename the file to lowercase: `satoshi_variable.ttf`.
3. Place it into `/app/src/main/res/font/satoshi_variable.ttf`.
4. In `/app/src/main/java/com/aros/app/designsystem/theme/ArOsTypography.kt`, uncomment:
   ```kotlin
   val SatoshiFont = FontFamily(Font(R.font.satoshi_variable, FontWeight.Normal))
   val ArOsFontDisplay = SatoshiFont
   val ArOsFontText = SatoshiFont
   ```
5. Recompile with `compile_applet`.

---

## 4. Spacing & Shapes

- **Grid:** 4dp base grid (`xxs=4dp`, `xs=8dp`, `sm=12dp`, `md=16dp`, `lg=20dp`, `xl=24dp`, `xxl=32dp`).
- **Screen Margins:** 20dp horizontal screen margins on mobile.
- **Corner Radii:** `xs=8dp`, `sm=12dp`, `md=14dp`, `lg=20dp`, `xl=28dp`, `pill=CircleShape`.
- **Minimum Touch Target:** Strictly enforced **48dp x 48dp** for interactive components, with **64dp** for the safety-critical AR SOS Cancel button.

---

## 5. Motion & Haptics

- **Press Scale Indication:** `ArOsPressIndication` implements Compose 1.7 `IndicationNodeFactory`, gently scaling pressed components to `0.97f` on touch down with natural spring return.
- **Spring Curves:**
  - `springStandard`: damping `LowBouncy`, stiffness `MediumLow` (for sheets and drawer transitions).
  - `springSnappy`: damping `NoBouncy`, stiffness `Medium` (for segment switches and toggles).
- **Reduced Motion:** Observes system `Settings.Global.ANIMATOR_DURATION_SCALE` to respect accessibility animation elimination.
- **Tactile Feedback:** Hardware haptic engine patterns (`click`, `tick`, `heavy`, `success`, `sosPulse`).
