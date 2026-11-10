package com.aros.app.feature.sos.platform

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import android.telephony.TelephonyManager
import com.aros.app.feature.sos.domain.DeviceStateProvider

class DeviceStateProviderImpl(private val context: Context) : DeviceStateProvider {

    private val keyguardManager by lazy {
        context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    }

    private val powerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }

    private val telephonyManager by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    }

    override fun isDeviceLocked(): Boolean {
        return keyguardManager?.isKeyguardLocked ?: false
    }

    override fun isScreenInteractive(): Boolean {
        return powerManager?.isInteractive ?: false
    }

    override fun isAirplaneModeOn(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        } catch (_: Exception) {
            false
        }
    }

    override fun hasSimCard(): Boolean {
        val state = telephonyManager?.simState ?: TelephonyManager.SIM_STATE_UNKNOWN
        return state != TelephonyManager.SIM_STATE_ABSENT && state != TelephonyManager.SIM_STATE_UNKNOWN
    }
}
