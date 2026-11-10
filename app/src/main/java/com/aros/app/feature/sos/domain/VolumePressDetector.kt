package com.aros.app.feature.sos.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SosKeyCode {
    VOLUME_DOWN,
    VOLUME_UP,
    OTHER
}

enum class SosKeyAction {
    DOWN,
    UP
}

enum class SensitivityPreset(val label: String, val windowMs: Long) {
    Relaxed("Relaxed", 3500L),
    Normal("Normal", 2500L),
    Strict("Strict", 1800L);

    companion object {
        fun fromString(value: String): SensitivityPreset {
            return when (value.trim().uppercase()) {
                "RELAXED" -> Relaxed
                "STRICT", "AGGRESSIVE" -> Strict
                else -> Normal // "Normal", "STANDARD", etc.
            }
        }
    }
}

data class DetectorParams(
    val requiredPresses: Int = 5,
    val windowMs: Long = 2500L,
    val maxPressDurationMs: Long = 600L,
    val minGapMs: Long = 40L,
    val cooldownMs: Long = 8000L
)

data class TriggerTelemetry(
    val currentCount: Int = 0,
    val intervals: List<Long> = emptyList(),
    val lastRejectionReason: String? = null,
    val lastPressTimestamp: Long = 0L,
    val isCooldownActive: Boolean = false
)

/**
 * Pure Kotlin VolumePressDetector per AR SOS Spec §6.
 * Zero Android imports (KeyEvent is mapped to SosKeyCode / SosKeyAction by caller).
 */
