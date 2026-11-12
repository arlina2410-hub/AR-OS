package com.aros.app.feature.guard.platform

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.view.accessibility.AccessibilityEvent
import com.aros.app.ArOsApplication
import com.aros.app.core.TimeFormatter
import com.aros.app.feature.guard.domain.GuardConfig
import com.aros.app.feature.guard.domain.GuardEngine
import com.aros.app.feature.guard.domain.GuardRepository
import com.aros.app.feature.guard.domain.LostGuardAction
import com.aros.app.feature.guard.domain.LostGuardRules
import com.aros.app.feature.guard.domain.ObservedWindow
import com.aros.app.feature.sos.domain.SosController
import com.aros.app.feature.sos.domain.SosState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ArGuardAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ArGuardAccessibilityService? = null
            private set

        fun triggerLockNow() {
            instance?.lockNowAndShow()
        }

        fun triggerTestLock() {
            instance?.showTestLock()
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sosStateJob: Job? = null
    private var configJob: Job? = null

    private lateinit var guardRepository: GuardRepository
    private lateinit var guardEngine: GuardEngine
    private lateinit var sosController: SosController
    private var overlayManager: GuardOverlayManager? = null

    private val lostGuardRules = LostGuardRules()

    private var screenOffElapsedRealtime: Long = 0L
    private var isCallActive = false
    private var isSosActive = false
    private var currentForegroundPackage: String? = null
    private var currentConfig = GuardConfig()

    // Telephony listener references for clean unregistration
    private var telephonyCallback: TelephonyCallback? = null
    @Suppress("DEPRECATION")
    private var phoneStateListener: PhoneStateListener? = null

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOffElapsedRealtime = SystemClock.elapsedRealtime()
                }
                Intent.ACTION_USER_PRESENT -> {
                    handleUserPresent()
                }
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                    val isAirplaneOn = Settings.Global.getInt(
                        contentResolver,
                        Settings.Global.AIRPLANE_MODE_ON,
                        0
                    ) != 0

                    val isGuardLocked = currentConfig.isConfigured && !guardRepository.isSessionUnlocked.value
                    if (isGuardLocked && isAirplaneOn) {
                        serviceScope.launch {
                            guardRepository.logTamperEvent(
                                eventType = "AIRPLANE_MODE_TOGGLED",
                                detail = "Airplane mode switched ON while AR Guard was locked"
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: ""
        val className = event.className?.toString() ?: ""
        currentForegroundPackage = packageName

        val nowMillis = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()

        // 1. If Learn Mode is active (within 10-minute auto-expiry window by wall clock), capture observed window
        if (nowMillis < currentConfig.learnModeExpiresAtEpochMillis) {
            val formattedTime = TimeFormatter.formatClock(nowMillis, includeSeconds = true)
            guardRepository.recordObservedWindow(
                ObservedWindow(
                    packageName = packageName,
                    className = className,
                    timestampMillis = nowMillis,
                    formattedTime12h = formattedTime
                )
            )
        }

        val isGuardLocked = currentConfig.isConfigured && !guardRepository.isSessionUnlocked.value

        // Prompt 3B: During call or SOS, evaluate whether gate should be visible
        if (isGuardLocked && (isSosActive || isCallActive)) {
            val isAllowed = lostGuardRules.isPackageAllowedDuringCallSos(
                packageName,
                currentConfig.allowedCallSosPackages
            )
            if (isAllowed) {
                // Allowed package in foreground (InCallUI, Dialer, AR/OS SOS): keep gate hidden
                overlayManager?.dismissOverlay()
            } else {
                // Unallowed package came to front during call/SOS: bring gate back immediately!
                maybeShowGate()
            }
        }

        // 2. Evaluate window against Lost Guard rules
        val action = lostGuardRules.evaluateWindow(
            packageName = packageName,
            className = className,
            isGuardLocked = isGuardLocked,
            isSosActive = isSosActive,
            isCallActive = isCallActive,
            allowedCallSosPackages = currentConfig.allowedCallSosPackages,
            customBlockedTargets = currentConfig.customBlockedTargets,
            nowElapsed = nowElapsed
        )

        executeLostGuardAction(action, packageName, className)
    }

    private fun executeLostGuardAction(action: LostGuardAction, pkg: String, cls: String) {
        when (action) {
            LostGuardAction.NONE -> Unit
            LostGuardAction.DISMISS_NOTIFICATION_SHADE -> {
                // Rule 1: GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE needs API 31+.
                // Fall back to GLOBAL_ACTION_BACK, then GLOBAL_ACTION_HOME, on older versions.
                var succeeded = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        succeeded = performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
                    } catch (_: Exception) {}
                }
                if (!succeeded) {
                    try {
                        succeeded = performGlobalAction(GLOBAL_ACTION_BACK)
                    } catch (_: Exception) {}
                }
                if (!succeeded) {
                    try {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                    } catch (_: Exception) {}
                }

                serviceScope.launch {
                    guardRepository.logTamperEvent(
                        eventType = "NOTIFICATION_SHADE_BLOCKED",
                        detail = "Closed notification shade / quick settings ($pkg)"
                    )
                }
            }
            LostGuardAction.CLOSE_POWER_MENU -> {
                // Power menu: BACK then HOME
                try {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                } catch (_: Exception) {}
                try {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                } catch (_: Exception) {}

                serviceScope.launch {
                    guardRepository.logTamperEvent(
                        eventType = "POWER_MENU_BLOCKED",
                        detail = "Closed power / shutdown menu ($pkg)"
                    )
                }
            }
            LostGuardAction.GO_HOME -> {
                // Settings or installer or custom blocked target: HOME
                try {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                } catch (_: Exception) {}

                serviceScope.launch {
                    guardRepository.logTamperEvent(
                        eventType = "TARGET_BLOCKED",
                        detail = "Redirected to home screen from blocked target: $pkg / $cls"
                    )
                }
            }
        }
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        val container = (applicationContext as ArOsApplication).container
        guardRepository = container.guardRepository
        guardEngine = container.guardEngine
        sosController = container.sosController

        overlayManager = GuardOverlayManager(this, guardRepository, serviceScope)
        guardRepository.setServiceConnected(true)

        // Log service re-enabled / connected to tamper log
        serviceScope.launch {
            guardRepository.logTamperEvent(
                eventType = "SERVICE_CONNECTED",
                detail = "AR Guard accessibility service connected"
            )
        }

        // Register dynamic broadcasts (screen states & airplane mode)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        }
        registerReceiver(systemReceiver, filter)

        // Telephony listener to monitor call state
        setupTelephonyListener()

        // SOS Controller listener (monitors countdown or active call)
        sosStateJob = serviceScope.launch {
            sosController.state.collect { sosState ->
                val active = sosState !is SosState.Idle
                isSosActive = active
                if (active) {
                    // During active SOS countdown/call: allow AR/OS screen or dialer screen
                    val isAllowed = lostGuardRules.isPackageAllowedDuringCallSos(
                        currentForegroundPackage,
                        currentConfig.allowedCallSosPackages
                    )
                    if (isAllowed) {
                        overlayManager?.dismissOverlay()
                    } else {
                        maybeShowGate()
                    }
                } else {
                    // SOS ended -> re-evaluate showing gate
                    maybeShowGate()
                }
            }
        }

        configJob = serviceScope.launch {
            guardRepository.configFlow.collect { config ->
                currentConfig = config
            }
        }

        // Set currentConfig from first() before locking session and showing gate
        serviceScope.launch {
            val config = guardRepository.configFlow.first()
            currentConfig = config
            if (config.isConfigured) {
                guardRepository.lockSession()
                maybeShowGate()
            }
        }
    }

    private fun handleUserPresent() {
        if (!currentConfig.isConfigured) return

        val nowElapsed = SystemClock.elapsedRealtime()
        val shouldRelock = guardEngine.shouldRelockAfterScreenOff(
            screenOffElapsedRealtime = screenOffElapsedRealtime,
            currentElapsedRealtime = nowElapsed,
            relockDelaySeconds = currentConfig.relockDelaySeconds
        )

        if (shouldRelock) {
            guardRepository.lockSession()
        }

        maybeShowGate()
    }

    fun lockNowAndShow() {
        if (!currentConfig.isConfigured) return
        guardRepository.lockSession()
        maybeShowGate()
    }

    fun showTestLock() {
        overlayManager?.showOverlay(isTestMode = true) {
            guardRepository.unlockSession()
        }
    }

    private fun maybeShowGate() {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isKeyguardLocked = keyguardManager?.isKeyguardLocked ?: false

        val shouldShow = lostGuardRules.shouldShowGate(
            isGuardConfigured = currentConfig.isConfigured,
            isSessionUnlocked = guardRepository.isSessionUnlocked.value,
            isKeyguardLocked = isKeyguardLocked,
            isSosActive = isSosActive,
            isCallActive = isCallActive,
            foregroundPackage = currentForegroundPackage,
            allowedCallSosPackages = currentConfig.allowedCallSosPackages
        )

        if (!shouldShow) {
            return
        }

        if (currentConfig.goToHomeOnLock) {
            try {
                performGlobalAction(GLOBAL_ACTION_HOME)
            } catch (_: Exception) {}
        }

        overlayManager?.showOverlay(isTestMode = false) {
            guardRepository.unlockSession()
        }
    }

    private fun setupTelephonyListener() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleCallState(state)
                }
            }
            telephonyCallback = callback
            try {
                telephonyManager.registerTelephonyCallback(mainExecutor, callback)
            } catch (_: SecurityException) {}
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleCallState(state)
                }
            }
            phoneStateListener = listener
            try {
                @Suppress("DEPRECATION")
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            } catch (_: SecurityException) {}
        }
    }

    private fun handleCallState(state: Int) {
        val inCall = state != TelephonyManager.CALL_STATE_IDLE
        isCallActive = inCall
        if (inCall) {
            // Prompt 3B: If current foreground is an allowed in-call screen, dismiss overlay
            val isAllowed = lostGuardRules.isPackageAllowedDuringCallSos(
                currentForegroundPackage,
                currentConfig.allowedCallSosPackages
            )
            if (isAllowed) {
                overlayManager?.dismissOverlay()
            } else {
                maybeShowGate()
            }
        } else {
            // Call ended: restore gate if locked
            maybeShowGate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null

        overlayManager?.dismissOverlay()
        overlayManager = null

        try {
            unregisterReceiver(systemReceiver)
        } catch (_: Exception) {}

        // Unregister telephony callback/listener cleanly
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        if (telephonyManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback?.let {
                    try {
                        telephonyManager.unregisterTelephonyCallback(it)
                    } catch (_: Exception) {}
                }
                telephonyCallback = null
            } else {
                phoneStateListener?.let {
                    try {
                        @Suppress("DEPRECATION")
                        telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
                    } catch (_: Exception) {}
                }
                phoneStateListener = null
            }
        }

        val appContainer = (applicationContext as? ArOsApplication)?.container

        // Log SERVICE_DISABLED using the applicationScope BEFORE serviceScope is cancelled
        if (::guardRepository.isInitialized && appContainer != null) {
            guardRepository.setServiceConnected(false)
            appContainer.applicationScope.launch(Dispatchers.IO) {
                guardRepository.logTamperEvent(
                    eventType = "SERVICE_DISABLED",
                    detail = "AR Guard accessibility service stopped or disabled"
                )
            }
            if (currentConfig.isConfigured) {
                try {
                    appContainer.notifier.showReadinessWarning(listOf("AR Guard is OFF. Device protection gate is not active."))
                } catch (_: Exception) {}
            }
        }

        serviceScope.cancel()
    }
}
