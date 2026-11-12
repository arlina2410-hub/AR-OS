package com.aros.app.feature.guard.domain

/**
 * Pure Kotlin decision engine for Lost Guard.
 * Evaluates whether an observed window state change should be intercepted and closed
 * while AR Guard is locked, including closing the call/SOS hole (Prompt 3B).
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
     * Determines whether the gate overlay should be shown or hidden.
     * While a call or SOS is active, the gate steps aside ONLY while the foreground package is allowed.
     * The moment any other window comes to the front, the gate comes back.
     *
     * @return true if the gate overlay should be visible, false if it should be hidden
     */
    fun shouldShowGate(
        isGuardConfigured: Boolean,
        isSessionUnlocked: Boolean,
        isKeyguardLocked: Boolean,
        isSosActive: Boolean,
        isCallActive: Boolean,
        foregroundPackage: String?,
        allowedCallSosPackages: List<String>
    ): Boolean {
        if (!isGuardConfigured || isSessionUnlocked || isKeyguardLocked) {
            return false
        }

        // If a call or SOS is active:
        if (isSosActive || isCallActive) {
            val pkg = foregroundPackage ?: ""
            // Gate steps aside ONLY if foreground package is in allowed list
            if (isPackageAllowedDuringCallSos(pkg, allowedCallSosPackages)) {
                return false
            }
            // Otherwise, any other window brings the gate back
            return true
        }

        // Normal locked state without active call/SOS: gate must show
        return true
    }

    /**
     * Checks if a package is allowed during an active call or SOS.
     */
    fun isPackageAllowedDuringCallSos(
        packageName: String?,
        allowedCallSosPackages: List<String>
    ): Boolean {
        if (packageName.isNullOrBlank()) return false
        return allowedCallSosPackages.any { it.equals(packageName, ignoreCase = true) }
    }

    /**
     * Evaluates window state change.
     *
     * @param packageName Package name of the foreground window
     * @param className Class name of the foreground window
     * @param isGuardLocked True if AR Guard is currently locked
     * @param isSosActive True if SOS countdown or call is in progress
     * @param isCallActive True if regular phone call is in progress
     * @param allowedCallSosPackages Packages allowed during call/SOS (dialer, in-call UI, AR/OS)
     * @param customBlockedTargets User-defined custom targets from Guard Lab
     * @param nowElapsed Current elapsed realtime in milliseconds
     */
    fun evaluateWindow(
        packageName: String?,
        className: String?,
        isGuardLocked: Boolean,
        isSosActive: Boolean,
        isCallActive: Boolean,
        allowedCallSosPackages: List<String> = GuardConfig.DEFAULT_ALLOWED_CALL_SOS_PACKAGES,
        customBlockedTargets: List<BlockedTarget> = emptyList(),
        nowElapsed: Long = 0L
    ): LostGuardAction {
        // Lost Guard rules run only while AR Guard is LOCKED
        if (!isGuardLocked) {
            return LostGuardAction.NONE
        }

        val pkg = packageName ?: ""
        val cls = className ?: ""

        // Prompt 3B: While a call or SOS is active, rules and gate step aside ONLY while foreground window is an allowed window
        if (isSosActive || isCallActive) {
            if (isPackageAllowedDuringCallSos(pkg, allowedCallSosPackages)) {
                return LostGuardAction.NONE
            }
            // Otherwise, an unallowed window came to the front during a call or SOS: rules APPLY
        }

        // Throttle check: at most 1 action per 300ms per window
        if (nowElapsed > 0L && (nowElapsed - lastActionElapsedRealtime < throttleMs)) {
            return LostGuardAction.NONE
        }

        // 1. Power Menu: typically SystemUI GlobalActions / PowerMenu
        if (isPowerMenu(pkg, cls)) {
            if (nowElapsed > 0L) lastActionElapsedRealtime = nowElapsed
            return LostGuardAction.CLOSE_POWER_MENU
        }

        // 2. Notification Shade & Quick Settings: SystemUI Notification / QuickSettings / Shade
        if (isNotificationShade(pkg, cls)) {
            if (nowElapsed > 0L) lastActionElapsedRealtime = nowElapsed
            return LostGuardAction.DISMISS_NOTIFICATION_SHADE
        }

        // 3. Settings / Package Installer / Uninstaller
        if (isSettingsOrInstaller(pkg, cls)) {
            if (nowElapsed > 0L) lastActionElapsedRealtime = nowElapsed
            return LostGuardAction.GO_HOME
        }

        // 4. Custom user-defined blocked targets (from Guard Lab)
        for (target in customBlockedTargets) {
            if (target.packageName.isNotBlank() && pkg == target.packageName) {
                if (target.className.isBlank() || cls == target.className || cls.contains(target.className, ignoreCase = true)) {
                    if (nowElapsed > 0L) lastActionElapsedRealtime = nowElapsed
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
