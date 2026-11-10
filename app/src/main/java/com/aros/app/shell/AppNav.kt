package com.aros.app.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aros.app.designsystem.components.ArOsBottomBar
import com.aros.app.designsystem.components.ArOsNavDestination
import com.aros.app.designsystem.gallery.ArOsDesignGallery
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.adaptive.data.AdaptiveInsightEntity
import com.aros.app.feature.adaptive.domain.SovereigntyScoreBreakdown
import com.aros.app.feature.adaptive.ui.AdaptiveScreen
import com.aros.app.feature.discipline.domain.DisciplineChallengeType
import com.aros.app.feature.discipline.domain.DisciplineRule
import com.aros.app.feature.discipline.ui.DisciplineScreen
import com.aros.app.feature.focus.domain.FocusMode
import com.aros.app.feature.focus.domain.FocusTimerState
import com.aros.app.feature.focus.ui.FocusScreen
import com.aros.app.feature.guardian.data.GuardianScanEntity
import com.aros.app.feature.guardian.ui.GuardianScreen
import com.aros.app.feature.ledger.data.ActivityLedgerEntity
import com.aros.app.feature.ledger.domain.DailyLedgerSummary
import com.aros.app.feature.ledger.ui.ActivityLedgerScreen
import com.aros.app.feature.routine.domain.RoutineCategory
import com.aros.app.feature.routine.domain.RoutineItem
import com.aros.app.feature.routine.ui.RoutineScreen
import com.aros.app.feature.setup.PermissionCenterScreen
import com.aros.app.feature.setup.WhatArOsCanDoScreen
import com.aros.app.feature.sos.domain.ContactRole
import com.aros.app.feature.sos.domain.ReadinessAction
import com.aros.app.feature.sos.domain.ReadinessReport
import com.aros.app.feature.sos.domain.SosConfig
import com.aros.app.feature.sos.domain.SosContact
import com.aros.app.feature.sos.domain.SosEvent
import com.aros.app.feature.sos.domain.SosMode
import com.aros.app.feature.sos.domain.TriggerTelemetry
import com.aros.app.feature.sos.platform.SimAccountInfo
import com.aros.app.feature.sos.ui.ContactsScreen
import com.aros.app.feature.sos.ui.DashboardScreen
import com.aros.app.feature.sos.ui.EventLogScreen
import com.aros.app.feature.sos.ui.HelpLimitsScreen
import com.aros.app.feature.sos.ui.SafetyHubScreen
import com.aros.app.feature.sos.ui.SettingsScreen
import com.aros.app.feature.sos.ui.TriggerLabScreen

enum class AppDestination(val label: String) {
    HOME("Home"),
    SAFETY("Safety"),
    ROUTINES("Rhythms"),
    FOCUS("Focus"),
    MORE("More"),
    PERMISSIONS("Permissions"),
    WHAT_AR_OS_CAN_DO("Capabilities"),
    LEDGER("Activity Ledger"),
    GUARDIAN("Digital Guardian"),
    DISCIPLINE("Focus Rules"),
    ADAPTIVE("Insights"),
    SOS_CONTACTS("Contacts"),
    SOS_SETTINGS("Settings"),
    SOS_LAB("Trigger Lab"),
    SOS_LOG("SOS History"),
    SOS_HELP("Safety Limits"),
    GALLERY("Design Gallery")
}

