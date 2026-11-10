package com.aros.app

import com.aros.app.feature.guard.domain.GuardConfig
import com.aros.app.feature.guard.domain.GuardEngine
import com.aros.app.feature.guard.domain.RecoveryResult
import com.aros.app.feature.guard.domain.VerifyResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class GuardEngineTest {

    private val engine = GuardEngine()

    @Test
    fun `validatePinFormat enforces 6 to 12 digit constraint`() {
        assertFalse(engine.validatePinFormat("12345")) // too short (5)
        assertTrue(engine.validatePinFormat("123456")) // minimum valid (6)
        assertTrue(engine.validatePinFormat("1234567890")) // valid (10)
        assertTrue(engine.validatePinFormat("123456789012")) // maximum valid (12)
        assertFalse(engine.validatePinFormat("1234567890123")) // too long (13)
        assertFalse(engine.validatePinFormat("12345a")) // non-digit
        assertFalse(engine.validatePinFormat(""))
    }

    @Test
    fun `hashSecret and verifySecret correctly validates correct PIN and rejects wrong PIN`() {
        val pin = "849201"
        val salt = engine.generateSalt()
        val hash = engine.hashSecret(pin.toCharArray(), salt)

        val saltBase64 = Base64.getEncoder().encodeToString(salt)
        val hashBase64 = Base64.getEncoder().encodeToString(hash)

        assertTrue(engine.verifySecret(pin.toCharArray(), hashBase64, saltBase64))
        assertFalse(engine.verifySecret("849202".toCharArray(), hashBase64, saltBase64))
        assertFalse(engine.verifySecret("".toCharArray(), hashBase64, saltBase64))
    }

    @Test
    fun `lockout ladder enforces 5 free tries then increasing lockout intervals`() {
        assertEquals(0L, engine.computeLockoutSeconds(1))
        assertEquals(0L, engine.computeLockoutSeconds(5))
        assertEquals(30L, engine.computeLockoutSeconds(6))
        assertEquals(60L, engine.computeLockoutSeconds(7))
        assertEquals(300L, engine.computeLockoutSeconds(8))
        assertEquals(900L, engine.computeLockoutSeconds(9))
        assertEquals(3600L, engine.computeLockoutSeconds(10))
        assertEquals(3600L, engine.computeLockoutSeconds(25)) // Capped at 1 hour
    }

    @Test
    fun `evaluatePinAttempt transitions through free tries and lockout correctly`() {
        val pin = "123456"
        val salt = engine.generateSalt()
        val hash = engine.hashSecret(pin.toCharArray(), salt)
        val saltBase64 = Base64.getEncoder().encodeToString(salt)
        val hashBase64 = Base64.getEncoder().encodeToString(hash)

        var config = GuardConfig(
            isConfigured = true,
            pinHashBase64 = hashBase64,
            saltBase64 = saltBase64
        )

        var elapsed = 100_000L

        // 1st wrong try
        val (res1, cfg1) = engine.evaluatePinAttempt("000000".toCharArray(), config, elapsed, "12:00 PM")
        assertTrue(res1 is VerifyResult.WrongPin)
        assertEquals(4, (res1 as VerifyResult.WrongPin).remainingFreeAttempts)
        assertEquals(0L, res1.lockoutSeconds)
        config = cfg1

        // 2nd through 5th wrong tries
        for (i in 2..5) {
            val (_, nextCfg) = engine.evaluatePinAttempt("000000".toCharArray(), config, elapsed, "12:00 PM")
            config = nextCfg
        }
        assertEquals(5, config.consecutiveFailedAttempts)

        // 6th wrong try -> triggers 30s lockout
        val (res6, cfg6) = engine.evaluatePinAttempt("000000".toCharArray(), config, elapsed, "12:00 PM")
        assertTrue(res6 is VerifyResult.WrongPin)
        assertEquals(30L, (res6 as VerifyResult.WrongPin).lockoutSeconds)
        assertEquals(elapsed + 30_000L, cfg6.lockoutUntilElapsedRealtime)
        config = cfg6

        // Attempt during active lockout is rejected with LockedOut
        val (resLocked, _) = engine.evaluatePinAttempt(pin.toCharArray(), config, elapsed + 10_000L, "12:00 PM")
        assertTrue(resLocked is VerifyResult.LockedOut)
        assertEquals(20L, (resLocked as VerifyResult.LockedOut).remainingLockoutSeconds)

        // After lockout expires, entering correct PIN succeeds and resets consecutive failed attempts
        val (resSuccess, cfgSuccess) = engine.evaluatePinAttempt(pin.toCharArray(), config, elapsed + 31_000L, "12:01 PM")
        assertTrue(resSuccess is VerifyResult.Success)
        assertEquals(0, cfgSuccess.consecutiveFailedAttempts)
        assertEquals(0L, cfgSuccess.lockoutUntilElapsedRealtime)
    }

    @Test
    fun `recovery code generation produces unambiguous characters and verifies properly`() {
        val code = engine.generateRecoveryCode()
        assertEquals(16, code.length)
        assertFalse(code.contains('0'))
        assertFalse(code.contains('O'))
        assertFalse(code.contains('1'))
        assertFalse(code.contains('I'))

        val salt = engine.generateSalt()
        val hash = engine.hashSecret(code.toCharArray(), salt)
        val saltBase64 = Base64.getEncoder().encodeToString(salt)
        val hashBase64 = Base64.getEncoder().encodeToString(hash)

        val config = GuardConfig(
            isConfigured = true,
            recoveryHashBase64 = hashBase64,
            saltBase64 = saltBase64
        )

        // Entering with formatting dashes and lowercase should succeed
        val formattedEntered = code.lowercase().chunked(4).joinToString("-")
        val (recResult, _) = engine.evaluateRecoveryAttempt(formattedEntered.toCharArray(), config, 50_000L, "12:00 PM")
        assertTrue(recResult is RecoveryResult.Success)

        // Wrong recovery code fails
        val (recWrong, _) = engine.evaluateRecoveryAttempt("WRONGCODE1234567".toCharArray(), config, 50_000L, "12:00 PM")
        assertTrue(recWrong is RecoveryResult.WrongCode)
    }

    @Test
    fun `shouldRelockAfterScreenOff correctly measures elapsed delay`() {
        val screenOff = 100_000L

        // Immediately (0s) relocks immediately
        assertTrue(engine.shouldRelockAfterScreenOff(screenOff, 100_000L, 0L))
        assertTrue(engine.shouldRelockAfterScreenOff(screenOff, 100_500L, 0L))

        // 30s delay
        assertFalse(engine.shouldRelockAfterScreenOff(screenOff, 100_000L + 15_000L, 30L))
        assertTrue(engine.shouldRelockAfterScreenOff(screenOff, 100_000L + 30_000L, 30L))
        assertTrue(engine.shouldRelockAfterScreenOff(screenOff, 100_000L + 45_000L, 30L))

        // 2 min delay (120s)
        assertFalse(engine.shouldRelockAfterScreenOff(screenOff, 100_000L + 60_000L, 120L))
        assertTrue(engine.shouldRelockAfterScreenOff(screenOff, 100_000L + 120_000L, 120L))
    }
}
