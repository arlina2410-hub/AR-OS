package com.aros.app.feature.sos.domain

enum class SosMode {
    TEST,
    LIVE
}

enum class ContactRole {
    PRIMARY,
    BACKUP
}

data class SosContact(
    val id: Long = 0,
    val name: String,
    val number: String,
    val role: ContactRole = ContactRole.BACKUP,
    val sortOrder: Int = 0
)

enum class OverallReadinessStatus(val displayString: String) {
    READY("READY — not a guarantee"),
    TEST_MODE("TEST MODE — no calls will be placed"),
    NEEDS_ATTENTION("NEEDS ATTENTION");

    val displayLabel: String get() = displayString
}

enum class ReadinessAction {
    ENABLE_ACCESSIBILITY,
    GRANT_CALL_PHONE,
    GRANT_NOTIFICATIONS,
    ADD_CONTACT,
    DISABLE_BATTERY_OPTIMIZATION,
    OPEN_RESTRICTED_SETTINGS,
    RESOLVE_CONFLICTS,
    SWITCH_TO_LIVE,
    CONFIRM_DEFAULT_SIM,
    CONFIRM_FIRST_UNLOCK
}

data class ReadinessCheckResult(
    val id: String,
    val title: String,
    val description: String,
    val isPassing: Boolean,
    val action: ReadinessAction? = null
)

data class ReadinessReport(
    val overallStatus: OverallReadinessStatus,
    val mode: SosMode,
    val checks: List<ReadinessCheckResult>
)

sealed class SosState {
    object Idle : SosState()

    data class Countdown(
        val remainingSeconds: Int,
        val totalSeconds: Int,
        val targetContact: SosContact,
        val mode: SosMode
    ) : SosState()

    data class Dialing(
        val targetContact: SosContact,
        val mode: SosMode,
        val method: String
    ) : SosState()

    data class Finished(
        val targetContact: SosContact?,
        val mode: SosMode,
        val reason: String
    ) : SosState()
}

data class SosConfig(
    val mode: SosMode = SosMode.TEST,
    val countdownSeconds: Int = 5,
    val sensitivityPreset: String = "Normal", // Relaxed, Normal, Strict
    val restoreVolumeAfterSos: Boolean = true,
    val startCallOnSpeaker: Boolean = false,
    val showStatusNotification: Boolean = true,
    val selectedSimId: String? = null,
    val setupCompleted: Boolean = false,
    val use24HourTime: Boolean = false
)

enum class SosEventType {
    SERVICE_CONNECTED,
    SERVICE_INTERRUPTED,
    SERVICE_UNBOUND,
    TRIGGER_DETECTED,
    COUNTDOWN_STARTED,
    CANCELLED,
    CALL_NOW,
    DIAL_REQUESTED,
    DIAL_FAILED,
    TEST_NO_CALL,
    NO_CONTACT,
    WARN_AIRPLANE_MODE,
    READINESS_WARNING,
    BOOT_COMPLETED,
    SETTINGS_CHANGED;

    companion object {
        fun fromLegacyOrName(name: String): SosEventType {
            return when (name) {
                "TRIGGERED" -> TRIGGER_DETECTED
                "CALL_INITIATED" -> DIAL_REQUESTED
                "COMPLETED" -> DIAL_REQUESTED
                "REBOOT" -> BOOT_COMPLETED
                "CONFIG_CHANGED" -> SETTINGS_CHANGED
                "COUNTDOWN_TICK" -> COUNTDOWN_STARTED
                "CALL_FAILED" -> DIAL_FAILED
                else -> try {
                    valueOf(name)
                } catch (_: Exception) {
                    SETTINGS_CHANGED
                }
            }
        }
    }
}

data class SosEvent(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: SosEventType,
    val mode: SosMode,
    val contactName: String?,
    val deviceLocked: Boolean,
    val screenInteractive: Boolean,
    val detail: String
)
