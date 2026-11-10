package com.aros.app.feature.sos.platform

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.aros.app.ArOsApplication
import com.aros.app.feature.sos.domain.ServiceState
import com.aros.app.feature.sos.domain.SosKeyAction
import com.aros.app.feature.sos.domain.SosKeyCode
import com.aros.app.feature.sos.domain.SosEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ArSosAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val volumeRestorer by lazy {
        val container = (application as? ArOsApplication)?.container
        VolumeRestorer(this, container?.settingsRepository)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        ServiceState.setConnected(true)

        // Ensure key event filtering flag is active (spec §4.1)
        serviceInfo = serviceInfo?.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }

        val container = (application as? ArOsApplication)?.container ?: return

        // Wire detector volume restorer hooks (spec §6)
        container.volumePressDetector.onSequenceStarted = {
            volumeRestorer.snapshotVolume()
        }
        container.volumePressDetector.onSequenceCleared = {
            volumeRestorer.clear()
        }

        // Apply saved preset immediately upon connection (spec §4.2)
        val savedPreset = container.settingsRepository.configSnapshot.value.sensitivityPreset
        container.volumePressDetector.setSensitivity(savedPreset)

        serviceScope.launch {
            container.eventLog.logEvent(
                type = SosEventType.SERVICE_CONNECTED,
                mode = container.settingsRepository.configSnapshot.value.mode,
                contactName = null,
                deviceLocked = container.deviceState.isDeviceLocked(),
                screenInteractive = container.deviceState.isScreenInteractive(),
                detail = "AR SOS Accessibility Service connected. Sensitivity preset: $savedPreset."
            )
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        // ALWAYS return false so system volume behavior and Emergency SOS are NEVER consumed (spec §4.2)
        if (event == null) return false

        try {
            val keyCode = when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> SosKeyCode.VOLUME_DOWN
                KeyEvent.KEYCODE_VOLUME_UP -> SosKeyCode.VOLUME_UP
                else -> SosKeyCode.OTHER
            }

            if (keyCode == SosKeyCode.OTHER) {
                return false
            }

            val action = when (event.action) {
                KeyEvent.ACTION_DOWN -> SosKeyAction.DOWN
                KeyEvent.ACTION_UP -> SosKeyAction.UP
                else -> return false
            }

            val container = (application as? ArOsApplication)?.container ?: return false

            // Use event.eventTime as time source per spec §4.2
            val eventTime = event.eventTime
            val isLongPress = event.isLongPress || (event.flags and KeyEvent.FLAG_LONG_PRESS != 0)

            val triggered = container.volumePressDetector.onKeyEvent(
                keyCode = keyCode,
                action = action,
                eventTime = eventTime,
                repeatCount = event.repeatCount,
                isLongPress = isLongPress
            )

            if (triggered) {
                volumeRestorer.restoreVolume()
                // Hand off to main handler with zero I/O on callback thread
                mainHandler.post {
                    container.sosController.onTrigger()
                }
            }
        } catch (_: Exception) {
            // Must never crash key event pipeline
        }

        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Zero screen reading required or implemented (canRetrieveWindowContent = false)
    }

    override fun onInterrupt() {
        handleServiceStopped(SosEventType.SERVICE_INTERRUPTED, "AR SOS Accessibility Service interrupted by system.")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        handleServiceStopped(SosEventType.SERVICE_UNBOUND, "AR SOS Accessibility Service unbound.")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        handleServiceStopped(SosEventType.SERVICE_UNBOUND, "AR SOS Accessibility Service destroyed.")
        super.onDestroy()
    }

    private fun handleServiceStopped(eventType: SosEventType, detail: String) {
        ServiceState.setConnected(false)
        val container = (application as? ArOsApplication)?.container ?: return

        container.volumePressDetector.onSequenceStarted = null
        container.volumePressDetector.onSequenceCleared = null

        container.notifier.showReadinessWarning(listOf("AR SOS monitoring stopped"))

        serviceScope.launch {
            try {
                container.eventLog.logEvent(
                    type = eventType,
                    mode = container.settingsRepository.configSnapshot.value.mode,
                    contactName = null,
                    deviceLocked = container.deviceState.isDeviceLocked(),
                    screenInteractive = container.deviceState.isScreenInteractive(),
                    detail = detail
                )
            } catch (_: Exception) {}
        }
    }
}
