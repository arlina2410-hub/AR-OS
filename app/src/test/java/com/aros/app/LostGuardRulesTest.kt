package com.aros.app

import com.aros.app.feature.guard.domain.BlockedTarget
import com.aros.app.feature.guard.domain.LostGuardAction
import com.aros.app.feature.guard.domain.LostGuardRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
    fun `when SOS is active - returns NONE even if locked`() {
        val action = rules.evaluateWindow(
            packageName = "com.android.systemui",
            className = "com.android.systemui.globalactions.GlobalActionsDialog",
            isGuardLocked = true,
            isSosActive = true,
            isCallActive = false,
            customBlockedTargets = emptyList(),
            nowElapsed = 1000L
        )
        assertEquals(LostGuardAction.NONE, action)
    }

    @Test
    fun `when regular phone call is active - returns NONE even if locked`() {
        val action = rules.evaluateWindow(
            packageName = "com.android.settings",
            className = "com.android.settings.Settings",
            isGuardLocked = true,
            isSosActive = false,
            isCallActive = true,
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
}
