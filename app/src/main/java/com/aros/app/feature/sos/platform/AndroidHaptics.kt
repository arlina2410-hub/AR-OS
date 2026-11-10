package com.aros.app.feature.sos.platform

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.aros.app.feature.sos.domain.Haptics

/**
 * Haptics engine using ALARM usage attributes per AR SOS Spec §9.1
 * so user touch-feedback disablement does not silence emergency haptic feedback.
 */
class AndroidHaptics(private val context: Context) : Haptics {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun triggerDetected() {
        // Spec §9.1: Detected = one 400 ms pulse
        vibratePattern(
            timings = longArrayOf(0, 400),
            amplitudes = intArrayOf(0, 255)
        )
    }

    override fun countdownTick(remainingSeconds: Int) {
        // Subtle tick during active countdown
        vibratePattern(
            timings = longArrayOf(0, 30),
            amplitudes = intArrayOf(0, 150)
        )
    }

    override fun countdownCancelled() {
        // Spec §9.1: Cancelled = three 40 ms pulses
        vibratePattern(
            timings = longArrayOf(0, 40, 40, 40, 40, 40),
            amplitudes = intArrayOf(0, 200, 0, 200, 0, 200)
        )
    }

    override fun callInitiating() {
        // Spec §9.1: Dialing = two 80 ms pulses with 80 ms gap
        vibratePattern(
            timings = longArrayOf(0, 80, 80, 80),
            amplitudes = intArrayOf(0, 255, 0, 255)
        )
    }

    private fun vibratePattern(timings: LongArray, amplitudes: IntArray) {
        try {
            val v = vibrator ?: return
            if (!v.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val attrs = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_ALARM)
                        .build()
                    v.vibrate(effect, attrs)
                } else {
                    @Suppress("DEPRECATION")
                    val audioAttrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    @Suppress("DEPRECATION")
                    v.vibrate(effect, audioAttrs)
                }
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(timings, -1)
            }
        } catch (_: Exception) {
            // Must never crash on device vibration limits or power saver
        }
    }
}
