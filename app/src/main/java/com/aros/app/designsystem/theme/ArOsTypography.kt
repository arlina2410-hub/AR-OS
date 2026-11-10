package com.aros.app.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * AR/OS Typography definitions.
 *
 * Font Selection & Offline-First Policy:
 * AR/OS is strictly offline-first and bundles no network fonts.
 * By default, [ArOsFontDisplay] and [ArOsFontText] resolve to [FontFamily.Default]
 * (system modern sans-serif: Roboto on Android / Moto G57 Power).
 *
 * To enable Satoshi Variable local font:
 * 1. Download Satoshi-Variable.ttf from Fontshare (free commercial license).
 * 2. Place it in app/src/main/res/font/satoshi_variable.ttf.
 * 3. Uncomment the Font wiring below:
 *
 * val SatoshiFont = FontFamily(Font(R.font.satoshi_variable, FontWeight.Normal))
 * val ArOsFontDisplay = SatoshiFont
 * val ArOsFontText = SatoshiFont
 */

val ArOsFontDisplay: FontFamily = FontFamily.Default
val ArOsFontText: FontFamily = FontFamily.Default

@Immutable
data class ArOsTypography(
    val largeTitle: TextStyle = TextStyle(
        fontFamily = ArOsFontDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    val title1: TextStyle = TextStyle(
        fontFamily = ArOsFontDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    val title2: TextStyle = TextStyle(
        fontFamily = ArOsFontDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    val headline: TextStyle = TextStyle(
        fontFamily = ArOsFontText,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp
    ),
    val body: TextStyle = TextStyle(
        fontFamily = ArOsFontText,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    val callout: TextStyle = TextStyle(
        fontFamily = ArOsFontText,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    val subhead: TextStyle = TextStyle(
        fontFamily = ArOsFontText,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    val footnote: TextStyle = TextStyle(
        fontFamily = ArOsFontText,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.1.sp
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = ArOsFontText,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    ),
    // Monospaced tabular figures for all numeric counters, timers, and ledgers
    val numericDisplay: TextStyle = TextStyle(
        fontFamily = ArOsFontDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum"
    ),
    val numericTitle: TextStyle = TextStyle(
        fontFamily = ArOsFontDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
        fontFeatureSettings = "tnum"
    ),
    val numericBody: TextStyle = TextStyle(
        fontFamily = ArOsFontText,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontFeatureSettings = "tnum"
    )
)

val LocalArOsTypography = staticCompositionLocalOf { ArOsTypography() }
