package com.aros.app.feature.sos.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ContactRepository {
    fun getAllContactsFlow(): Flow<List<SosContact>>
    suspend fun getAllContacts(): List<SosContact>
    fun getPrimaryContactFlow(): Flow<SosContact?>
    suspend fun getPrimaryContact(): SosContact?
    suspend fun addContact(name: String, number: String, role: ContactRole): Result<Long>
    suspend fun updateContact(contact: SosContact): Result<Unit>
    suspend fun deleteContact(id: Long): Result<Unit>
    suspend fun setPrimaryContact(id: Long): Result<Unit>
}

interface SettingsRepository {
    val configFlow: Flow<SosConfig>
    val configSnapshot: StateFlow<SosConfig>
    suspend fun setMode(mode: SosMode)
    suspend fun setCountdownSeconds(seconds: Int)
    suspend fun setSensitivityPreset(preset: String)
    suspend fun setRestoreVolumeAfterSos(restore: Boolean)
    suspend fun setStartCallOnSpeaker(speaker: Boolean)
    suspend fun setShowStatusNotification(show: Boolean)
    suspend fun setSelectedSimId(simId: String?)
    suspend fun setSetupCompleted(completed: Boolean)
    suspend fun setUse24HourTime(use24Hour: Boolean)
}

interface SosEventLog {
    val eventsFlow: Flow<List<SosEvent>>
    suspend fun logEvent(
        type: SosEventType,
        mode: SosMode,
        contactName: String?,
        deviceLocked: Boolean,
        screenInteractive: Boolean,
        detail: String = ""
    )
    suspend fun clearLog()
}

interface CallRequester {
    sealed class CallResult {
        object PlacedDirectly : CallResult()
        data class DialFallback(val reason: String) : CallResult()
        data class Failed(val error: String) : CallResult()
    }

    suspend fun requestCall(phoneNumber: String, isTestMode: Boolean, startCallOnSpeaker: Boolean = false): CallResult
}

interface Haptics {
    fun triggerDetected()
    fun countdownTick(remainingSeconds: Int)
    fun countdownCancelled()
    fun callInitiating()
}

interface SosNotifier {
    fun showOngoingNotification(state: SosState.Countdown)
    fun showCallingNotification(state: SosState.Dialing)
    fun showDialFailedFallback(targetContact: SosContact)
    fun showBackupContactsNotification(backupContacts: List<SosContact>)
    fun showStatusNotification(mode: SosMode, readiness: OverallReadinessStatus)
    fun dismissStatusNotification()
    fun showReadinessWarning(issues: List<String>)
    fun dismissSosNotification()
}

interface DeviceStateProvider {
    fun isDeviceLocked(): Boolean
    fun isScreenInteractive(): Boolean
    fun isAirplaneModeOn(): Boolean
    fun hasSimCard(): Boolean
}

interface SosScreenLauncher {
    fun launchSosCountdown()
    fun bringAppToFront()
}

interface ReadinessChecker {
    val readinessReportFlow: Flow<ReadinessReport>
    suspend fun checkReadiness(): ReadinessReport
}
