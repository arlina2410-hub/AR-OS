package com.aros.app.feature.sos.platform

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.UserManager
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.aros.app.feature.sos.domain.ContactRepository
import com.aros.app.feature.sos.domain.OverallReadinessStatus
import com.aros.app.feature.sos.domain.ReadinessAction
import com.aros.app.feature.sos.domain.ReadinessCheckResult
import com.aros.app.feature.sos.domain.ReadinessChecker
import com.aros.app.feature.sos.domain.ReadinessReport
import com.aros.app.feature.sos.domain.ServiceState
import com.aros.app.feature.sos.domain.SettingsRepository
import com.aros.app.feature.sos.domain.SosMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ReadinessCheckerImpl(
    private val context: Context,
    private val contactRepository: ContactRepository,
    private val settingsRepository: SettingsRepository,
    private val simManager: SimManager
) : ReadinessChecker {

    override val readinessReportFlow: Flow<ReadinessReport> = flow {
        while (true) {
            emit(checkReadiness())
            kotlinx.coroutines.delay(10_000L) // Refresh every 10s
        }
    }

    override suspend fun checkReadiness(): ReadinessReport {
        val config = settingsRepository.configSnapshot.value
        val primaryContact = contactRepository.getPrimaryContact()

        val checks = mutableListOf<ReadinessCheckResult>()

        // 1. Accessibility Service check (Must be enabled in settings AND actively connected)
        val isA11ySystemEnabled = isAccessibilityServiceEnabled()
        val isA11yBound = ServiceState.connected.value
        val isA11yFullyActive = isA11ySystemEnabled && isA11yBound
        checks.add(
            ReadinessCheckResult(
                id = "accessibility_service",
                title = "Volume button trigger",
                description = when {
                    isA11yFullyActive -> "Volume button trigger is active and listening"
                    isA11ySystemEnabled -> "Service enabled in settings but interrupted or not yet bound"
                    else -> "Required to detect Volume Down 5x press"
                },
                isPassing = isA11yFullyActive,
                action = if (!isA11yFullyActive) ReadinessAction.ENABLE_ACCESSIBILITY else null
            )
        )

        // 2. CALL_PHONE Permission
        val hasCallPhone = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        checks.add(
            ReadinessCheckResult(
                id = "call_phone_permission",
                title = "Call permission",
                description = if (hasCallPhone) "Phone call permission granted" else "Required to place emergency call",
                isPassing = hasCallPhone,
                action = if (!hasCallPhone) ReadinessAction.GRANT_CALL_PHONE else null
            )
        )

        // 3. READ_PHONE_STATE Permission & SIM Selection
        val hasPhoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        val resolvedSim = simManager.getDefaultCallingSim()
        val hasSimResolved = resolvedSim != null || !simManager.getAvailableSims().isEmpty()
        val simPassing = hasPhoneState && hasSimResolved
        checks.add(
            ReadinessCheckResult(
                id = "sim_card_check",
                title = "Calling SIM card",
                description = when {
                    !hasPhoneState -> "Phone status permission needed to read SIMs"
                    resolvedSim != null -> "Using ${resolvedSim.label} for SOS calls"
                    else -> "No active SIM card detected"
                },
                isPassing = simPassing,
                action = if (!hasPhoneState) ReadinessAction.GRANT_CALL_PHONE else null
            )
        )

        // 4. POST_NOTIFICATIONS Permission
        val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        checks.add(
            ReadinessCheckResult(
                id = "notifications_permission",
                title = "Status & countdown alerts",
                description = if (hasNotifications) "Notification permission granted" else "Required for countdown banner",
                isPassing = hasNotifications,
                action = if (!hasNotifications) ReadinessAction.GRANT_NOTIFICATIONS else null
            )
        )

        // 5. Primary Contact
        val hasPrimaryContact = primaryContact != null
        checks.add(
            ReadinessCheckResult(
                id = "primary_contact",
                title = "Primary contact",
                description = if (hasPrimaryContact) "${primaryContact?.name}" else "No primary contact chosen",
                isPassing = hasPrimaryContact,
                action = if (!hasPrimaryContact) ReadinessAction.ADD_CONTACT else null
            )
        )

        // 6. Battery Optimization
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isBatteryIgnoring = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            true
        }
        checks.add(
            ReadinessCheckResult(
                id = "battery_optimization",
                title = "Background protection",
                description = if (isBatteryIgnoring) "Battery optimization ignored" else "May be paused by system battery saver",
                isPassing = isBatteryIgnoring,
                action = if (!isBatteryIgnoring) ReadinessAction.DISABLE_BATTERY_OPTIMIZATION else null
            )
        )

        // 7. Conflicting Key Filters
        val hasConflictingA11y = checkConflictingKeyFilters()
        checks.add(
            ReadinessCheckResult(
                id = "conflicting_services",
                title = "Volume button conflicts",
                description = if (!hasConflictingA11y) "No conflicting accessibility filters" else "Other services filtering keys may delay trigger",
                isPassing = !hasConflictingA11y,
                action = if (hasConflictingA11y) ReadinessAction.RESOLVE_CONFLICTS else null
            )
        )

        // 8. Auto-derived Unlocked Since Restart check (UserManager.isUserUnlocked)
        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
        val isUnlockedSinceRestart = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            userManager?.isUserUnlocked ?: true
        } else {
            true
        }
        checks.add(
            ReadinessCheckResult(
                id = "unlocked_since_restart",
                title = "Unlocked since restart",
                description = if (isUnlockedSinceRestart) "Device has been unlocked once since boot" else "Must unlock phone once after restart for service to run",
                isPassing = isUnlockedSinceRestart,
                action = null
            )
        )

        // 9. Operational Mode
        val isLiveMode = config.mode == SosMode.LIVE
        checks.add(
            ReadinessCheckResult(
                id = "operational_mode",
                title = "Operating Mode",
                description = if (isLiveMode) "LIVE Mode active (Real calls placed)" else "TEST Mode active (Safe simulation only)",
                isPassing = true,
                action = if (!isLiveMode) ReadinessAction.SWITCH_TO_LIVE else null
            )
        )

        val criticalPass = isA11yFullyActive && hasCallPhone && hasPrimaryContact
        val overallStatus = when {
            !criticalPass -> OverallReadinessStatus.NEEDS_ATTENTION
            config.mode == SosMode.TEST -> OverallReadinessStatus.TEST_MODE
            else -> OverallReadinessStatus.READY
        }

        return ReadinessReport(
            overallStatus = overallStatus,
            mode = config.mode,
            checks = checks
        )
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val expectedServiceName = "${context.packageName}/${ArSosAccessibilityService::class.java.name}"
        return enabledServices.any { it.id.equals(expectedServiceName, ignoreCase = true) }
    }

    private fun checkConflictingKeyFilters(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val ourServiceName = "${context.packageName}/${ArSosAccessibilityService::class.java.name}"

        return enabledServices.any { service ->
            !service.id.equals(ourServiceName, ignoreCase = true) &&
                    (service.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS != 0)
        }
    }
}