@Composable
fun AppNav(
    // SOS State & Callbacks
    readinessReport: ReadinessReport?,
    primaryContact: SosContact?,
    allContacts: List<SosContact>,
    sosConfig: SosConfig,
    telemetry: TriggerTelemetry,
    sosEvents: List<SosEvent>,
    availableSims: List<SimAccountInfo>,
    onSimulateSosTrigger: () -> Unit,
    onPlaceRealTestCallNow: () -> Unit,
    onOpenWizard: () -> Unit,
    onAddContact: (String, String, ContactRole) -> Unit,
    onSetPrimaryContact: (Long) -> Unit,
    onDeleteContact: (Long) -> Unit,
    onSetSosMode: (SosMode) -> Unit,
    onSetCountdownSeconds: (Int) -> Unit,
    onSetSensitivityPreset: (String) -> Unit,
    onSetRestoreVolumeAfterSos: (Boolean) -> Unit,
    onSetStartCallOnSpeaker: (Boolean) -> Unit,
    onSetShowStatusNotification: (Boolean) -> Unit,
    onSetUse24HourTime: (Boolean) -> Unit,
    onResetTelemetry: () -> Unit,
    onClearCooldown: () -> Unit,
    onClearEventLog: () -> Unit,

    // Routine Engine
    routines: List<RoutineItem>,
    onToggleRoutine: (RoutineItem) -> Unit,
    onAddRoutine: (String, String, Int, Int, RoutineCategory) -> Unit,
    onDeleteRoutine: (Long) -> Unit,

    // Focus Engine
    focusTimerState: FocusTimerState,
    todayFocusMinutes: Int,
    onSelectFocusMode: (FocusMode) -> Unit,
    onStartFocus: () -> Unit,
    onPauseFocus: () -> Unit,
    onResumeFocus: () -> Unit,
    onCancelFocus: () -> Unit,
    onLogFocusInterruption: () -> Unit,

    // Guardian
    latestGuardianScan: GuardianScanEntity?,
    onRunGuardianAudit: () -> Unit,

    // Ledger
    dailyLedgerSummary: DailyLedgerSummary,
    ledgerEntries: List<ActivityLedgerEntity>,
    onLogIntentionalLedgerEvent: (String, Long) -> Unit,

    // Discipline
    disciplineRules: List<DisciplineRule>,
    onToggleDisciplineRule: (DisciplineRule) -> Unit,
    onAddDisciplineRule: (String, DisciplineChallengeType, Int, String) -> Unit,
    onDeleteDisciplineRule: (Long) -> Unit,
    onRecordDisciplinePass: (Long) -> Unit,

    // Adaptive
    sovereigntyBreakdown: SovereigntyScoreBreakdown,
    adaptiveInsights: List<AdaptiveInsightEntity>,
    onDismissAdaptiveInsight: (Long) -> Unit
) {
    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }

    // Intercept back presses on secondary destinations
    BackHandler(enabled = currentDestination != AppDestination.HOME) {
        currentDestination = AppDestination.HOME
    }

    val bottomBarDestination = when (currentDestination) {
        AppDestination.HOME -> ArOsNavDestination.Home
        AppDestination.SAFETY -> ArOsNavDestination.Safety
        AppDestination.ROUTINES -> ArOsNavDestination.Rhythms
        AppDestination.FOCUS -> ArOsNavDestination.Focus
        AppDestination.MORE,
        AppDestination.PERMISSIONS,
        AppDestination.WHAT_AR_OS_CAN_DO,
        AppDestination.LEDGER,
        AppDestination.GUARDIAN,
        AppDestination.DISCIPLINE,
        AppDestination.ADAPTIVE,
        AppDestination.SOS_CONTACTS,
        AppDestination.SOS_SETTINGS,
        AppDestination.SOS_LAB,
        AppDestination.SOS_LOG,
        AppDestination.SOS_HELP,
        AppDestination.GALLERY -> ArOsNavDestination.More
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ArOsTheme.colors.background)
    ) {
        // Active Destination Content Area
        Box(modifier = Modifier.weight(1f)) {
            when (currentDestination) {
                AppDestination.HOME -> DashboardScreen(
                    readinessReport = readinessReport,
                    sosConfig = sosConfig,
                    routines = routines,
                    onSimulateSosTrigger = onSimulateSosTrigger,
                    onOpenPermissions = { currentDestination = AppDestination.PERMISSIONS },
                    onOpenWizard = onOpenWizard,
                    onOpenFocus = { currentDestination = AppDestination.FOCUS },
                    onAddRoutine = { currentDestination = AppDestination.ROUTINES },
                    onExploreFeatures = { currentDestination = AppDestination.WHAT_AR_OS_CAN_DO },
                    onToggleRoutine = onToggleRoutine
                )

                AppDestination.SAFETY -> SafetyHubScreen(
                    config = sosConfig,
                    readinessReport = readinessReport,
                    primaryContact = primaryContact,
                    availableSims = availableSims,
                    onSetMode = onSetSosMode,
                    onTestSosNow = onSimulateSosTrigger,
                    onPlaceRealTestCallNow = onPlaceRealTestCallNow,
                    onNavigateTo = { dest -> currentDestination = dest }
                )

                AppDestination.ROUTINES -> RoutineScreen(
                    routines = routines,
                    onToggleCompletion = onToggleRoutine,
                    onAddRoutine = onAddRoutine,
                    onDeleteRoutine = onDeleteRoutine
                )

                AppDestination.FOCUS -> FocusScreen(
                    timerState = focusTimerState,
                    todayFocusMinutes = todayFocusMinutes,
                    onSelectMode = onSelectFocusMode,
                    onStart = onStartFocus,
                    onPause = onPauseFocus,
                    onResume = onResumeFocus,
                    onCancel = onCancelFocus,
                    onLogInterruption = onLogFocusInterruption
                )

                AppDestination.MORE -> MoreHubScreen(
                    onNavigateTo = { dest -> currentDestination = dest }
                )

                AppDestination.PERMISSIONS -> PermissionCenterScreen(
                    readinessReport = readinessReport,
                    onNavigateBack = { currentDestination = AppDestination.HOME }
                )

                AppDestination.WHAT_AR_OS_CAN_DO -> WhatArOsCanDoScreen(
                    onNavigateTo = { dest -> currentDestination = dest },
                    onNavigateBack = { currentDestination = AppDestination.HOME }
                )

                AppDestination.LEDGER -> ActivityLedgerScreen(
                    summary = dailyLedgerSummary,
                    entries = ledgerEntries,
                    onLogIntentionalEvent = onLogIntentionalLedgerEvent
                )

                AppDestination.GUARDIAN -> GuardianScreen(
                    latestScan = latestGuardianScan,
                    onRunAudit = onRunGuardianAudit
                )

                AppDestination.DISCIPLINE -> DisciplineScreen(
                    rules = disciplineRules,
                    onToggleRule = onToggleDisciplineRule,
                    onAddRule = onAddDisciplineRule,
                    onDeleteRule = onDeleteDisciplineRule,
                    onRecordChallengePass = onRecordDisciplinePass
                )

                AppDestination.ADAPTIVE -> AdaptiveScreen(
                    scoreBreakdown = sovereigntyBreakdown,
                    insights = adaptiveInsights,
                    onDismissInsight = onDismissAdaptiveInsight
                )

                AppDestination.SOS_CONTACTS -> ContactsScreen(
                    contacts = allContacts,
                    onAddContact = onAddContact,
                    onSetPrimary = onSetPrimaryContact,
                    onDeleteContact = onDeleteContact
                )

                AppDestination.SOS_SETTINGS -> SettingsScreen(
                    config = sosConfig,
                    onSetMode = onSetSosMode,
                    onSetCountdown = onSetCountdownSeconds,
                    onSetSensitivity = onSetSensitivityPreset,
                    onSetRestoreVolumeAfterSos = onSetRestoreVolumeAfterSos,
                    onSetStartCallOnSpeaker = onSetStartCallOnSpeaker,
                    onSetShowStatusNotification = onSetShowStatusNotification,
                    onSetUse24HourTime = onSetUse24HourTime
                )

                AppDestination.SOS_LAB -> TriggerLabScreen(
                    telemetry = telemetry,
                    onResetTelemetry = onResetTelemetry,
                    onClearCooldown = onClearCooldown
                )

                AppDestination.SOS_LOG -> EventLogScreen(
                    events = sosEvents,
                    onClearLog = onClearEventLog
                )

                AppDestination.SOS_HELP -> HelpLimitsScreen()

                AppDestination.GALLERY -> ArOsDesignGallery(
                    onBack = { currentDestination = AppDestination.HOME }
                )
            }
        }

        // Persistent 5-Item Navigation Bar: Home, Safety, Rhythms, Focus, More
        ArOsBottomBar(
            currentDestination = bottomBarDestination,
            onNavigate = { dest ->
                currentDestination = when (dest) {
                    ArOsNavDestination.Home -> AppDestination.HOME
                    ArOsNavDestination.Safety -> AppDestination.SAFETY
                    ArOsNavDestination.Rhythms -> AppDestination.ROUTINES
                    ArOsNavDestination.Focus -> AppDestination.FOCUS
                    ArOsNavDestination.More -> AppDestination.MORE
                }
            }
        )
    }
}
