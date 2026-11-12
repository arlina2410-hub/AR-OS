package com.aros.app

import com.aros.app.feature.guard.domain.BlockedTarget
import com.aros.app.feature.guard.domain.GuardConfig
import com.aros.app.feature.guard.domain.LostGuardAction
import com.aros.app.feature.guard.domain.LostGuardRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LostGuardRulesTest {

    private lateinit var rules: LostGuardRules

    @Before
    fun setUp() {
        rules = LostGuardRules(throttleMs = 300L)
    }

    @Test
    fun `when guard is not locked - returns NONE`() {
        val action = rules.evaluateWindow(
            packageName = "com.android.systemui",
            className = "com.android.systemui.globalactions.GlobalActionsDialog",
            isGuardLocked = false,
            isSosActive = false,
            isCallActive = false,
            customBlockedTargets = emptyList(),
            nowElapsed = 1000L
        )
        assertEquals(LostGuardAction.NONE, action)
    }

    @Test
    fun `power menu window while locked returns CLOSE_POWER_MENU`() {
        val action = rules.evaluateWindow(
            packageName = "com.android.systemui",
            className = "com.android.systemui.globalactions.GlobalActionsDialogLite",
            isGuardLocked = true,
            isSosActive = false,
            isCallActive = false,
            customBlockedTargets = emptyList(),
            nowElapsed = 1000L
        )
        assertEquals(LostGuardAction.CLOSE_POWER_MENU, action)
    }

    @Test
    fun `notification shade or quick settings window while locked returns DISMISS_NOTIFICATION_SHADE`() {
        val action = rules.evaluateWindow(
            packageName = "com.android.systemui",
            className = "com.android.systemui.shade.NotificationShadeWindowView",
            isGuardLocked = true,
            isSosActive = false,
            isCallActive = false,
            customBlockedTargets = emptyList(),
            nowElapsed = 1000L
        )
        assertEquals(LostGuardAction.DISMISS_NOTIFICATION_SHADE, action)
    }

    @Test
    fun `settings app while locked returns GO_HOME`() {
        val action = rules.evaluateWindow(
            packageName = "com.android.settings",
            className = "com.android.settings.SubSettings",
            isGuardLocked = true,
            isSosActive = false,
            isCallActive = false,
            customBlockedTargets = emptyList(),
            nowElapsed = 1000L
        )
        assertEquals(LostGuardAction.GO_HOME, action)
    }

    @Test
    fun `package installer while locked returns GO_HOME`() {
        val action = rules.evaluateWindow(
            packageName = "com.google.android.packageinstaller",
            className = "com.android.packageinstaller.UninstallerActivity",
            isGuardLocked = true,
            isSosActive = false,
            isCallActive = false,
            customBlockedTargets = emptyList(),
            nowElapsed = 1000L
        )
        assertEquals(LostGuardAction.GO_HOME, action)
    }

    @Test
    fun `throttling suppresses rapid actions within 300ms`() {
        val firstAction = rules.evaluateWindow(
            packageName = "com.android.settings",
            className = "com.android.settings.Settings",
            isGuardLocked = true,
            isSosActive = false,
            isCallActive = false,
            customBlockedTargets = emptyList(),
            nowElapsed = 1000L
        )
        assertEquals(LostGuardAction.GO_HOME, firstAction)

        // Rapid subsequent call at 1100ms (+100ms, less than 300ms)
        val throttledAction = rules.evaluateWindow(
            packageName = "com.android.settings",
            className = "com.android.settings.Settings",
            isGuardLocked = true,
            isSosActive = false,
            isCallActive = false,
            customBlockedTargets = emptyList(),
            nowElapsed = 1100L
        )
        assertEquals(LostGuardAction.NONE, throttledAction)

        // After 350ms elapsed (+350ms from first action)
        val afterThrottleAction = rules.evaluateWindow(
            packageName = "com.android.settings",
            className = "com.android.settings.Settings",
            isGuardLocked = true,
            isSosActive = false,
            isCallActive = false,
            customBlockedTargets = emptyList(),
            nowElapsed = 1350L
        )
        assertEquals(LostGuardAction.GO_HOME, afterThrottleAction)
    }

    @Test
    fun `custom blocked target triggers GO_HOME`() {
        val customTargets = listOf(
            BlockedTarget(
                packageName = "com.motorola.ccc.notification",
                className = "",
                label = "Motorola notifications"
            )
        )

        val action = rules.evaluateWindow(
            packageName = "com.motorola.ccc.notification",
            className = "com.motorola.ccc.notification.AlertActivity",
            isGuardLocked = true,
            isSosActive = false,
            isCallActive = false,
            customBlockedTargets = customTargets,
            nowElapsed = 2000L
        )
        assertEquals(LostGuardAction.GO_HOME, action)
    }

    // --- Prompt 3B: Call / SOS Hole Protection Tests ---

    @Test
    fun `allowed package during call keeps gate hidden and rules suspended`() {
        val allowedPackages = GuardConfig.DEFAULT_ALLOWED_CALL_SOS_PACKAGES // contains com.android.incallui

        // 1. shouldShowGate returns false for allowed in-call screen
        val showGate = rules.shouldShowGate(
            isGuardConfigured = true,
            isSessionUnlocked = false,
            isKeyguardLocked = false,
            isSosActive = false,
            isCallActive = true,
            foregroundPackage = "com.android.incallui",
            allowedCallSosPackages = allowedPackages
        )
        assertFalse(showGate)

        // 2. evaluateWindow returns NONE for allowed in-call screen
        val action = rules.evaluateWindow(
            packageName = "com.android.incallui",
            className = "com.android.incallui.InCallActivity",
            isGuardLocked = true,
            isSosActive = false,
            isCallActive = true,
            allowedCallSosPackages = allowedPackages,
            nowElapsed = 1000L
        )
        assertEquals(LostGuardAction.NONE, action)
    }

    @Test
    fun `unknown package during call shows gate and applies Lost Guard rules`() {
        val allowedPackages = GuardConfig.DEFAULT_ALLOWED_CALL_SOS_PACKAGES

        // 1. shouldShowGate returns true if user/thief switches to Settings or launcher during a call
        val showGate = rules.shouldShowGate(
            isGuardConfigured = true,
            isSessionUnlocked = false,
            isKeyguardLocked = false,
            isSosActive = false,
            isCallActive = true,
            foregroundPackage = "com.android.settings",
            allowedCallSosPackages = allowedPackages
        )
        assertTrue(showGate)

        // 2. evaluateWindow intercepts Settings during a call and redirects to GO_HOME
        val actionSettings = rules.evaluateWindow(
            packageName = "com.android.settings",
            className = "com.android.settings.Settings",
            isGuardLocked = true,
            isSosActive = false,
            isCallActive = true,
            allowedCallSosPackages = allowedPackages,
            nowElapsed = 1000L
        )
        assertEquals(LostGuardAction.GO_HOME, actionSettings)

        // 3. Quick settings shade pulled down during a call is closed
        val actionShade = rules.evaluateWindow(
            packageName = "com.android.systemui",
            className = "com.android.systemui.shade.NotificationShadeWindowView",
            isGuardLocked = true,
            isSosActive = false,
            isCallActive = true,
            allowedCallSosPackages = allowedPackages,
            nowElapsed = 2000L
        )
        assertEquals(LostGuardAction.DISMISS_NOTIFICATION_SHADE, actionShade)
    }

    @Test
    fun `user-added allowed package keeps gate hidden during call`() {
        // e.g., Moto G57 Power proprietary dialer
        val customAllowed = GuardConfig.DEFAULT_ALLOWED_CALL_SOS_PACKAGES + "com.motorola.phone"

        val showGate = rules.shouldShowGate(
            isGuardConfigured = true,
            isSessionUnlocked = false,
            isKeyguardLocked = false,
            isSosActive = false,
            isCallActive = true,
            foregroundPackage = "com.motorola.phone",
            allowedCallSosPackages = customAllowed
        )
        assertFalse(showGate)

        val action = rules.evaluateWindow(
            packageName = "com.motorola.phone",
            className = "com.motorola.phone.CallScreen",
            isGuardLocked = true,
            isSosActive = false,
            isCallActive = true,
            allowedCallSosPackages = customAllowed,
            nowElapsed = 1000L
        )
        assertEquals(LostGuardAction.NONE, action)
    }

    @Test
    fun `when call ends - gate returns if phone is unlocked and session is locked`() {
        val allowedPackages = GuardConfig.DEFAULT_ALLOWED_CALL_SOS_PACKAGES

        // During call on allowed screen: false
        assertFalse(
            rules.shouldShowGate(
                isGuardConfigured = true,
                isSessionUnlocked = false,
                isKeyguardLocked = false,
                isSosActive = false,
                isCallActive = true,
                foregroundPackage = "com.android.incallui",
                allowedCallSosPackages = allowedPackages
            )
        )

        // Call ends (isCallActive = false): gate must show!
        assertTrue(
            rules.shouldShowGate(
                isGuardConfigured = true,
                isSessionUnlocked = false,
                isKeyguardLocked = false,
                isSosActive = false,
                isCallActive = false,
                foregroundPackage = "com.android.incallui",
                allowedCallSosPackages = allowedPackages
            )
        )
    }

    @Test
    fun `allowed AR OS screen during SOS countdown stays visible but switching away brings gate back`() {
        val allowedPackages = GuardConfig.DEFAULT_ALLOWED_CALL_SOS_PACKAGES // contains com.aistudio.aros.kpxwzm

        // AR/OS SOS screen is allowed
        assertFalse(
            rules.shouldShowGate(
                isGuardConfigured = true,
                isSessionUnlocked = false,
                isKeyguardLocked = false,
                isSosActive = true,
                isCallActive = false,
                foregroundPackage = "com.aistudio.aros.kpxwzm",
                allowedCallSosPackages = allowedPackages
            )
        )

        // If user navigates away to YouTube or Browser during SOS, gate must show
        assertTrue(
            rules.shouldShowGate(
                isGuardConfigured = true,
                isSessionUnlocked = false,
                isKeyguardLocked = false,
                isSosActive = true,
                isCallActive = false,
                foregroundPackage = "com.google.android.youtube",
                allowedCallSosPackages = allowedPackages
            )
        )
    }
}
