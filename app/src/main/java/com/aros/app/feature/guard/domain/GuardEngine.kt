package com.aros.app.feature.guard.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class GuardEngine {

    companion object {
        const val HASH_ALGORITHM = "PBKDF2WithHmacSHA256"
        const val ITERATION_COUNT = 100_000
        const val KEY_LENGTH_BITS = 256
        const val SALT_LENGTH_BYTES = 16
        const val MIN_PIN_LENGTH = 6
        const val MAX_PIN_LENGTH = 12
        const val RECOVERY_CODE_LENGTH = 16

        // Unambiguous alphanumeric characters (omitting 0, O, 1, I)
        const val RECOVERY_CHARSET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"

        val LOCKOUT_LADDER_SECONDS = listOf(
            30L,    // 6th attempt
            60L,    // 7th attempt (1m)
            300L,   // 8th attempt (5m)
            900L,   // 9th attempt (15m)
            3600L   // 10th+ attempt (1h cap)
        )

        const val MAX_LOCKOUT_SECONDS = 3600L
        const val MAX_LEARN_MODE_SECONDS = 600L // 10 minutes
    }

    private val secureRandom = SecureRandom()

    fun validatePinFormat(pin: String): Boolean {
        if (pin.length !in MIN_PIN_LENGTH..MAX_PIN_LENGTH) return false
        return pin.all { it.isDigit() }
    }

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun hashSecret(secret: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret, salt, ITERATION_COUNT, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(HASH_ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    fun generateRecoveryCode(): String {
        val chars = CharArray(RECOVERY_CODE_LENGTH)
        for (i in 0 until RECOVERY_CODE_LENGTH) {
            chars[i] = RECOVERY_CHARSET[secureRandom.nextInt(RECOVERY_CHARSET.length)]
        }
        return String(chars)
    }

    fun computeLockoutSeconds(failedAttempts: Int): Long {
        if (failedAttempts <= 5) return 0L
        val index = (failedAttempts - 6).coerceIn(0, LOCKOUT_LADDER_SECONDS.lastIndex)
        return LOCKOUT_LADDER_SECONDS[index]
    }

    /**
     * Clamps stored lockout expiration against the maximum allowed delay for the failed-attempts count.
     * Prevents reboot corruption where elapsedRealtime values were written as epoch or far-future times.
     */
    fun clampLockoutUntilEpoch(
        storedLockoutEpochMillis: Long,
        failedAttempts: Int,
        nowEpochMillis: Long
    ): Long {
        if (storedLockoutEpochMillis <= 0L || storedLockoutEpochMillis <= nowEpochMillis) {
            return 0L
        }
        val maxDelaySec = computeLockoutSeconds(failedAttempts)
        if (maxDelaySec <= 0L) {
            return 0L
        }
        val maxAllowedEpoch = nowEpochMillis + (maxDelaySec * 1000L)
        return if (storedLockoutEpochMillis > maxAllowedEpoch) {
            maxAllowedEpoch
        } else {
            storedLockoutEpochMillis
        }
    }

    /**
     * Clamps stored learn mode expiration to at most 10 minutes from now.
     * If stored value exceeds 10 minutes from now, it is treated as expired (0L).
     */
    fun clampLearnModeExpiresEpoch(
        storedLearnEpochMillis: Long,
        nowEpochMillis: Long
    ): Long {
        if (storedLearnEpochMillis <= 0L || storedLearnEpochMillis <= nowEpochMillis) {
            return 0L
        }
        val maxAllowedEpoch = nowEpochMillis + (MAX_LEARN_MODE_SECONDS * 1000L)
        return if (storedLearnEpochMillis > maxAllowedEpoch) {
            0L // Treat far-future corruption as expired per Fix 2
        } else {
            storedLearnEpochMillis
        }
    }

    fun verifySecret(
        secret: CharArray,
        storedHashBase64: String,
        saltBase64: String
    ): Boolean {
        if (storedHashBase64.isBlank() || saltBase64.isBlank()) return false
        val salt = try {
            Base64.getDecoder().decode(saltBase64)
        } catch (_: Exception) {
            return false
        }
        val expectedHash = try {
            Base64.getDecoder().decode(storedHashBase64)
        } catch (_: Exception) {
            return false
        }
        val computedHash = hashSecret(secret, salt)
        return MessageDigest.isEqual(computedHash, expectedHash)
    }

    fun evaluatePinAttempt(
        enteredPin: CharArray,
        config: GuardConfig,
        nowEpochMillis: Long,
        formattedTime12h: String
    ): Pair<VerifyResult, GuardConfig> {
        val clampedLockout = clampLockoutUntilEpoch(
            config.lockoutUntilEpochMillis,
            config.consecutiveFailedAttempts,
            nowEpochMillis
        )

        // 1. Check if currently locked out
        if (nowEpochMillis < clampedLockout) {
            val remainingSec = ((clampedLockout - nowEpochMillis) / 1000L).coerceAtLeast(1L)
            return Pair(VerifyResult.LockedOut(remainingSec), config.copy(lockoutUntilEpochMillis = clampedLockout))
        }

        // 2. Verify PIN
        val isCorrect = verifySecret(enteredPin, config.pinHashBase64, config.saltBase64)
        if (isCorrect) {
            val updatedConfig = config.copy(
                consecutiveFailedAttempts = 0,
                lockoutUntilEpochMillis = 0L
            )
            return Pair(VerifyResult.Success, updatedConfig)
        }

        // 3. Failed attempt: increment counter and calculate delay
        val newFailedCount = config.consecutiveFailedAttempts + 1
        val lockoutSec = computeLockoutSeconds(newFailedCount)
        val lockoutUntil = if (lockoutSec > 0) nowEpochMillis + (lockoutSec * 1000L) else 0L

        val newHistoryRecord = FailedAttemptRecord(
            timestampMillis = nowEpochMillis,
            formattedTime12h = formattedTime12h,
            totalFailedCount = newFailedCount
        )
        val updatedHistory = (listOf(newHistoryRecord) + config.failedAttemptHistory).take(20)

        val updatedConfig = config.copy(
            consecutiveFailedAttempts = newFailedCount,
            lockoutUntilEpochMillis = lockoutUntil,
            failedAttemptHistory = updatedHistory
        )

        val remainingFree = (5 - newFailedCount).coerceAtLeast(0)
        return Pair(
            VerifyResult.WrongPin(remainingFreeAttempts = remainingFree, lockoutSeconds = lockoutSec),
            updatedConfig
        )
    }

    fun evaluateRecoveryAttempt(
        enteredCode: CharArray,
        config: GuardConfig,
        nowEpochMillis: Long,
        formattedTime12h: String
    ): Pair<RecoveryResult, GuardConfig> {
        val clampedLockout = clampLockoutUntilEpoch(
            config.lockoutUntilEpochMillis,
            config.consecutiveFailedAttempts,
            nowEpochMillis
        )

        // 1. Check if currently locked out
        if (nowEpochMillis < clampedLockout) {
            val remainingSec = ((clampedLockout - nowEpochMillis) / 1000L).coerceAtLeast(1L)
            return Pair(RecoveryResult.LockedOut(remainingSec), config.copy(lockoutUntilEpochMillis = clampedLockout))
        }

        // Clean code (normalize uppercase, remove spaces/hyphens)
        val normalized = String(enteredCode).uppercase().filter { !it.isWhitespace() && it != '-' }.toCharArray()
        val isCorrect = verifySecret(normalized, config.recoveryHashBase64, config.saltBase64)

        if (isCorrect) {
            val updatedConfig = config.copy(
                consecutiveFailedAttempts = 0,
                lockoutUntilEpochMillis = 0L
            )
            return Pair(RecoveryResult.Success, updatedConfig)
        }

        val newFailedCount = config.consecutiveFailedAttempts + 1
        val lockoutSec = computeLockoutSeconds(newFailedCount)
        val lockoutUntil = if (lockoutSec > 0) nowEpochMillis + (lockoutSec * 1000L) else 0L

        val newHistoryRecord = FailedAttemptRecord(
            timestampMillis = nowEpochMillis,
            formattedTime12h = formattedTime12h,
            totalFailedCount = newFailedCount
        )
        val updatedHistory = (listOf(newHistoryRecord) + config.failedAttemptHistory).take(20)

        val updatedConfig = config.copy(
            consecutiveFailedAttempts = newFailedCount,
            lockoutUntilEpochMillis = lockoutUntil,
            failedAttemptHistory = updatedHistory
        )

        val remainingFree = (5 - newFailedCount).coerceAtLeast(0)
        return Pair(
            RecoveryResult.WrongCode(remainingFreeAttempts = remainingFree, lockoutSeconds = lockoutSec),
            updatedConfig
        )
    }

    fun shouldRelockAfterScreenOff(
        screenOffElapsedRealtime: Long,
        currentElapsedRealtime: Long,
        relockDelaySeconds: Long
    ): Boolean {
        if (screenOffElapsedRealtime <= 0L) return true
        if (relockDelaySeconds <= 0L) return true
        val elapsedMillis = currentElapsedRealtime - screenOffElapsedRealtime
        return elapsedMillis >= (relockDelaySeconds * 1000L)
    }
}
