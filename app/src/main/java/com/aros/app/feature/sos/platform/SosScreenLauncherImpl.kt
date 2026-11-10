package com.aros.app.feature.sos.platform

import android.content.Context
import android.content.Intent
import com.aros.app.MainActivity
import com.aros.app.feature.sos.domain.SosScreenLauncher

class SosScreenLauncherImpl(private val context: Context) : SosScreenLauncher {

    companion object {
        const val EXTRA_SHOW_SOS = "extra_show_sos"
    }

    override fun launchSosCountdown() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SHOW_SOS, true)
        }
        context.startActivity(intent)
    }

    override fun bringAppToFront() {
        launchSosCountdown()
    }
}
