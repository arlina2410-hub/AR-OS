package com.aros.app.feature.guard.domain

enum class GuardStatus {
    NOT_SET_UP,
    ARMED,
    SERVICE_OFF
}

enum class RelockDelay(val seconds: Long, val label: String) {
    IMMEDIATELY(0L, "Immediately"),
    SECONDS_30(30L, "30 seconds"),
    MINUTES_2(120L, "2 minutes");

    companion object {
        fun fromSeconds(seconds: Long): RelockDelay {
            return entries.find { it.seconds == seconds } ?: IMMEDIATELY
        }
    }
}

enum class LostGuardAction {
    NONE,
    DISMISS_NOTIFICATION_SHADE,
    CLOSE_POWER_MENU,
    GO_HOME
}

data class ObservedWindow(
    val packageName: String,
    val className: String,
    val timestampMillis: Long,
    val formattedTime12h: String
)

data class BlockedTarget(
    val packageName: String,
    val className: String = "",
    val label: String = ""
)

data class TamperLogEntry(
    val id: Long = 0,
    val timestampMillis: Long,
    val formattedTime12h: String,
    val eventType: String,
    val detail: String
)

data class FailedAttemptRecord(
    val timestampMillis: Long,
    val formattedTime12h: String,
    val totalFailedCount: Int
)

data class GuardConfig(
    val isConfigured: Boolean = false,
    val pinHashBase64: String = "",
    val saltBase64: String = "",
    val recoveryHashBase64: String = "",
    val relockDelaySeconds: Long = 0L,
    val goToHomeOnLock: Boolean = true,
    val consecutiveFailedAttempts: Int = 0,
    val lockoutUntilEpochMillis: Long = 0L,
    val failedAttemptHistory: List<FailedAttemptRecord> = emptyList(),
    val customBlockedTargets: List<BlockedTarget> = emptyList(),
    val allowedCallSosPackages: List<String> = DEFAULT_ALLOWED_CALL_SOS_PACKAGES,
    val learnModeExpiresAtEpochMillis: Long = 0L,
    val tamperLogs: List<TamperLogEntry> = emptyList()
) {
    companion object {
        val DEFAULT_ALLOWED_CALL_SOS_PACKAGES = listOf(
            "com.aistudio.aros.kpxwzm",
            "com.android.incallui",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.server.telecom"
        )
    }
}

sealed class VerifyResult {
    object Success : VerifyResult()
    data class WrongPin(
        val remainingFreeAttempts: Int,
        val lockoutSeconds: Long
    ) : VerifyResult()
    data class LockedOut(
        val remainingLockoutSeconds: Long
    ) : VerifyResult()
}

sealed class RecoveryResult {
    object Success : RecoveryResult()
    data class WrongCode(
        val remainingFreeAttempts: Int,
        val lockoutSeconds: Long
    ) : RecoveryResult()
    data class LockedOut(
        val remainingLockoutSeconds: Long
    ) : RecoveryResult()
}