class VolumePressDetector(
    params: DetectorParams = DetectorParams()
) {
    val requiredPresses: Int = params.requiredPresses
    var windowMs: Long = params.windowMs
        private set
    val maxPressDurationMs: Long = params.maxPressDurationMs
    val minGapMs: Long = params.minGapMs
    val cooldownMs: Long = params.cooldownMs

    private val _telemetry = MutableStateFlow(TriggerTelemetry())
    val telemetry: StateFlow<TriggerTelemetry> = _telemetry.asStateFlow()

    // Internal state
    private val acceptedDownTimes = mutableListOf<Long>()
    private var currentDownTime: Long? = null
    private var currentIsHold = false
    private var lastAcceptedDown = 0L
    private var cooldownUntil = 0L

    var onSequenceStarted: (() -> Unit)? = null
    var onSequenceCleared: (() -> Unit)? = null

    fun setSensitivity(presetName: String) {
        val preset = SensitivityPreset.fromString(presetName)
        windowMs = preset.windowMs
    }

    @Synchronized
    fun onKeyEvent(
        keyCode: SosKeyCode,
        action: SosKeyAction,
        eventTime: Long,
        repeatCount: Int = 0,
        isLongPress: Boolean = false
    ): Boolean {
        // 1. Cooldown check first
        if (eventTime < cooldownUntil) {
            val remainingCooldown = ((cooldownUntil - eventTime + 999) / 1000).coerceAtLeast(1)
            updateTelemetry(
                lastRejection = "Cooldown active ($remainingCooldown s remaining)",
                timestamp = eventTime,
                cooldownActive = true
            )
            return false
        }

        // 2. Volume Up ACTION_DOWN clears buffer
        if (keyCode == SosKeyCode.VOLUME_UP) {
            if (action == SosKeyAction.DOWN) {
                resetSequence("Volume Up pressed — sequence cancelled", eventTime)
            }
            return false
        }

        // 3. Ignore any other key
        if (keyCode != SosKeyCode.VOLUME_DOWN) {
            return false
        }

        // 4. Handle Volume Down
        return when (action) {
            SosKeyAction.DOWN -> handleVolumeDown(eventTime, repeatCount, isLongPress)
            SosKeyAction.UP -> handleVolumeUp(eventTime)
        }
    }

    private fun handleVolumeDown(
        eventTime: Long,
        repeatCount: Int,
        isLongPress: Boolean
    ): Boolean {
        // Volume Down ACTION_DOWN with repeatCount > 0 or isLongPress is a HOLD (clear buffer)
        if (repeatCount > 0 || isLongPress) {
            currentIsHold = true
            currentDownTime = null
            acceptedDownTimes.clear()
            updateTelemetry(
                lastRejection = "Hold detected on key down — buffer cleared",
                timestamp = eventTime
            )
            onSequenceCleared?.invoke()
            return false
        }

        // Bounce is measured DOWN-to-DOWN against lastAcceptedDown
        if (lastAcceptedDown > 0L && (eventTime - lastAcceptedDown) < minGapMs) {
            currentDownTime = null
            updateTelemetry(
                lastRejection = "Debounce rejected (${eventTime - lastAcceptedDown}ms < ${minGapMs}ms)",
                timestamp = eventTime
            )
            return false
        }

        val wasEmpty = acceptedDownTimes.isEmpty()
        currentDownTime = eventTime
        lastAcceptedDown = eventTime
        currentIsHold = false

        if (wasEmpty) {
            onSequenceStarted?.invoke()
        }

        return false
    }

    private fun handleVolumeUp(eventTime: Long): Boolean {
        val downTime = currentDownTime
        currentDownTime = null

        if (downTime == null || currentIsHold) {
            return false
        }

        // Check hold duration
        val duration = eventTime - downTime
        if (duration > maxPressDurationMs) {
            acceptedDownTimes.clear()
            updateTelemetry(
                lastRejection = "Hold too long (${duration}ms > ${maxPressDurationMs}ms)",
                timestamp = eventTime
            )
            onSequenceCleared?.invoke()
            return false
        }

        // Valid press: append DOWN time
        acceptedDownTimes.add(downTime)

        // Drop entries older than currentDownTime - windowMs (sliding window)
        val cutoff = downTime - windowMs
        val removed = acceptedDownTimes.removeAll { it < cutoff }
        if (removed && acceptedDownTimes.isEmpty()) {
            onSequenceCleared?.invoke()
        }

        val intervals = if (acceptedDownTimes.size > 1) {
            acceptedDownTimes.zipWithNext { a, b -> b - a }
        } else {
            emptyList()
        }

        // Trigger on fifth valid UP
        if (acceptedDownTimes.size >= requiredPresses) {
            acceptedDownTimes.clear()
            cooldownUntil = eventTime + cooldownMs
            _telemetry.value = TriggerTelemetry(
                currentCount = requiredPresses,
                intervals = intervals,
                lastRejectionReason = null,
                lastPressTimestamp = eventTime,
                isCooldownActive = true
            )
            return true
        }

        _telemetry.value = TriggerTelemetry(
            currentCount = acceptedDownTimes.size,
            intervals = intervals,
            lastRejectionReason = null,
            lastPressTimestamp = eventTime,
            isCooldownActive = false
        )
        return false
    }

    @Synchronized
    fun resetSequence(reason: String? = null, timestamp: Long = 0L) {
        acceptedDownTimes.clear()
        currentDownTime = null
        currentIsHold = false
        updateTelemetry(lastRejection = reason, timestamp = timestamp)
        onSequenceCleared?.invoke()
    }

    @Synchronized
    fun clearCooldown() {
        cooldownUntil = 0L
        _telemetry.value = _telemetry.value.copy(isCooldownActive = false)
    }

    private fun updateTelemetry(
        lastRejection: String?,
        timestamp: Long = 0L,
        cooldownActive: Boolean = false
    ) {
        _telemetry.value = _telemetry.value.copy(
            currentCount = acceptedDownTimes.size,
            lastRejectionReason = lastRejection,
            lastPressTimestamp = if (timestamp > 0L) timestamp else _telemetry.value.lastPressTimestamp,
            isCooldownActive = cooldownActive || (timestamp > 0L && timestamp < cooldownUntil)
        )
    }
}
