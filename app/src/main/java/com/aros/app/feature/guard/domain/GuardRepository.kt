package com.aros.app.feature.guard.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface GuardRepository {
    val configFlow: Flow<GuardConfig>
    val isServiceConnected: StateFlow<Boolean>
    val isGateShowing: StateFlow<Boolean>
    val isSessionUnlocked: StateFlow<Boolean>

    suspend fun setupGuard(pin: String, recoveryCode: String): Boolean
    suspend fun changePin(currentPin: String, newPin: String): Boolean
    suspend fun disableGuard(currentPin: String): Boolean
    suspend fun resetPinWithRecoveryCode(code: String, newPin: String): Boolean

    suspend fun verifyPin(pin: String): VerifyResult
    suspend fun verifyRecoveryCode(code: String): RecoveryResult

    suspend fun setRelockDelay(delay: RelockDelay)
    suspend fun setGoToHomeOnLock(goToHome: Boolean)
    suspend fun clearFailedAttemptHistory()

    // Lost Guard & Guard Lab
    val observedWindows: StateFlow<List<ObservedWindow>>
    fun recordObservedWindow(window: ObservedWindow)
    suspend fun startLearnMode(durationMinutes: Int = 10)
    suspend fun stopLearnMode()
    suspend fun addCustomBlockedTarget(target: BlockedTarget)
    suspend fun removeCustomBlockedTarget(target: BlockedTarget)
    suspend fun addAllowedCallSosPackage(packageName: String)
    suspend fun removeAllowedCallSosPackage(packageName: String)
    suspend fun logTamperEvent(eventType: String, detail: String)
    suspend fun clearTamperLogs()

    fun unlockSession()
    fun lockSession()

    fun setServiceConnected(connected: Boolean)
    fun setGateShowing(showing: Boolean)
}
