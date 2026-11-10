package com.aros.app

import android.Manifest
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aros.app.feature.adaptive.domain.SovereigntyScoreBreakdown
import com.aros.app.feature.ledger.domain.DailyLedgerSummary
import com.aros.app.feature.setup.SetupWizardScreen
import com.aros.app.feature.sos.domain.OverallReadinessStatus
import com.aros.app.feature.sos.domain.SosState
import com.aros.app.feature.sos.platform.ReadinessWorker
import com.aros.app.feature.sos.platform.SimAccountInfo
import com.aros.app.feature.sos.platform.SosScreenLauncherImpl
import com.aros.app.feature.sos.ui.SosCountdownScreen
import com.aros.app.shell.AppNav
import com.aros.app.ui.theme.ArOsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var launchedFromLockScreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if launched specifically for SOS
        val showSos = intent?.getBooleanExtra(SosScreenLauncherImpl.EXTRA_SHOW_SOS, false) ?: false
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLocked = keyguardManager?.isKeyguardLocked ?: false
        if (showSos && isLocked) {
            launchedFromLockScreen = true
        }

        // Schedule 15-minute background readiness audit
        ReadinessWorker.schedule(this)

        val app = application as ArOsApplication
        val container = app.container

        setContent {
            ArOsTheme {
                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current

                // SOS States
                val sosState by container.sosController.state.collectAsState()
                val allContacts by container.contactRepository.getAllContactsFlow().collectAsState(initial = emptyList())
                val primaryContact by container.contactRepository.getPrimaryContactFlow().collectAsState(initial = null)
                val sosConfig by container.settingsRepository.configSnapshot.collectAsState()
                val telemetry by container.volumePressDetector.telemetry.collectAsState()
                val sosEvents by container.eventLog.eventsFlow.collectAsState(initial = emptyList())
                val readinessReport by container.readinessChecker.readinessReportFlow.collectAsState(initial = null)

                // Query SIMs
                var availableSims by remember { mutableStateOf<List<SimAccountInfo>>(emptyList()) }
                LaunchedEffect(Unit) {
                    availableSims = container.simManager.getAvailableSims()
                }

                // Check if wizard should be shown on launch (first run or uncompleted setup)
                var showWizard by remember { mutableStateOf(!sosConfig.setupCompleted) }

                // Manage window flags dynamically based on active SOS state
                LaunchedEffect(sosState) {
                    if (sosState !is SosState.Idle) {
                        setWindowShowWhenLocked(true)
                    } else {
                        setWindowShowWhenLocked(false)
                        if (launchedFromLockScreen) {
                            launchedFromLockScreen = false
                            finishAndRemoveTask()
                        }
                    }
                }

                // Routine States
                val routines by container.routineController.routines.collectAsState(initial = emptyList())

                // Focus States
                val focusTimerState by container.focusController.timerState.collectAsState()
                val todayFocusMinutesNullable by container.focusController.todayFocusMinutes.collectAsState(initial = 0)
                val todayFocusMinutes = todayFocusMinutesNullable ?: 0

                // Guardian States
                val latestGuardianScan by container.guardianScanner.latestScanFlow.collectAsState(initial = null)

                // Activity Ledger States
                val dailyLedgerSummary by container.ledgerRepository.dailySummaryFlow.collectAsState(
                    initial = DailyLedgerSummary()
                )
                val ledgerEntries by container.ledgerRepository.recentEntries.collectAsState(initial = emptyList())

                // Discipline Rules
                val disciplineRules by container.disciplineGatekeeper.rules.collectAsState(initial = emptyList())

                // Adaptive Insights & Sovereignty Breakdown
                val adaptiveBreakdown by container.adaptiveEngine.scoreBreakdown.collectAsState()
                val adaptiveInsights by container.adaptiveEngine.visibleInsights.collectAsState(initial = emptyList())

                // Dynamic Lock Screen Observer using BroadcastReceiver for real-time unlock detection
                var isDeviceLocked by remember { mutableStateOf(keyguardManager?.isKeyguardLocked ?: false) }

                DisposableEffect(Unit) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(c: Context?, intent: Intent?) {
                            isDeviceLocked = keyguardManager?.isKeyguardLocked ?: false
                        }
                    }
                    val filter = IntentFilter().apply {
                        addAction(Intent.ACTION_SCREEN_ON)
                        addAction(Intent.ACTION_SCREEN_OFF)
                        addAction(Intent.ACTION_USER_PRESENT)
                    }
                    context.registerReceiver(receiver, filter)

                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            isDeviceLocked = keyguardManager?.isKeyguardLocked ?: false
                            availableSims = container.simManager.getAvailableSims()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)

                    onDispose {
                        context.unregisterReceiver(receiver)
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when {
                            // Active SOS sequence (Countdown, Dialing, or Finished) takes full priority
                            sosState !is SosState.Idle -> {
                                SosCountdownScreen(
                                    state = sosState,
                                    onCancel = { container.sosController.cancelSos() },
                                    onCallNow = { container.sosController.callNow() }
                                )
                            }

                            // Strict Lock-Screen Privacy Rule:
                            // When the keyguard is locked, shield all personal data, routines, contacts, and logs!
                            isDeviceLocked -> {
                                LockedPrivacyShieldScreen(
                                    onUnlockRequested = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            keyguardManager?.requestDismissKeyguard(
                                                this@MainActivity,
                                                object : KeyguardManager.KeyguardDismissCallback() {
                                                    override fun onDismissSucceeded() {
                                                        super.onDismissSucceeded()
                                                        isDeviceLocked = false
                                                    }
                                                }
                                            )
                                        }
                                    }
                                )
                            }

                            // Full-screen Guided Setup Wizard on first launch or missing permissions
                            showWizard -> {
                                SetupWizardScreen(
                                    config = sosConfig,
                                    readinessReport = readinessReport,
                                    primaryContact = primaryContact,
                                    availableSims = availableSims,
                                    telemetry = telemetry,
                                    onCompleteSetup = {
                                        coroutineScope.launch {
                                            container.settingsRepository.setSetupCompleted(true)
                                            showWizard = false
                                        }
                                    },
                                    onOpenSimPicker = { /* Handled in step */ },
                                    onSelectSim = { simId ->
                                        coroutineScope.launch {
                                            container.settingsRepository.setSelectedSimId(simId)
                                        }
                                    },
                                    onSetMode = { mode ->
                                        coroutineScope.launch {
                                            container.settingsRepository.setMode(mode)
                                        }
                                    },
                                    onOpenContactsPicker = {
                                        // Pick contact intent
                                        val pickIntent = Intent(
                                            Intent.ACTION_PICK,
                                            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                                        )
                                        startActivity(pickIntent)
                                    },
                                    onDismissWizard = { showWizard = false }
                                )
                            }

                            // Device is unlocked: Render the complete AR/OS 5-tab shell
                            else -> {
                                AppNav(
                                    readinessReport = readinessReport,
                                    primaryContact = primaryContact,
                                    allContacts = allContacts,
                                    sosConfig = sosConfig,
                                    telemetry = telemetry,
                                    sosEvents = sosEvents,
                                    availableSims = availableSims,
                                    onSimulateSosTrigger = { container.sosController.onTrigger() },
                                    onPlaceRealTestCallNow = {
                                        if (primaryContact != null) {
                                            container.sosController.dialContactById(primaryContact!!.id)
                                        }
                                    },
                                    onOpenWizard = { showWizard = true },
                                    onAddContact = { name, number, role ->
                                        coroutineScope.launch {
                                            container.contactRepository.addContact(name, number, role)
                                        }
                                    },
                                    onSetPrimaryContact = { id ->
                                        coroutineScope.launch {
                                            container.contactRepository.setPrimaryContact(id)
                                        }
                                    },
                                    onDeleteContact = { id ->
                                        coroutineScope.launch {
                                            container.contactRepository.deleteContact(id)
                                        }
                                    },
                                    onSetSosMode = { mode ->
                                        coroutineScope.launch {
                                            container.settingsRepository.setMode(mode)
                                        }
                                    },
                                    onSetCountdownSeconds = { seconds ->
                                        coroutineScope.launch {
                                            container.settingsRepository.setCountdownSeconds(seconds)
                                        }
                                    },
                                    onSetSensitivityPreset = { preset ->
                                        coroutineScope.launch {
                                            container.settingsRepository.setSensitivityPreset(preset)
                                            container.volumePressDetector.setSensitivity(preset)
                                        }
                                    },
                                    onSetRestoreVolumeAfterSos = { restore ->
                                        coroutineScope.launch {
                                            container.settingsRepository.setRestoreVolumeAfterSos(restore)
                                        }
                                    },
                                    onSetStartCallOnSpeaker = { speaker ->
                                        coroutineScope.launch {
                                            container.settingsRepository.setStartCallOnSpeaker(speaker)
                                        }
                                    },
                                    onSetShowStatusNotification = { show ->
                                        coroutineScope.launch {
                                            container.settingsRepository.setShowStatusNotification(show)
                                        }
                                    },
                                    onSetUse24HourTime = { use24H ->
                                        coroutineScope.launch {
                                            container.settingsRepository.setUse24HourTime(use24H)
                                        }
                                    },
                                    onResetTelemetry = { container.volumePressDetector.resetSequence() },
                                    onClearCooldown = { container.volumePressDetector.clearCooldown() },
                                    onClearEventLog = {
                                        coroutineScope.launch {
                                            container.eventLog.clearLog()
                                        }
                                    },
                                    routines = routines,
                                    onToggleRoutine = { routine ->
                                        coroutineScope.launch {
                                            container.routineController.toggleRoutineCompletion(routine)
                                        }
                                    },
                                    onAddRoutine = { title, desc, time, duration, category ->
                                        coroutineScope.launch {
                                            container.routineController.addRoutine(title, desc, time, duration, category)
                                        }
                                    },
                                    onDeleteRoutine = { id ->
                                        coroutineScope.launch {
                                            container.routineController.deleteRoutine(id)
                                        }
                                    },
                                    focusTimerState = focusTimerState,
                                    todayFocusMinutes = todayFocusMinutes,
                                    onSelectFocusMode = { mode -> container.focusController.selectMode(mode) },
                                    onStartFocus = { container.focusController.start() },
                                    onPauseFocus = { container.focusController.pause() },
                                    onResumeFocus = { container.focusController.resume() },
                                    onCancelFocus = { container.focusController.cancel() },
                                    onLogFocusInterruption = { container.focusController.recordManualInterruption() },
                                    latestGuardianScan = latestGuardianScan,
                                    onRunGuardianAudit = {
                                        coroutineScope.launch {
                                            container.guardianScanner.performAudit()
                                        }
                                    },
                                    dailyLedgerSummary = dailyLedgerSummary,
                                    ledgerEntries = ledgerEntries,
                                    onLogIntentionalLedgerEvent = { label, duration ->
                                        coroutineScope.launch {
                                            container.ledgerRepository.logIntentionalEvent(label, duration)
                                        }
                                    },
                                    disciplineRules = disciplineRules,
                                    onToggleDisciplineRule = { rule ->
                                        coroutineScope.launch {
                                            container.disciplineGatekeeper.toggleRuleActive(rule)
                                        }
                                    },
                                    onAddDisciplineRule = { name, type, friction, target ->
                                        coroutineScope.launch {
                                            container.disciplineGatekeeper.addRule(name, type, friction, target)
                                        }
                                    },
                                    onDeleteDisciplineRule = { id ->
                                        coroutineScope.launch {
                                            container.disciplineGatekeeper.deleteRule(id)
                                        }
                                    },
                                    onRecordDisciplinePass = { id ->
                                        coroutineScope.launch {
                                            container.disciplineGatekeeper.recordSuccessfulChallengePass(id)
                                        }
                                    },
                                    sovereigntyBreakdown = adaptiveBreakdown,
                                    adaptiveInsights = adaptiveInsights,
                                    onDismissAdaptiveInsight = { id ->
                                        coroutineScope.launch {
                                            container.adaptiveEngine.dismissInsight(id)
                                        }
                                    },
                                    guardRepository = container.guardRepository,
                                    guardEngine = container.guardEngine
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val showSos = intent.getBooleanExtra(SosScreenLauncherImpl.EXTRA_SHOW_SOS, false)
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLocked = keyguardManager?.isKeyguardLocked ?: false
        if (showSos && isLocked) {
            launchedFromLockScreen = true
            setWindowShowWhenLocked(true)
        }
    }

    private fun setWindowShowWhenLocked(show: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(show)
            setTurnScreenOn(show)
        } else {
            @Suppress("DEPRECATION")
            if (show) {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            } else {
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
        }
        if (show) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Composable
fun LockedPrivacyShieldScreen(onUnlockRequested: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF1E293B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Device Locked",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "AR/OS PRIVACY SHIELD",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Device is locked. All routines, focus sessions, and contacts are shielded from view.\n\nFive rapid Volume Down presses will trigger AR SOS.",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onUnlockRequested,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Unlock Phone to Open AR/OS", fontWeight = FontWeight.Bold)
            }
        }
    }
}
