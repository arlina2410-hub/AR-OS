package com.aros.app.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ArOsColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceOverlay: Color,
    val contentPrimary: Color,
    val contentSecondary: Color,
    val contentTertiary: Color,
    val contentOnAccent: Color,
    val accent: Color,
    val accentPressed: Color,
    val separator: Color,
    val hairline: Color,
    val scrim: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val glassFill: Color,
    val glassBorder: Color,
    val focusRing: Color,
    val sosCritical: Color
)

val DarkArOsColors = ArOsColors(
    background = Color(0xFF090D16),        // Deep Obsidian Navy base
    surface = Color(0xFF0F172A),           // Card container surface
    surfaceRaised = Color(0xFF1E293B),     // Inset group row surface
    surfaceOverlay = Color(0xFF26334D),    // Floating sheets and dialogs
    contentPrimary = Color(0xFFF8FAFC),    // 100% white-slate text
    contentSecondary = Color(0xFF94A3B8),  // Subordinate text (contrast > 4.5:1)
    contentTertiary = Color(0xFF64748B),   // Footnote & metadata
    contentOnAccent = Color(0xFF090D16),   // High-contrast text on cyan/neon buttons
    accent = Color(0xFF38BDF8),            // Crisp Sky Neon
    accentPressed = Color(0xFF0284C7),     // Deepened accent for pressed states
    separator = Color(0xFF1E293B),         // Inset-group divider lines
    hairline = Color(0xFF334155),          // 1dp / hairline surface border
    scrim = Color(0xCC000000),             // 80% black backdrop
    success = Color(0xFF10B981),           // Sovereign Green
    warning = Color(0xFFF59E0B),           // Warning Amber
    danger = Color(0xFFEF4444),            // Urgent Red
    info = Color(0xFF38BDF8),              // Info Cyan
    glassFill = Color(0xD90F172A),         // Translucent frosted glass fill (85%)
    glassBorder = Color(0x4D38BDF8),       // Glass ambient border (30% cyan)
    focusRing = Color(0xFF38BDF8),         // Accessibility focus indicator
    sosCritical = Color(0xFFEF4444)        // Dedicated AR SOS countdown trigger color
)

val LightArOsColors = ArOsColors(
    background = Color(0xFFF8FAFC),        // Pure clean slate background
    surface = Color(0xFFFFFFFF),           // Pure white container
    surfaceRaised = Color(0xFFF1F5F9),     // Raised inset rows
    surfaceOverlay = Color(0xFFFFFFFF),    // Floating dialogs & sheets
    contentPrimary = Color(0xFF0F172A),    // Dark charcoal text
    contentSecondary = Color(0xFF475569),  // Secondary text (contrast > 4.5:1)
    contentTertiary = Color(0xFF94A3B8),   // Footnote text
    contentOnAccent = Color(0xFFFFFFFF),   // White text on dark accent
    accent = Color(0xFF0284C7),            // Electric Sky Blue
    accentPressed = Color(0xFF0369A1),     // Darkened pressed accent
    separator = Color(0xFFE2E8F0),         // Clean border separator
    hairline = Color(0xFFCBD5E1),          // Hairline border
    scrim = Color(0x80000000),             // Semi-transparent scrim
    success = Color(0xFF059669),           // Emerald Green
    warning = Color(0xFFD97706),           // Amber Alert
    danger = Color(0xFFDC2626),            // High-contrast Red
    info = Color(0xFF0284C7),              // Info Blue
    glassFill = Color(0xE6FFFFFF),         // Translucent light glass fill (90%)
    glassBorder = Color(0x330284C7),       // Light glass border
    focusRing = Color(0xFF0284C7),         // Focus indicator
    sosCritical = Color(0xFFDC2626)        // Critical SOS color
)

val LocalArOsColors = staticCompositionLocalOf { DarkArOsColors }
