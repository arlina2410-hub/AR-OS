package com.aros.app.designsystem.haptics

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

class ArOsHaptics(
    private val composeHaptics: HapticFeedback,
    private val vibrator: Vibrator?
) {
    /** Quick tactile click for buttons and segments */
    fun click() {
        composeHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /** Subtle tick for scrolling, sliders, and stepper changes */
    fun tick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator?.hasVibrator() == true) {
            try {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } catch (_: Exception) {
                composeHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } else {
            composeHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    /** Heavy impact for primary activation and state commits */
    fun heavy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrator?.hasVibrator() == true) {
            try {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } catch (_: Exception) {
                composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } else {
            composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    /** Success pattern (double light tap) */
    fun success() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasVibrator() == true) {
            try {
                val timings = longArrayOf(0, 30, 60, 30)
                val amplitudes = intArrayOf(0, 160, 0, 220)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } catch (_: Exception) {
                composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } else {
            composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    /** Warning pattern */
    fun warning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasVibrator() == true) {
            try {
                val timings = longArrayOf(0, 50, 50, 50)
                val amplitudes = intArrayOf(0, 180, 0, 180)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } catch (_: Exception) {
                composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } else {
            composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    /** Urgent critical SOS alert buzz */
    fun sosPulse() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasVibrator() == true) {
            try {
                val timings = longArrayOf(0, 120, 80, 120)
                val amplitudes = intArrayOf(0, 255, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } catch (_: Exception) {
                composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } else {
            composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

@Composable
fun rememberArOsHaptics(): ArOsHaptics {
    val composeHaptics = LocalHapticFeedback.current
    val context = LocalContext.current
    return remember(context, composeHaptics) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        ArOsHaptics(composeHaptics, vibrator)
    }
}
