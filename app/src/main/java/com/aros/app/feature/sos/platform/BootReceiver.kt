package com.aros.app.feature.sos.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aros.app.ArOsApplication
import com.aros.app.feature.sos.domain.SosEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && context != null) {
            val pendingResult = goAsync()
            val app = context.applicationContext as? ArOsApplication
            if (app == null) {
                pendingResult.finish()
                return
            }

            val container = app.container
            ReadinessWorker.schedule(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val config = container.settingsRepository.configSnapshot.value
                    val report = container.readinessChecker.checkReadiness()

                    container.eventLog.logEvent(
                        type = SosEventType.BOOT_COMPLETED,
                        mode = config.mode,
                        contactName = null,
                        deviceLocked = container.deviceState.isDeviceLocked(),
                        screenInteractive = container.deviceState.isScreenInteractive(),
                        detail = "Device boot completed. Readiness audit scheduled."
                    )

                    if (config.showStatusNotification) {
                        container.notifier.showStatusNotification(config.mode, report.overallStatus)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
