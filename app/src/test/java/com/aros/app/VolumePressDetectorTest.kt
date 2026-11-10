package com.aros.app

import com.aros.app.feature.sos.domain.SosKeyAction
import com.aros.app.feature.sos.domain.SosKeyCode
import com.aros.app.feature.sos.domain.VolumePressDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VolumePressDetectorTest {

    private lateinit var detector: VolumePressDetector

    @Before
    fun setup() {
        detector = VolumePressDetector()
        detector.setSensitivity("NORMAL") // windowMs = 2500ms
    }

    private fun down(eventTime: Long) = detector.onKeyEvent(
        keyCode = SosKeyCode.VOLUME_DOWN,
        action = SosKeyAction.DOWN,
        eventTime = eventTime
    )

    private fun up(eventTime: Long) = detector.onKeyEvent(
        keyCode = SosKeyCode.VOLUME_DOWN,
        action = SosKeyAction.UP,
        eventTime = eventTime
    )

    @Test
    fun testU1_FiveRapidPressesTriggers() {
        var now = 1000L
        val results = mutableListOf<Boolean>()

        for (i in 1..5) {
            down(now)
            now += 50 // 50ms hold
            val triggered = up(now)
            results.add(triggered)
            now += 200 // 200ms gap
        }

        assertFalse("Presses 1..4 should not trigger", results.take(4).any { it })
        assertTrue("5th press should trigger", results[4])
        assertEquals(5, detector.telemetry.value.currentCount)
        assertTrue(detector.telemetry.value.isCooldownActive)
    }

    @Test
    fun testU2_SlowPressesTimeoutAndReset() {
        var now = 1000L
        // Press 1
        down(now)
        up(now + 50)

        // Wait 2600ms (exceeds windowMs of 2500ms)
        now += 2600L

        // Press 2
        down(now)
        val triggered = up(now + 50)

        assertFalse(triggered)
        // First press has aged out of sliding window
        assertEquals(1, detector.telemetry.value.currentCount)
    }

    @Test
    fun testU3_LongHoldIsRejected() {
        val now = 1000L
        down(now)
        // Held for 650ms (exceeds maxPressDurationMs of 600ms)
        val triggered = up(now + 650L)

        assertFalse(triggered)
        assertEquals(0, detector.telemetry.value.currentCount)
        assertTrue(detector.telemetry.value.lastRejectionReason?.contains("Hold too long") == true)
    }

    @Test
    fun testU4_DebounceRejectionForChatter() {
        var now = 1000L
        // Press 1
        down(now)
        up(now + 20)

        // Press 2 arrives only 25ms later (bounce < minGapMs 40ms)
        now += 25L
        down(now)
        val triggered = up(now + 20)

        assertFalse(triggered)
        assertEquals(1, detector.telemetry.value.currentCount)
        assertTrue(detector.telemetry.value.lastRejectionReason?.contains("Debounce rejected") == true)
    }

    @Test
    fun testU5_VolumeUpImmediatelyCancelsSequence() {
        var now = 1000L
        for (i in 1..3) {
            down(now)
            up(now + 50)
            now += 150
        }
        assertEquals(3, detector.telemetry.value.currentCount)

        // Volume Up pressed
        detector.onKeyEvent(
            keyCode = SosKeyCode.VOLUME_UP,
            action = SosKeyAction.DOWN,
            eventTime = now
        )
        assertEquals(0, detector.telemetry.value.currentCount)
        assertTrue(detector.telemetry.value.lastRejectionReason?.contains("Volume Up") == true)
    }

    @Test
    fun testU6_CooldownLockoutPreventsRetrigger() {
        var now = 1000L
        for (i in 1..5) {
            down(now)
            up(now + 50)
            now += 150
        }
        assertTrue(detector.telemetry.value.isCooldownActive)

        // Immediate next press during 8s cooldown
        now += 2000L
        down(now)
        val triggered = up(now + 50)

        assertFalse(triggered)
        assertTrue(detector.telemetry.value.lastRejectionReason?.contains("Cooldown active") == true)
    }

    // Spec §6 Regression Tests:
    @Test
    fun testRegression_FivePressesTaking3500msTotal_RejectedInNormalMode() {
        // In Normal mode, windowMs is 2500ms. 5 presses evenly spaced over 3500ms must NOT trigger.
        var now = 1000L
        var triggered = false
        for (i in 1..5) {
            down(now)
            now += 100
            val t = up(now)
            if (t) triggered = true
            now += 600 // total interval = 700ms * 5 = 3500ms
        }
        assertFalse("5 presses taking 3.5s must not trigger in 2.5s Normal window", triggered)
    }

    @Test
    fun testRegression_FivePressesHeld450ms_AcceptedPerSpec() {
        // Spec §6 has maxPressDurationMs = 600ms, so 450ms press duration must be ACCEPTED
        var now = 10000L
        var triggered = false
        for (i in 1..5) {
            down(now)
            now += 450 // 450ms hold (valid, > 400ms which old V1 rejected)
            val t = up(now)
            if (t) triggered = true
            now += 45 // 45ms gap
        }
        // Total down to down: 4 intervals of 495ms = 1980ms <= 2500ms
        assertTrue("Presses held 450ms must be accepted under 600ms spec limit", triggered)
    }

    @Test
    fun testRegression_Cooldown8000ms_BlocksRetriggerAt5100ms() {
        var now = 1000L
        for (i in 1..5) {
            down(now)
            now += 50
            up(now)
            now += 150
        }
        val triggerTime = now - 150

        // Attempt second trigger 5.1s later (V1 allowed because 5s, Spec §6 requires 8s cooldown)
        now = triggerTime + 5100L
        down(now)
        val triggered = up(now + 50)

        assertFalse("Second trigger at 5.1s must be blocked by 8000ms cooldown", triggered)
        assertTrue(detector.telemetry.value.isCooldownActive)
    }
}
