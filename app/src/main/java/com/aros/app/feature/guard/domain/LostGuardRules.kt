package com.aros.app.feature.guard.domain

/**
 * Pure Kotlin decision engine for Lost Guard.
 * Evaluates whether an observed window state change should be intercepted and closed
 * while AR Guard is locked.
 *
 * Guaranteed zero android.* imports for local JVM testing.
 */
class LostGuardRules(
    private val throttleMs: Long = 300L
) {
    private var lastActionElapsedRealtime: Long = 0L

    fun resetThrottle() {
        lastActionElapsedRealtime = 0L
    }

    /**
     * Evaluates window state change.
     *
     * @param packageName Package name of the foreground window
     * @param className Class name of the foreground window
     * @param isGuardLocked True if AR Guard is currently locked
     * @param isSosActive True if SOS countdown or call is in progress
     * @param isCallActive True if regular phone call is in progress
     * @param customBlockedTargets User-defined custom targets from Guard Lab
     * @param nowElapsed Current elapsed realtime in milliseconds
     */
    fun evaluateWindow(
        packageName: String?,
        className: String?,
        isGuardLocked: Boolean,
        isSosActive: Boolean,
        isCallActive: Boolean,
        customBlockedTargets: List<BlockedTarget>,
        nowElapsed: Long
    ): LostGuardAction {
        // Lost Guard rules run only while AR Guard is LOCKED
        if (!isGuardLocked) {
            return LostGuardAction.NONE
        }

        // Never intercept or interfere with active SOS or phone calls
        if (isSosActive || isCallActive) {
            return LostGuardAction.NONE
        }

        val pkg = packageName ?: ""
        val cls = className ?: ""

        // Throttle check: at most 1 action per 300ms per window
        if (nowElapsed - lastActionElapsedRealtime < throttleMs) {
            return LostGuardAction.NONE
        }

        // 1. Power Menu: typically SystemUI GlobalActions / PowerMenu
        if (isPowerMenu(pkg, cls)) {
            lastActionElapsedRealtime = nowElapsed
            return LostGuardAction.CLOSE_POWER_MENU
        }

        // 2. Notification Shade & Quick Settings: SystemUI Notification / QuickSettings / Shade
        if (isNotificationShade(pkg, cls)) {
            lastActionElapsedRealtime = nowElapsed
            return LostGuardAction.DISMISS_NOTIFICATION_SHADE
        }

        // 3. Settings / Package Installer / Uninstaller
        if (isSettingsOrInstaller(pkg, cls)) {
            lastActionElapsedRealtime = nowElapsed
            return LostGuardAction.GO_HOME
        }

        // 4. Custom user-defined blocked targets (from Guard Lab)
        for (target in customBlockedTargets) {
            if (target.packageName.isNotBlank() && pkg == target.packageName) {
                if (target.className.isBlank() || cls == target.className || cls.contains(target.className, ignoreCase = true)) {
                    lastActionElapsedRealtime = nowElapsed
                    return LostGuardAction.GO_HOME
                }
            }
        }

        return LostGuardAction.NONE
    }

    private fun isPowerMenu(pkg: String, cls: String): Boolean {
        if (pkg == "com.android.systemui") {
            val lower = cls.lowercase()
            if (lower.contains("globalactions") ||
                lower.contains("powermenu") ||
                lower.contains("powerdialog") ||
                lower.contains("shutdown")
            ) {
                return true
            }
        }
        return false
    }

    private fun isNotificationShade(pkg: String, cls: String): Boolean {
        if (pkg == "com.android.systemui") {
            val lower = cls.lowercase()
            // Notification shade, quick settings, or panel view
            if (lower.contains("notificationpanel") ||
                lower.contains("quicksettings") ||
                lower.contains("shade") ||
                lower.contains("notificationshade") ||
                lower.contains("expandedshade")
            ) {
                return true
            }
        }
        return false
    }

    private fun isSettingsOrInstaller(pkg: String, cls: String): Boolean {
        if (pkg == "com.android.settings" ||
            pkg == "com.google.android.packageinstaller" ||
            pkg == "com.android.packageinstaller"
        ) {
            return true
        }
        return false
    }
}
