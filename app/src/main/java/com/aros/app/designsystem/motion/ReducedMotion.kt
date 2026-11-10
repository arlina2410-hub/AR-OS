package com.aros.app.designsystem.motion

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext

/**
 * Observes system animator duration scale to respect user's "Remove animations" accessibility setting.
 */
@Composable
fun rememberReducedMotion(): State<Boolean> {
    val context = LocalContext.current
    return produceState(initialValue = false, context) {
        val durationScale = try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
        } catch (_: Exception) {
            1.0f
        }
        value = durationScale == 0f
    }
}
