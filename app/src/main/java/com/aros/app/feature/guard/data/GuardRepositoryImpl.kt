package com.aros.app.feature.guard.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aros.app.core.TimeFormatter
import com.aros.app.feature.guard.domain.BlockedTarget
import com.aros.app.feature.guard.domain.FailedAttemptRecord
import com.aros.app.feature.guard.domain.GuardConfig
import com.aros.app.feature.guard.domain.GuardEngine
import com.aros.app.feature.guard.domain.GuardRepository
import com.aros.app.feature.guard.domain.ObservedWindow
import com.aros.app.feature.guard.domain.RecoveryResult
import com.aros.app.feature.guard.domain.RelockDelay
import com.aros.app.feature.guard.domain.TamperLogEntry
import com.aros.app.feature.guard.domain.VerifyResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Base64

private val Context.guardDataStore: DataStore<Preferences> by preferencesDataStore(name = "ar_os_guard_settings")

class GuardRepositoryImpl(
    private val context: Context,
    private val guardEngine: GuardEngine,
    private val scope: CoroutineScope,
    private val cryptoDispatcher: CoroutineDispatcher = Dispatchers.Default
) : GuardRepository {

    private object Keys {
        val IS_CONFIGURED = booleanPreferencesKey("guard_is_configured")
        val PIN_HASH = stringPreferencesKey("guard_pin_hash")
        val SALT = stringPreferencesKey("guard_salt")
        val RECOVERY_HASH = stringPreferencesKey("guard_recovery_hash")
        val RELOCK_DELAY_SECONDS = longPreferencesKey("guard_relock_delay_seconds")
        val GO_TO_HOME_ON_LOCK = booleanPreferencesKey("guard_go_to_home_on_lock")
        val FAILED_ATTEMPTS = intPreferencesKey("guard_failed_attempts")

        // NEW epoch-based keys per Fix 2
        val LOCKOUT_UNTIL_EPOCH = longPreferencesKey("guard_lockout_until_epoch")
        val LEARN_EXPIRES_EPOCH = longPreferencesKey("guard_learn_expires_epoch")

        // Old keys to ignore and clean up
        val LEGACY_LOCKOUT_UNTIL_ELAPSED = longPreferencesKey("guard_lockout_until_elapsed")
        val LEGACY_LEARN_MODE_EXPIRES_ELAPSED = longPreferencesKey("guard_learn_mode_expires_elapsed")

        val FAILED_HISTORY_RAW = stringPreferencesKey("guard_failed_history_raw")
        val CUSTOM_BLOCKED_RAW = stringPreferencesKey("guard_custom_blocked_raw")
        val ALLOWED_CALL_SOS_RAW = stringPreferencesKey("guard_allowed_call_sos_raw")
        val TAMPER_LOGS_RAW = stringPreferencesKey("guard_tamper_logs_raw")
    }

    private val _isServiceConnected = MutableStateFlow(false)
    override val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    private val _isGateShowing = MutableStateFlow(false)
    override val isGateShowing: StateFlow<Boolean> = _isGateShowing.asStateFlow()

    private val _isSessionUnlocked = MutableStateFlow(false)
    override val isSessionUnlocked: StateFlow<Boolean> = _isSessionUnlocked.asStateFlow()

    // In-memory ring buffer of up to 50 observed windows for Guard Lab
    private val _observedWindows = MutableStateFlow<List<ObservedWindow>>(emptyList())
    override val observedWindows: StateFlow<List<ObservedWindow>> = _observedWindows.asStateFlow()

    init {
        // Proactively clean up legacy elapsed keys so they cannot pollute DataStore
        scope.launch {
            try {
                context.guardDataStore.edit { prefs ->
                    if (prefs.contains(Keys.LEGACY_LOCKOUT_UNTIL_ELAPSED)) {
                        prefs.remove(Keys.LEGACY_LOCKOUT_UNTIL_ELAPSED)
                    }
                    if (prefs.contains(Keys.LEGACY_LEARN_MODE_EXPIRES_ELAPSED)) {
                        prefs.remove(Keys.LEGACY_LEARN_MODE_EXPIRES_ELAPSED)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override val configFlow: Flow<GuardConfig> = context.guardDataStore.data.map { prefs ->
        try {
            val configured = prefs[Keys.IS_CONFIGURED] ?: false
            val pinHash = prefs[Keys.PIN_HASH] ?: ""
            val salt = prefs[Keys.SALT] ?: ""
            val recoveryHash = prefs[Keys.RECOVERY_HASH] ?: ""
            val relockDelay = prefs[Keys.RELOCK_DELAY_SECONDS] ?: 0L
            val goToHome = prefs[Keys.GO_TO_HOME_ON_LOCK] ?: true
            val failed = prefs[Keys.FAILED_ATTEMPTS] ?: 0

            val rawLockoutEpoch = prefs[Keys.LOCKOUT_UNTIL_EPOCH] ?: 0L
            val rawLearnEpoch = prefs[Keys.LEARN_EXPIRES_EPOCH] ?: 0L

            val now = System.currentTimeMillis()
            val clampedLockout = guardEngine.clampLockoutUntilEpoch(rawLockoutEpoch, failed, now)
            val clampedLearn = guardEngine.clampLearnModeExpiresEpoch(rawLearnEpoch, now)

            val historyRaw = prefs[Keys.FAILED_HISTORY_RAW] ?: ""
            val customBlockedRaw = prefs[Keys.CUSTOM_BLOCKED_RAW] ?: ""
            val allowedCallSosRaw = prefs[Keys.ALLOWED_CALL_SOS_RAW]
            val tamperRaw = prefs[Keys.TAMPER_LOGS_RAW] ?: ""

            val history = parseHistory(historyRaw)
            val customBlocked = parseCustomBlocked(customBlockedRaw)
            val allowedCallSos = if (allowedCallSosRaw == null) {
                GuardConfig.DEFAULT_ALLOWED_CALL_SOS_PACKAGES
            } else {
                parseAllowedCallSos(allowedCallSosRaw)
            }
            val tamperLogs = parseTamperLogs(tamperRaw)

            GuardConfig(
                isConfigured = configured && pinHash.isNotBlank() && salt.isNotBlank(),
                pinHashBase64 = pinHash,
                saltBase64 = salt,
                recoveryHashBase64 = recoveryHash,
                relockDelaySeconds = relockDelay,
                goToHomeOnLock = goToHome,
                consecutiveFailedAttempts = failed,
                lockoutUntilEpochMillis = clampedLockout,
                failedAttemptHistory = history,
                customBlockedTargets = customBlocked,
                allowedCallSosPackages = allowedCallSos,
                learnModeExpiresAtEpochMillis = clampedLearn,
                tamperLogs = tamperLogs
            )
        } catch (_: Exception) {
            // Corruption fallback: Treat as DISABLED so the user is never bricked
            GuardConfig(isConfigured = false)
        }
    }

    private val configSnapshot: StateFlow<GuardConfig> = configFlow.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = GuardConfig()
    )

    private fun parseHistory(raw: String): List<FailedAttemptRecord> {
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 3) {
                val timestamp = parts[0].toLongOrNull() ?: 0L
                val timeStr = parts[1]
                val count = parts[2].toIntOrNull() ?: 0
                FailedAttemptRecord(timestamp, timeStr, count)
            } else null
        }
    }

    private fun serializeHistory(history: List<FailedAttemptRecord>): String {
        return history.joinToString("\n") { "${it.timestampMillis}|${it.formattedTime12h}|${it.totalFailedCount}" }
    }

    private fun parseCustomBlocked(raw: String): List<BlockedTarget> {
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                val pkg = parts[0]
                val cls = if (parts.size > 1) parts[1] else ""
                val label = if (parts.size > 2) parts[2] else ""
                BlockedTarget(pkg, cls, label)
            } else null
        }
    }

    private fun serializeCustomBlocked(targets: List<BlockedTarget>): String {
        return targets.joinToString("\n") { "${it.packageName}|${it.className}|${it.label}" }
    }

    private fun parseTamperLogs(raw: String): List<TamperLogEntry> {
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 5) {
                val id = parts[0].toLongOrNull() ?: 0L
                val timestamp = parts[1].toLongOrNull() ?: 0L
                val formattedTime = parts[2]
                val eventType = parts[3]
                val detail = parts[4]
                TamperLogEntry(id, timestamp, formattedTime, eventType, detail)
            } else null
        }
    }

    private fun serializeTamperLogs(logs: List<TamperLogEntry>): String {
        return logs.joinToString("\n") { "${it.id}|${it.timestampMillis}|${it.formattedTime12h}|${it.eventType}|${it.detail}" }
    }

    override fun recordObservedWindow(window: ObservedWindow) {
        val current = _observedWindows.value.toMutableList()
        // Deduplicate recent identical window
        if (current.isNotEmpty() &&
            current.first().packageName == window.packageName &&
            current.first().className == window.className
        ) {
            return
        }
        current.add(0, window)
        // Keep at most 50 windows in memory
        if (current.size > 50) {
            _observedWindows.value = current.take(50)
        } else {
            _observedWindows.value = current
        }
    }

    override suspend fun startLearnMode(durationMinutes: Int) {
        val expiresAt = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        context.guardDataStore.edit { prefs ->
            prefs[Keys.LEARN_EXPIRES_EPOCH] = expiresAt
            prefs.remove(Keys.LEGACY_LEARN_MODE_EXPIRES_ELAPSED)
        }
    }

    override suspend fun stopLearnMode() {
        context.guardDataStore.edit { prefs ->
            prefs[Keys.LEARN_EXPIRES_EPOCH] = 0L
            prefs.remove(Keys.LEGACY_LEARN_MODE_EXPIRES_ELAPSED)
        }
    }

    override suspend fun addCustomBlockedTarget(target: BlockedTarget) {
        val current = configSnapshot.value.customBlockedTargets.toMutableList()
        if (current.none { it.packageName == target.packageName && it.className == target.className }) {
            current.add(target)
            context.guardDataStore.edit { prefs ->
                prefs[Keys.CUSTOM_BLOCKED_RAW] = serializeCustomBlocked(current)
            }
        }
    }

    override suspend fun removeCustomBlockedTarget(target: BlockedTarget) {
        val current = configSnapshot.value.customBlockedTargets.toMutableList()
        current.removeAll { it.packageName == target.packageName && it.className == target.className }
        context.guardDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_BLOCKED_RAW] = serializeCustomBlocked(current)
        }
    }

    private fun parseAllowedCallSos(raw: String): List<String> {
        if (raw.isBlank()) return GuardConfig.DEFAULT_ALLOWED_CALL_SOS_PACKAGES
        return raw.split("\n").map { it.trim() }.filter { it.isNotBlank() }
    }

    private fun serializeAllowedCallSos(packages: List<String>): String {
        return packages.joinToString("\n")
    }

    override suspend fun addAllowedCallSosPackage(packageName: String) {
        val clean = packageName.trim()
        if (clean.isBlank()) return
        val current = configSnapshot.value.allowedCallSosPackages.toMutableList()
        if (!current.any { it.equals(clean, ignoreCase = true) }) {
            current.add(clean)
            context.guardDataStore.edit { prefs ->
                prefs[Keys.ALLOWED_CALL_SOS_RAW] = serializeAllowedCallSos(current)
            }
        }
    }

    override suspend fun removeAllowedCallSosPackage(packageName: String) {
        val clean = packageName.trim()
        val current = configSnapshot.value.allowedCallSosPackages.toMutableList()
        current.removeAll { it.equals(clean, ignoreCase = true) }
        context.guardDataStore.edit { prefs ->
            prefs[Keys.ALLOWED_CALL_SOS_RAW] = serializeAllowedCallSos(current)
        }
    }

    override suspend fun logTamperEvent(eventType: String, detail: String) {
        val current = configSnapshot.value.tamperLogs.toMutableList()
        val now = System.currentTimeMillis()

        // Fix 5.2: skip writing a new entry when the same eventType and detail were already written within 10 seconds
        val recentDuplicate = current.firstOrNull {
            it.eventType == eventType && it.detail == detail && (now - it.timestampMillis) < 10_000L
        }
        if (recentDuplicate != null) {
            return
        }

        val formattedTime = TimeFormatter.formatClock(now, includeSeconds = true)
        val entry = TamperLogEntry(
            id = now,
            timestampMillis = now,
            formattedTime12h = formattedTime,
            eventType = eventType,
            detail = detail
        )
        current.add(0, entry)
        // Keep at most 100 entries in log
        val trimmed = if (current.size > 100) current.take(100) else current
        context.guardDataStore.edit { prefs ->
            prefs[Keys.TAMPER_LOGS_RAW] = serializeTamperLogs(trimmed)
        }
    }

    override suspend fun clearTamperLogs() {
        context.guardDataStore.edit { prefs ->
            prefs[Keys.TAMPER_LOGS_RAW] = ""
        }
    }

    override suspend fun setupGuard(pin: String, recoveryCode: String): Boolean = withContext(cryptoDispatcher) {
        if (!guardEngine.validatePinFormat(pin)) return@withContext false
        val salt = guardEngine.generateSalt()
        val pinHash = guardEngine.hashSecret(pin.toCharArray(), salt)

        val cleanRecovery = recoveryCode.uppercase().filter { !it.isWhitespace() && it != '-' }
        val recoveryHash = guardEngine.hashSecret(cleanRecovery.toCharArray(), salt)

        val saltBase64 = Base64.getEncoder().encodeToString(salt)
        val pinHashBase64 = Base64.getEncoder().encodeToString(pinHash)
        val recoveryHashBase64 = Base64.getEncoder().encodeToString(recoveryHash)

        context.guardDataStore.edit { prefs ->
            prefs[Keys.IS_CONFIGURED] = true
            prefs[Keys.PIN_HASH] = pinHashBase64
            prefs[Keys.SALT] = saltBase64
            prefs[Keys.RECOVERY_HASH] = recoveryHashBase64
            prefs[Keys.FAILED_ATTEMPTS] = 0
            prefs[Keys.LOCKOUT_UNTIL_EPOCH] = 0L
            prefs.remove(Keys.LEGACY_LOCKOUT_UNTIL_ELAPSED)
        }
        _isSessionUnlocked.value = true
        true
    }

    override suspend fun changePin(currentPin: String, newPin: String): Boolean = withContext(cryptoDispatcher) {
        val currentConfig = configSnapshot.value
        if (!currentConfig.isConfigured) return@withContext false
        if (!guardEngine.validatePinFormat(newPin)) return@withContext false

        val isCurrentValid = guardEngine.verifySecret(
            currentPin.toCharArray(),
            currentConfig.pinHashBase64,
            currentConfig.saltBase64
        )
        if (!isCurrentValid) return@withContext false

        val salt = try {
            Base64.getDecoder().decode(currentConfig.saltBase64)
        } catch (_: Exception) {
            guardEngine.generateSalt()
        }
        val newPinHash = guardEngine.hashSecret(newPin.toCharArray(), salt)
        val newPinHashBase64 = Base64.getEncoder().encodeToString(newPinHash)

        context.guardDataStore.edit { prefs ->
            prefs[Keys.PIN_HASH] = newPinHashBase64
            prefs[Keys.FAILED_ATTEMPTS] = 0
            prefs[Keys.LOCKOUT_UNTIL_EPOCH] = 0L
            prefs.remove(Keys.LEGACY_LOCKOUT_UNTIL_ELAPSED)
        }
        true
    }

    override suspend fun disableGuard(currentPin: String): Boolean = withContext(cryptoDispatcher) {
        val currentConfig = configSnapshot.value
        if (!currentConfig.isConfigured) return@withContext true

        val isCurrentValid = guardEngine.verifySecret(
            currentPin.toCharArray(),
            currentConfig.pinHashBase64,
            currentConfig.saltBase64
        )
        if (!isCurrentValid) return@withContext false

        context.guardDataStore.edit { prefs ->
            prefs[Keys.IS_CONFIGURED] = false
            prefs[Keys.PIN_HASH] = ""
            prefs[Keys.RECOVERY_HASH] = ""
            prefs[Keys.FAILED_ATTEMPTS] = 0
            prefs[Keys.LOCKOUT_UNTIL_EPOCH] = 0L
            prefs.remove(Keys.LEGACY_LOCKOUT_UNTIL_ELAPSED)
        }
        _isSessionUnlocked.value = true
        true
    }

    override suspend fun resetPinWithRecoveryCode(code: String, newPin: String): Boolean = withContext(cryptoDispatcher) {
        val currentConfig = configSnapshot.value
        if (!currentConfig.isConfigured) return@withContext false
        if (!guardEngine.validatePinFormat(newPin)) return@withContext false

        val now = System.currentTimeMillis()
        val formattedTime = TimeFormatter.formatClock(now, includeSeconds = false)
        val (recResult, updatedConfig) = guardEngine.evaluateRecoveryAttempt(
            code.toCharArray(),
            currentConfig,
            now,
            formattedTime
        )

        // Persist attempt updates
        context.guardDataStore.edit { prefs ->
            prefs[Keys.FAILED_ATTEMPTS] = updatedConfig.consecutiveFailedAttempts
            prefs[Keys.LOCKOUT_UNTIL_EPOCH] = updatedConfig.lockoutUntilEpochMillis
            prefs.remove(Keys.LEGACY_LOCKOUT_UNTIL_ELAPSED)
            prefs[Keys.FAILED_HISTORY_RAW] = serializeHistory(updatedConfig.failedAttemptHistory)
        }

        if (recResult !is RecoveryResult.Success) {
            return@withContext false
        }

        // Recovery succeeded -> compute new PIN hash using existing salt
        val salt = Base64.getDecoder().decode(currentConfig.saltBase64)
        val newPinHash = guardEngine.hashSecret(newPin.toCharArray(), salt)
        val newPinHashBase64 = Base64.getEncoder().encodeToString(newPinHash)

        context.guardDataStore.edit { prefs ->
            prefs[Keys.PIN_HASH] = newPinHashBase64
            prefs[Keys.FAILED_ATTEMPTS] = 0
            prefs[Keys.LOCKOUT_UNTIL_EPOCH] = 0L
            prefs.remove(Keys.LEGACY_LOCKOUT_UNTIL_ELAPSED)
        }
        _isSessionUnlocked.value = true
        true
    }

    override suspend fun verifyPin(pin: String): VerifyResult = withContext(cryptoDispatcher) {
        val currentConfig = configSnapshot.value
        val now = System.currentTimeMillis()
        val formattedTime = TimeFormatter.formatClock(now, includeSeconds = false)

        val (result, updatedConfig) = guardEngine.evaluatePinAttempt(
            pin.toCharArray(),
            currentConfig,
            now,
            formattedTime
        )

        context.guardDataStore.edit { prefs ->
            prefs[Keys.FAILED_ATTEMPTS] = updatedConfig.consecutiveFailedAttempts
            prefs[Keys.LOCKOUT_UNTIL_EPOCH] = updatedConfig.lockoutUntilEpochMillis
            prefs.remove(Keys.LEGACY_LOCKOUT_UNTIL_ELAPSED)
            prefs[Keys.FAILED_HISTORY_RAW] = serializeHistory(updatedConfig.failedAttemptHistory)
        }

        if (result is VerifyResult.Success) {
            _isSessionUnlocked.value = true
        }

        result
    }

    override suspend fun verifyRecoveryCode(code: String): RecoveryResult = withContext(cryptoDispatcher) {
        val currentConfig = configSnapshot.value
        val now = System.currentTimeMillis()
        val formattedTime = TimeFormatter.formatClock(now, includeSeconds = false)

        val (result, updatedConfig) = guardEngine.evaluateRecoveryAttempt(
            code.toCharArray(),
            currentConfig,
            now,
            formattedTime
        )

        context.guardDataStore.edit { prefs ->
            prefs[Keys.FAILED_ATTEMPTS] = updatedConfig.consecutiveFailedAttempts
            prefs[Keys.LOCKOUT_UNTIL_EPOCH] = updatedConfig.lockoutUntilEpochMillis
            prefs.remove(Keys.LEGACY_LOCKOUT_UNTIL_ELAPSED)
            prefs[Keys.FAILED_HISTORY_RAW] = serializeHistory(updatedConfig.failedAttemptHistory)
        }

        if (result is RecoveryResult.Success) {
            _isSessionUnlocked.value = true
        }

        result
    }

    override suspend fun setRelockDelay(delay: RelockDelay) {
        context.guardDataStore.edit { prefs ->
            prefs[Keys.RELOCK_DELAY_SECONDS] = delay.seconds
        }
    }

    override suspend fun setGoToHomeOnLock(goToHome: Boolean) {
        context.guardDataStore.edit { prefs ->
            prefs[Keys.GO_TO_HOME_ON_LOCK] = goToHome
        }
    }

    override suspend fun clearFailedAttemptHistory() {
        context.guardDataStore.edit { prefs ->
            prefs[Keys.FAILED_HISTORY_RAW] = ""
        }
    }

    override fun unlockSession() {
        _isSessionUnlocked.value = true
    }

    override fun lockSession() {
        _isSessionUnlocked.value = false
    }

    override fun setServiceConnected(connected: Boolean) {
        _isServiceConnected.value = connected
    }

    override fun setGateShowing(showing: Boolean) {
        _isGateShowing.value = showing
    }
}
