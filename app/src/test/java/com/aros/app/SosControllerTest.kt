package com.aros.app

import com.aros.app.core.Clock
import com.aros.app.feature.sos.domain.CallRequester
import com.aros.app.feature.sos.domain.ContactRepository
import com.aros.app.feature.sos.domain.ContactRole
import com.aros.app.feature.sos.domain.DeviceStateProvider
import com.aros.app.feature.sos.domain.Haptics
import com.aros.app.feature.sos.domain.OverallReadinessStatus
import com.aros.app.feature.sos.domain.SettingsRepository
import com.aros.app.feature.sos.domain.SosConfig
import com.aros.app.feature.sos.domain.SosContact
import com.aros.app.feature.sos.domain.SosController
import com.aros.app.feature.sos.domain.SosEvent
import com.aros.app.feature.sos.domain.SosEventLog
import com.aros.app.feature.sos.domain.SosEventType
import com.aros.app.feature.sos.domain.SosMode
import com.aros.app.feature.sos.domain.SosNotifier
import com.aros.app.feature.sos.domain.SosScreenLauncher
import com.aros.app.feature.sos.domain.SosState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SosControllerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val primaryContact = SosContact(
        id = 1L,
        name = "Alice Guardian",
        number = "+15551234567",
        role = ContactRole.PRIMARY
    )

    private val backupContact = SosContact(
        id = 2L,
        name = "Bob Backup",
        number = "+15557654321",
        role = ContactRole.BACKUP
    )

    private val fakeContactRepository = object : ContactRepository {
        var contacts = mutableListOf(primaryContact, backupContact)
        override fun getAllContactsFlow(): Flow<List<SosContact>> = flowOf(contacts)
        override suspend fun getAllContacts(): List<SosContact> = contacts
        override fun getPrimaryContactFlow(): Flow<SosContact?> = flowOf(contacts.find { it.role == ContactRole.PRIMARY })
        override suspend fun getPrimaryContact(): SosContact? = contacts.find { it.role == ContactRole.PRIMARY }
        override suspend fun addContact(name: String, number: String, role: ContactRole): Result<Long> = Result.success(3L)
        override suspend fun updateContact(contact: SosContact): Result<Unit> = Result.success(Unit)
        override suspend fun deleteContact(id: Long): Result<Unit> = Result.success(Unit)
        override suspend fun setPrimaryContact(id: Long): Result<Unit> = Result.success(Unit)
    }

    private val fakeConfig = MutableStateFlow(
        SosConfig(
            mode = SosMode.TEST,
            countdownSeconds = 5,
            sensitivityPreset = "Normal",
            restoreVolumeAfterSos = true,
            startCallOnSpeaker = false,
            showStatusNotification = true
        )
    )

    private val fakeSettingsRepository = object : SettingsRepository {
        override val configFlow: Flow<SosConfig> = fakeConfig
        override val configSnapshot: StateFlow<SosConfig> = fakeConfig.asStateFlow()
        override suspend fun setMode(mode: SosMode) { fakeConfig.value = fakeConfig.value.copy(mode = mode) }
        override suspend fun setCountdownSeconds(seconds: Int) { fakeConfig.value = fakeConfig.value.copy(countdownSeconds = seconds) }
        override suspend fun setSensitivityPreset(preset: String) { fakeConfig.value = fakeConfig.value.copy(sensitivityPreset = preset) }
        override suspend fun setRestoreVolumeAfterSos(restore: Boolean) { fakeConfig.value = fakeConfig.value.copy(restoreVolumeAfterSos = restore) }
        override suspend fun setStartCallOnSpeaker(speaker: Boolean) { fakeConfig.value = fakeConfig.value.copy(startCallOnSpeaker = speaker) }
        override suspend fun setShowStatusNotification(show: Boolean) { fakeConfig.value = fakeConfig.value.copy(showStatusNotification = show) }
        override suspend fun setSelectedSimId(simId: String?) { fakeConfig.value = fakeConfig.value.copy(selectedSimId = simId) }
        override suspend fun setSetupCompleted(completed: Boolean) { fakeConfig.value = fakeConfig.value.copy(setupCompleted = completed) }
        override suspend fun setUse24HourTime(use24Hour: Boolean) { fakeConfig.value = fakeConfig.value.copy(use24HourTime = use24Hour) }
    }

    val loggedEvents = mutableListOf<SosEvent>()
    private val fakeEventLog = object : SosEventLog {
        override val eventsFlow: Flow<List<SosEvent>> = flowOf(loggedEvents)
        override suspend fun logEvent(
            type: SosEventType,
            mode: SosMode,
            contactName: String?,
            deviceLocked: Boolean,
            screenInteractive: Boolean,
            detail: String
        ) {
            loggedEvents.add(
                SosEvent(
                    id = loggedEvents.size + 1L,
                    timestamp = System.currentTimeMillis(),
                    type = type,
                    mode = mode,
                    contactName = contactName,
                    deviceLocked = deviceLocked,
                    screenInteractive = screenInteractive,
                    detail = detail
                )
            )
        }
        override suspend fun clearLog() { loggedEvents.clear() }
    }

    var callRequestedCount = 0
    var lastRequestedNumber: String? = null
    var lastSpeakerSetting: Boolean? = null
    private val fakeCallRequester = object : CallRequester {
        override suspend fun requestCall(
            phoneNumber: String,
            isTestMode: Boolean,
            startCallOnSpeaker: Boolean
        ): CallRequester.CallResult {
            callRequestedCount++
            lastRequestedNumber = phoneNumber
            lastSpeakerSetting = startCallOnSpeaker
            return CallRequester.CallResult.PlacedDirectly
        }
    }

    var hapticsTriggerCount = 0
    var hapticsCancelledCount = 0
    private val fakeHaptics = object : Haptics {
        override fun triggerDetected() { hapticsTriggerCount++ }
        override fun countdownTick(remainingSeconds: Int) {}
        override fun countdownCancelled() { hapticsCancelledCount++ }
        override fun callInitiating() {}
    }

    private val fakeNotifier = object : SosNotifier {
        override fun showOngoingNotification(state: SosState.Countdown) {}
        override fun showCallingNotification(state: SosState.Dialing) {}
        override fun showDialFailedFallback(targetContact: SosContact) {}
        override fun showBackupContactsNotification(backupContacts: List<SosContact>) {}
        override fun showStatusNotification(mode: SosMode, readiness: OverallReadinessStatus) {}
        override fun dismissStatusNotification() {}
        override fun showReadinessWarning(issues: List<String>) {}
        override fun dismissSosNotification() {}
    }

    private val fakeDeviceState = object : DeviceStateProvider {
        override fun isDeviceLocked(): Boolean = false
        override fun isScreenInteractive(): Boolean = true
        override fun isAirplaneModeOn(): Boolean = false
        override fun hasSimCard(): Boolean = true
    }

    private val fakeScreenLauncher = object : SosScreenLauncher {
        override fun launchSosCountdown() {}
        override fun bringAppToFront() {}
    }

    private var testElapsedRealtime = 100000L
    private val fakeClock = object : Clock {
        override fun currentTimeMillis(): Long = System.currentTimeMillis()
        override fun elapsedRealtime(): Long = testElapsedRealtime
    }

    private lateinit var controller: SosController

    @Before
    fun setup() {
        testElapsedRealtime = 100000L
        loggedEvents.clear()
        callRequestedCount = 0
        lastRequestedNumber = null
        lastSpeakerSetting = null
        hapticsTriggerCount = 0
        hapticsCancelledCount = 0
        fakeConfig.value = fakeConfig.value.copy(mode = SosMode.TEST, countdownSeconds = 5)

        controller = SosController(
            scope = testScope,
            clock = fakeClock,
            contactRepository = fakeContactRepository,
            settingsRepository = fakeSettingsRepository,
            callRequester = fakeCallRequester,
            haptics = fakeHaptics,
            notifier = fakeNotifier,
            eventLog = fakeEventLog,
            deviceState = fakeDeviceState,
            screenLauncher = fakeScreenLauncher
        )
    }

    @Test
    fun testU11_TriggerStartsCountdown() = testScope.runTest {
        controller.onTrigger()
        testScheduler.advanceTimeBy(100L) // Advance initial launch without running the full 5s loop

        assertTrue(controller.state.value is SosState.Countdown)
        val countdown = controller.state.value as SosState.Countdown
        assertEquals(5, countdown.remainingSeconds)
        assertEquals(primaryContact.name, countdown.targetContact.name)
        assertEquals(1, hapticsTriggerCount)

        // Verify privacy: phone number must NOT appear in any event log details
        for (event in loggedEvents) {
            assertFalse(
                "Phone number leaked in log detail: ${event.detail}",
                event.detail.contains(primaryContact.number)
            )
        }

        controller.cancelSos()
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun testU11_CancelDuringCountdownResetsToIdle() = testScope.runTest {
        controller.onTrigger()
        testScheduler.advanceTimeBy(100L)

        assertTrue(controller.state.value is SosState.Countdown)

        controller.cancelSos()
        testScheduler.advanceUntilIdle()

        assertEquals(SosState.Idle, controller.state.value)
        assertEquals(1, hapticsCancelledCount)
        assertTrue(loggedEvents.any { it.type == SosEventType.CANCELLED })
    }

    @Test
    fun testU11_CancelIgnoredDuringDialing() = testScope.runTest {
        controller.onTrigger()
        testScheduler.advanceTimeBy(100L)

        // Call now to enter dialing immediately
        controller.callNow()
        testScheduler.advanceTimeBy(200L)

        // State is now Dialing or Finished
        val stateDuringDial = controller.state.value
        assertTrue(stateDuringDial is SosState.Dialing || stateDuringDial is SosState.Finished)

        val hapticsBeforeCancel = hapticsCancelledCount
        // Attempting to cancel during Dialing/Finished must be ignored
        controller.cancelSos()
        testScheduler.advanceTimeBy(200L)

        assertEquals(
            "Cancel during dialing/finished must be ignored",
            hapticsBeforeCancel,
            hapticsCancelledCount
        )

        testScheduler.advanceUntilIdle()
    }

    @Test
    fun testU11_CallNowJumpsToDialing() = testScope.runTest {
        fakeConfig.value = fakeConfig.value.copy(mode = SosMode.LIVE)

        controller.onTrigger()
        testScheduler.advanceTimeBy(100L)
        assertTrue(controller.state.value is SosState.Countdown)

        controller.callNow()
        testScheduler.advanceUntilIdle()

        // In LIVE mode, callRequester is invoked directly
        assertEquals(1, callRequestedCount)
        assertEquals(primaryContact.number, lastRequestedNumber)
    }

    @Test
    fun testU11_ZeroSecondCountdownDirectDial() = testScope.runTest {
        fakeConfig.value = fakeConfig.value.copy(mode = SosMode.LIVE, countdownSeconds = 0)

        controller.onTrigger()
        testScheduler.advanceUntilIdle()

        // Should directly execute call without waiting in countdown
        assertEquals(1, callRequestedCount)
        assertEquals(primaryContact.number, lastRequestedNumber)
    }
}
