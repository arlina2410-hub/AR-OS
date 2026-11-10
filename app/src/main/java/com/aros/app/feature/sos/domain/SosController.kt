package com.aros.app.feature.sos.domain

import com.aros.app.core.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SosController(
    private val scope: CoroutineScope,
    private val clock: Clock,
    private val contactRepository: ContactRepository,
    private val settingsRepository: SettingsRepository,
    private val callRequester: CallRequester,
    private val haptics: Haptics,
    private val notifier: SosNotifier,
    private val eventLog: SosEventLog,
    private val deviceState: DeviceStateProvider,
    private val screenLauncher: SosScreenLauncher
) {
    private val _state = MutableStateFlow<SosState>(SosState.Idle)
    val state: StateFlow<SosState> = _state.asStateFlow()

    private var flowJob: Job? = null

    fun onTrigger() {
        if (_state.value !is SosState.Idle) return

        flowJob?.cancel()
        flowJob = scope.launch {
            val contact = contactRepository.getPrimaryContact()
            val config = settingsRepository.configSnapshot.value

            if (contact == null) {
                haptics.triggerDetected()
                eventLog.logEvent(
                    type = SosEventType.NO_CONTACT,
                    mode = config.mode,
                    contactName = null,
                    deviceLocked = deviceState.isDeviceLocked(),
                    screenInteractive = deviceState.isScreenInteractive(),
                    detail = "Trigger ignored: No primary trusted contact configured."
                )
                notifier.showReadinessWarning(listOf("No primary contact configured. Add a contact in AR SOS."))
                return@launch
            }

            eventLog.logEvent(
                type = SosEventType.TRIGGER_DETECTED,
                mode = config.mode,
                contactName = contact.name,
                deviceLocked = deviceState.isDeviceLocked(),
                screenInteractive = deviceState.isScreenInteractive(),
                detail = "Hardware trigger detected."
            )

            val totalSecs = config.countdownSeconds.coerceIn(0, 10)
            if (totalSecs == 0) {
                // 0s countdown: jump directly to dialing
                executeCall(contact, config.mode, config.startCallOnSpeaker)
            } else {
                startCountdown(contact, config, totalSecs)
            }
        }
    }

    private suspend fun startCountdown(contact: SosContact, config: SosConfig, totalSecs: Int) {
        val countdownState = SosState.Countdown(
            remainingSeconds = totalSecs,
            totalSeconds = totalSecs,
            targetContact = contact,
            mode = config.mode
        )
        _state.value = countdownState

        haptics.triggerDetected()
        notifier.showOngoingNotification(countdownState)
        screenLauncher.launchSosCountdown()

        eventLog.logEvent(
            type = SosEventType.COUNTDOWN_STARTED,
            mode = config.mode,
            contactName = contact.name,
            deviceLocked = deviceState.isDeviceLocked(),
            screenInteractive = deviceState.isScreenInteractive(),
            detail = "Starting ${totalSecs}s countdown."
        )

        val deadline = clock.elapsedRealtime() + (totalSecs * 1000L)
        var lastRemaining = totalSecs

        while (true) {
            val currentElapsed = clock.elapsedRealtime()
            val remainingMillis = deadline - currentElapsed
            if (remainingMillis <= 0) break

            val remainingSecs = ((remainingMillis + 999L) / 1000L).toInt()

            if (remainingSecs != lastRemaining) {
                lastRemaining = remainingSecs
                if (_state.value !is SosState.Countdown) return

                val updatedState = SosState.Countdown(
                    remainingSeconds = remainingSecs,
                    totalSeconds = totalSecs,
                    targetContact = contact,
                    mode = config.mode
                )
                _state.value = updatedState
                haptics.countdownTick(remainingSecs)
                notifier.showOngoingNotification(updatedState)
            }

            delay(100L)
        }

        if (_state.value is SosState.Countdown) {
            executeCall(contact, config.mode, config.startCallOnSpeaker)
        }
    }

    fun callNow() {
        val current = _state.value
        if (current is SosState.Countdown) {
            flowJob?.cancel()
            flowJob = scope.launch {
                val config = settingsRepository.configSnapshot.value
                eventLog.logEvent(
                    type = SosEventType.CALL_NOW,
                    mode = current.mode,
                    contactName = current.targetContact.name,
                    deviceLocked = deviceState.isDeviceLocked(),
                    screenInteractive = deviceState.isScreenInteractive(),
                    detail = "Call now requested by user."
                )
                executeCall(current.targetContact, current.mode, config.startCallOnSpeaker)
            }
        }
    }

    private suspend fun executeCall(contact: SosContact, mode: SosMode, startCallOnSpeaker: Boolean) {
        val dialingState = SosState.Dialing(
            targetContact = contact,
            mode = mode,
            method = if (mode == SosMode.LIVE) "TelecomManager" else "Safe Test Simulation"
        )
        _state.value = dialingState

        haptics.callInitiating()
        notifier.showCallingNotification(dialingState)

        if (mode == SosMode.LIVE) {
            // Check airplane mode before live dial
            if (deviceState.isAirplaneModeOn()) {
                eventLog.logEvent(
                    type = SosEventType.WARN_AIRPLANE_MODE,
                    mode = mode,
                    contactName = contact.name,
                    deviceLocked = deviceState.isDeviceLocked(),
                    screenInteractive = deviceState.isScreenInteractive(),
                    detail = "Airplane mode is enabled; outgoing call may fail."
                )
            }

            val callResult = callRequester.requestCall(
                phoneNumber = contact.number,
                isTestMode = false,
                startCallOnSpeaker = startCallOnSpeaker
            )

            when (callResult) {
                is CallRequester.CallResult.PlacedDirectly -> {
                    eventLog.logEvent(
                        type = SosEventType.DIAL_REQUESTED,
                        mode = mode,
                        contactName = contact.name,
                        deviceLocked = deviceState.isDeviceLocked(),
                        screenInteractive = deviceState.isScreenInteractive(),
                        detail = "Placed via TelecomManager."
                    )
                    _state.value = SosState.Finished(
                        targetContact = contact,
                        mode = mode,
                        reason = "Live dial requested via TelecomManager."
                    )
                    postBackupContactsNotification(contact)
                }
                is CallRequester.CallResult.DialFallback -> {
                    eventLog.logEvent(
                        type = SosEventType.DIAL_REQUESTED,
                        mode = mode,
                        contactName = contact.name,
                        deviceLocked = deviceState.isDeviceLocked(),
                        screenInteractive = deviceState.isScreenInteractive(),
                        detail = "Placed via Intent.ACTION_CALL fallback."
                    )
                    _state.value = SosState.Finished(
                        targetContact = contact,
                        mode = mode,
                        reason = "Placed via fallback: ${callResult.reason}"
                    )
                    postBackupContactsNotification(contact)
                }
                is CallRequester.CallResult.Failed -> {
                    eventLog.logEvent(
                        type = SosEventType.DIAL_FAILED,
                        mode = mode,
                        contactName = contact.name,
                        deviceLocked = deviceState.isDeviceLocked(),
                        screenInteractive = deviceState.isScreenInteractive(),
                        detail = "Dial failed: ${callResult.error}"
                    )
                    _state.value = SosState.Finished(
                        targetContact = contact,
                        mode = mode,
                        reason = "Call failed: ${callResult.error}"
                    )
                    // Post Fallback 2 notification
                    notifier.showDialFailedFallback(contact)
                }
            }
        } else {
            // TEST mode: zero actual phone calls placed
            eventLog.logEvent(
                type = SosEventType.TEST_NO_CALL,
                mode = mode,
                contactName = contact.name,
                deviceLocked = deviceState.isDeviceLocked(),
                screenInteractive = deviceState.isScreenInteractive(),
                detail = "Safe test simulation for ${contact.name}."
            )
            delay(2000L)
            _state.value = SosState.Finished(
                targetContact = contact,
                mode = mode,
                reason = "Safe Test Simulation Complete. No real call made."
            )
        }

        delay(3500L)
        _state.value = SosState.Idle
        notifier.dismissSosNotification()
    }

    private suspend fun postBackupContactsNotification(primaryContact: SosContact) {
        val allContacts = contactRepository.getAllContacts()
        val backupContacts = allContacts
            .filter { it.id != primaryContact.id }
            .take(3)
        if (backupContacts.isNotEmpty()) {
            notifier.showBackupContactsNotification(backupContacts)
        }
    }

    fun dialContactById(contactId: Long) {
        flowJob?.cancel()
        flowJob = scope.launch {
            val allContacts = contactRepository.getAllContacts()
            val contact = allContacts.find { it.id == contactId } ?: return@launch
            val config = settingsRepository.configSnapshot.value
            executeCall(contact, config.mode, config.startCallOnSpeaker)
        }
    }

    fun cancelSos() {
        val current = _state.value
        // Only valid to cancel during Countdown
        if (current !is SosState.Countdown) {
            return
        }

        flowJob?.cancel()
        val contactName = current.targetContact.name
        val currentMode = current.mode

        _state.value = SosState.Idle
        haptics.countdownCancelled()
        notifier.dismissSosNotification()

        scope.launch {
            eventLog.logEvent(
                type = SosEventType.CANCELLED,
                mode = currentMode,
                contactName = contactName,
                deviceLocked = deviceState.isDeviceLocked(),
                screenInteractive = deviceState.isScreenInteractive(),
                detail = "Countdown cancelled by user."
            )
        }
    }
}
