package com.aros.app.core

import android.content.Context
import com.aros.app.feature.adaptive.domain.AdaptiveEngine
import com.aros.app.feature.discipline.domain.DisciplineGatekeeper
import com.aros.app.feature.focus.domain.FocusController
import com.aros.app.feature.guardian.domain.GuardianScanner
import com.aros.app.feature.ledger.domain.LedgerRepository
import com.aros.app.feature.routine.domain.RoutineController
import com.aros.app.feature.sos.data.AppDatabase
import com.aros.app.feature.sos.data.ContactRepositoryImpl
import com.aros.app.feature.sos.data.SettingsRepositoryImpl
import com.aros.app.feature.sos.data.SosEventLogImpl
import com.aros.app.feature.sos.domain.CallRequester
import com.aros.app.feature.sos.domain.ContactRepository
import com.aros.app.feature.sos.domain.DeviceStateProvider
import com.aros.app.feature.sos.domain.Haptics
import com.aros.app.feature.sos.domain.ReadinessChecker
import com.aros.app.feature.sos.domain.SettingsRepository
import com.aros.app.feature.sos.domain.SosController
import com.aros.app.feature.sos.domain.SosEventLog
import com.aros.app.feature.sos.domain.SosNotifier
import com.aros.app.feature.sos.domain.SosScreenLauncher
import com.aros.app.feature.sos.domain.VolumePressDetector
import com.aros.app.feature.sos.platform.AndroidHaptics
import com.aros.app.feature.sos.platform.DeviceStateProviderImpl
import com.aros.app.feature.sos.platform.ReadinessCheckerImpl
import com.aros.app.feature.sos.platform.SosNotifications
import com.aros.app.feature.sos.platform.SosScreenLauncherImpl
import com.aros.app.feature.sos.platform.TelecomCallRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(val context: Context) {
    val clock: Clock = Clock.System
    val dispatchers: DispatchersProvider = DispatchersProvider.Default

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val database: AppDatabase = AppDatabase.getInstance(context)

    // SOS Module
    val contactRepository: ContactRepository = ContactRepositoryImpl(database.contactDao())
    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(context, applicationScope)
    val eventLog: SosEventLog = SosEventLogImpl(database.eventDao(), dispatchers.io)

    val simManager: com.aros.app.feature.sos.platform.SimManager = com.aros.app.feature.sos.platform.SimManagerImpl(context)

    val callRequester: CallRequester = TelecomCallRequester(context, simManager)
    val haptics: Haptics = AndroidHaptics(context)
    val notifier: SosNotifier = SosNotifications(context)
    val deviceState: DeviceStateProvider = DeviceStateProviderImpl(context)
    val screenLauncher: SosScreenLauncher = SosScreenLauncherImpl(context)

    val readinessChecker: ReadinessChecker = ReadinessCheckerImpl(
        context = context,
        contactRepository = contactRepository,
        settingsRepository = settingsRepository,
        simManager = simManager
    )

    val volumePressDetector: VolumePressDetector = VolumePressDetector()

    init {
        applicationScope.launch {
            settingsRepository.configSnapshot.collect { config ->
                volumePressDetector.setSensitivity(config.sensitivityPreset)
            }
        }
    }

    val sosController: SosController = SosController(
        scope = applicationScope,
        clock = clock,
        contactRepository = contactRepository,
        settingsRepository = settingsRepository,
        callRequester = callRequester,
        haptics = haptics,
        notifier = notifier,
        eventLog = eventLog,
        deviceState = deviceState,
        screenLauncher = screenLauncher
    )

    // Routine Engine
    val routineController: RoutineController = RoutineController(
        dao = database.routineDao(),
        scope = applicationScope
    )

    // Focus Engine
    val focusController: FocusController = FocusController(
        dao = database.focusDao(),
        scope = applicationScope
    )

    // Digital Guardian
    val guardianScanner: GuardianScanner = GuardianScanner(
        context = context,
        dao = database.guardianDao(),
        ioDispatcher = dispatchers.io
    )

    // Activity Ledger
    val ledgerRepository: LedgerRepository = LedgerRepository(
        dao = database.ledgerDao(),
        ioDispatcher = dispatchers.io,
        scope = applicationScope
    )

    // Discipline Engine
    val disciplineGatekeeper: DisciplineGatekeeper = DisciplineGatekeeper(
        dao = database.disciplineDao(),
        scope = applicationScope
    )

    // Adaptive Intelligence
    val adaptiveEngine: AdaptiveEngine = AdaptiveEngine(
        dao = database.adaptiveDao(),
        ioDispatcher = dispatchers.io,
        scope = applicationScope
    )
}
