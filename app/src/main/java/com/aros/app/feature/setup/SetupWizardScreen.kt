package com.aros.app.feature.setup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aros.app.designsystem.components.ArOsButton
import com.aros.app.designsystem.components.ArOsButtonVariant
import com.aros.app.designsystem.components.ArOsCard
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.sos.domain.ContactRole
import com.aros.app.feature.sos.domain.ReadinessReport
import com.aros.app.feature.sos.domain.ServiceState
import com.aros.app.feature.sos.domain.SosConfig
import com.aros.app.feature.sos.domain.SosContact
import com.aros.app.feature.sos.domain.SosMode
import com.aros.app.feature.sos.domain.TriggerTelemetry
import com.aros.app.feature.sos.platform.SimAccountInfo

@Composable
fun SetupWizardScreen(
    config: SosConfig,
    readinessReport: ReadinessReport?,
    primaryContact: SosContact?,
    availableSims: List<SimAccountInfo>,
    telemetry: TriggerTelemetry,
    onCompleteSetup: () -> Unit,
    onOpenSimPicker: () -> Unit,
    onSelectSim: (String) -> Unit,
    onSetMode: (SosMode) -> Unit,
    onOpenContactsPicker: () -> Unit,
    onDismissWizard: () -> Unit
) {
    val context = LocalContext.current
    var currentStepIndex by remember { mutableIntStateOf(0) }

    // Runtime Permission Launchers
    val phoneCallLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Auto-evaluated in ReadinessChecker */ }

    val phoneStateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Auto-evaluated in ReadinessChecker */ }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Auto-evaluated in ReadinessChecker */ }

    val totalSteps = 10

    // Auto-advance logic on condition satisfaction
    val isCallGranted = readinessReport?.checks?.find { it.id == "call_phone_permission" }?.isPassing == true
    val isSimGranted = readinessReport?.checks?.find { it.id == "sim_card_check" }?.isPassing == true
    val isNotifGranted = readinessReport?.checks?.find { it.id == "notifications_permission" }?.isPassing == true
    val isA11yActive = readinessReport?.checks?.find { it.id == "accessibility_service" }?.isPassing == true
    val isBatteryIgnored = readinessReport?.checks?.find { it.id == "battery_optimization" }?.isPassing == true
    val hasContact = primaryContact != null
    val practiceSuccess = telemetry.currentCount >= 5

    LaunchedEffect(currentStepIndex, isCallGranted, isSimGranted, isNotifGranted, isA11yActive, isBatteryIgnored, hasContact, practiceSuccess) {
        when (currentStepIndex) {
            1 -> if (isCallGranted) currentStepIndex = 2
            2 -> if (isSimGranted) currentStepIndex = 3
            3 -> if (isNotifGranted) currentStepIndex = 4
            4 -> if (isA11yActive) currentStepIndex = 5
            5 -> if (isBatteryIgnored) currentStepIndex = 6
            6 -> if (hasContact) currentStepIndex = 7
            7 -> if (practiceSuccess) currentStepIndex = 8
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArOsTheme.colors.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Step Indicator & Skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step ${currentStepIndex + 1} of $totalSteps",
                    style = ArOsTheme.typography.caption,
                    color = ArOsTheme.colors.contentTertiary
                )
                if (currentStepIndex < 9) {
                    TextButton(onClick = onDismissWizard) {
                        Text(
                            text = "Skip for now",
                            style = ArOsTheme.typography.footnote,
                            color = ArOsTheme.colors.accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body: Animated Step Card
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = currentStepIndex,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "setup_step_transition"
                ) { step ->
                    when (step) {
                        0 -> StepWelcome(onNext = { currentStepIndex = 1 })
                        1 -> StepPermission(
                            icon = Icons.Default.Phone,
                            title = "Phone calls",
                            description = "Needed to call your trusted contact when you press Volume Down five times.",
                            isGranted = isCallGranted,
                            buttonText = "Allow Phone Calls",
                            onAction = { phoneCallLauncher.launch(Manifest.permission.CALL_PHONE) },
                            onNext = { currentStepIndex = 2 }
                        )
                        2 -> StepSimSelection(
                            availableSims = availableSims,
                            selectedSimId = config.selectedSimId,
                            isGranted = isSimGranted,
                            onGrantPermission = { phoneStateLauncher.launch(Manifest.permission.READ_PHONE_STATE) },
                            onSelectSim = onSelectSim,
                            onNext = { currentStepIndex = 3 }
                        )
                        3 -> StepPermission(
                            icon = Icons.Default.Notifications,
                            title = "Status & alerts",
                            description = "Shows the active countdown timer and lets you cancel anytime.",
                            isGranted = isNotifGranted,
                            buttonText = "Allow Notifications",
                            onAction = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    currentStepIndex = 4
                                }
                            },
                            onNext = { currentStepIndex = 4 }
                        )
                        4 -> StepAccessibility(
                            context = context,
                            isActive = isA11yActive,
                            onNext = { currentStepIndex = 5 }
                        )
                        5 -> StepBattery(
                            context = context,
                            isIgnored = isBatteryIgnored,
                            onNext = { currentStepIndex = 6 }
                        )
                        6 -> StepContact(
                            primaryContact = primaryContact,
                            onAddContact = onOpenContactsPicker,
                            onNext = { currentStepIndex = 7 }
                        )
                        7 -> StepPractice(
                            telemetry = telemetry,
                            onNext = { currentStepIndex = 8 }
                        )
                        8 -> StepModeSelection(
                            currentMode = config.mode,
                            contactName = primaryContact?.name ?: "Primary Contact",
                            onSelectMode = onSetMode,
                            onNext = { currentStepIndex = 9 }
                        )
                        9 -> StepSummary(
                            onFinish = onCompleteSetup
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepWelcome(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(ArOsTheme.colors.accent.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = ArOsTheme.colors.accent,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Welcome to AR/OS",
            style = ArOsTheme.typography.title1,
            color = ArOsTheme.colors.contentPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "• Offline emergency calling via 5x Volume Down\n• Mindful daily rhythm schedules\n• Focused work timers with zero distraction\n• 100% private and on-device",
            style = ArOsTheme.typography.body,
            color = ArOsTheme.colors.contentSecondary,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Setup takes about 2 minutes.",
            style = ArOsTheme.typography.footnote,
            color = ArOsTheme.colors.contentTertiary
        )
        Spacer(modifier = Modifier.height(32.dp))
        ArOsButton(
            text = "Get Started",
            onClick = onNext,
            variant = ArOsButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StepPermission(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    buttonText: String,
    onAction: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    if (isGranted) ArOsTheme.colors.success.copy(alpha = 0.15f) else ArOsTheme.colors.accent.copy(alpha = 0.15f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.Check else icon,
                contentDescription = null,
                tint = if (isGranted) ArOsTheme.colors.success else ArOsTheme.colors.accent,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = ArOsTheme.typography.headline,
            color = ArOsTheme.colors.contentPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = ArOsTheme.typography.body,
            color = ArOsTheme.colors.contentSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        if (!isGranted) {
            ArOsButton(
                text = buttonText,
                onClick = onAction,
                variant = ArOsButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            ArOsButton(
                text = "Allowed ✓ Continue",
                onClick = onNext,
                variant = ArOsButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StepSimSelection(
    availableSims: List<SimAccountInfo>,
    selectedSimId: String?,
    isGranted: Boolean,
    onGrantPermission: () -> Unit,
    onSelectSim: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(ArOsTheme.colors.accent.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SimCard,
                contentDescription = null,
                tint = ArOsTheme.colors.accent,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Phone Status & SIM",
            style = ArOsTheme.typography.headline,
            color = ArOsTheme.colors.contentPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Detects your default calling SIM so AR SOS calls directly without carrier prompt stalls.",
            style = ArOsTheme.typography.body,
            color = ArOsTheme.colors.contentSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (!isGranted) {
            ArOsButton(
                text = "Allow SIM Detection",
                onClick = onGrantPermission,
                variant = ArOsButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            if (availableSims.size > 1) {
                Text(
                    text = "Select SIM to use for SOS calls:",
                    style = ArOsTheme.typography.caption,
                    color = ArOsTheme.colors.contentTertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                availableSims.forEach { sim ->
                    val isChosen = sim.id == selectedSimId || (selectedSimId == null && sim.isDefault)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isChosen) ArOsTheme.colors.accent.copy(alpha = 0.15f) else ArOsTheme.colors.surfaceRaised)
                            .border(1.dp, if (isChosen) ArOsTheme.colors.accent else ArOsTheme.colors.hairline, RoundedCornerShape(10.dp))
                            .clickable { onSelectSim(sim.id) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${sim.label} (Slot ${sim.slotIndex + 1})",
                            style = ArOsTheme.typography.body,
                            color = ArOsTheme.colors.contentPrimary
                        )
                        if (isChosen) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ArOsTheme.colors.accent)
                        }
                    }
                }
            } else if (availableSims.size == 1) {
                Text(
                    text = "Auto-selected: ${availableSims.first().label}",
                    style = ArOsTheme.typography.body,
                    color = ArOsTheme.colors.success
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            ArOsButton(
                text = "Continue",
                onClick = onNext,
                variant = ArOsButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StepAccessibility(
    context: Context,
    isActive: Boolean,
    onNext: () -> Unit
) {
    var showRestrictedHelp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    if (isActive) ArOsTheme.colors.success.copy(alpha = 0.15f) else ArOsTheme.colors.warning.copy(alpha = 0.15f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.Check else Icons.Default.VolumeUp,
                contentDescription = null,
                tint = if (isActive) ArOsTheme.colors.success else ArOsTheme.colors.warning,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Volume button trigger",
            style = ArOsTheme.typography.headline,
            color = ArOsTheme.colors.contentPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Find AR/OS under Installed apps (or Downloaded apps) → turn it ON → tap Allow.",
            style = ArOsTheme.typography.body,
            color = ArOsTheme.colors.contentSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (!isActive) {
            ArOsButton(
                text = "Open Accessibility Settings",
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    showRestrictedHelp = true
                },
                variant = ArOsButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )

            if (showRestrictedHelp) {
                Spacer(modifier = Modifier.height(16.dp))
                ArOsCard {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = ArOsTheme.colors.warning, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Setting greyed out on Motorola?", style = ArOsTheme.typography.footnote, fontWeight = FontWeight.Bold, color = ArOsTheme.colors.contentPrimary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Android blocks sideloaded apps. Open App Info → tap ⋮ (top right) → Allow restricted settings → come back and turn on.",
                            style = ArOsTheme.typography.caption,
                            color = ArOsTheme.colors.contentSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        ArOsButton(
                            text = "Open App Info",
                            onClick = {
                                val appInfoIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(appInfoIntent)
                            },
                            variant = ArOsButtonVariant.Secondary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            Text("Service active & connected ✓", style = ArOsTheme.typography.body, color = ArOsTheme.colors.success)
            Spacer(modifier = Modifier.height(16.dp))
            ArOsButton(
                text = "Continue",
                onClick = onNext,
                variant = ArOsButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StepBattery(
    context: Context,
    isIgnored: Boolean,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    if (isIgnored) ArOsTheme.colors.success.copy(alpha = 0.15f) else ArOsTheme.colors.accent.copy(alpha = 0.15f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BatteryAlert,
                contentDescription = null,
                tint = if (isIgnored) ArOsTheme.colors.success else ArOsTheme.colors.accent,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Keep running in background",
            style = ArOsTheme.typography.headline,
            color = ArOsTheme.colors.contentPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Prevents Android from pausing AR SOS during deep sleep. On Motorola, choose 'Unrestricted' battery usage if prompted.",
            style = ArOsTheme.typography.body,
            color = ArOsTheme.colors.contentSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (!isIgnored) {
            ArOsButton(
                text = "Allow Background Running",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(fallbackIntent)
                        }
                    }
                },
                variant = ArOsButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            ArOsButton(
                text = "Protected ✓ Continue",
                onClick = onNext,
                variant = ArOsButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StepContact(
    primaryContact: SosContact?,
    onAddContact: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    if (primaryContact != null) ArOsTheme.colors.success.copy(alpha = 0.15f) else ArOsTheme.colors.accent.copy(alpha = 0.15f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ContactPhone,
                contentDescription = null,
                tint = if (primaryContact != null) ArOsTheme.colors.success else ArOsTheme.colors.accent,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Choose Trusted Contact",
            style = ArOsTheme.typography.headline,
            color = ArOsTheme.colors.contentPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select a family member, partner, or trusted guardian who will receive emergency calls.",
            style = ArOsTheme.typography.body,
            color = ArOsTheme.colors.contentSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (primaryContact == null) {
            ArOsButton(
                text = "Select From Contacts",
                onClick = onAddContact,
                variant = ArOsButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = "Primary: ${primaryContact.name}",
                style = ArOsTheme.typography.headline,
                color = ArOsTheme.colors.success
            )
            Spacer(modifier = Modifier.height(16.dp))
            ArOsButton(
                text = "Continue",
                onClick = onNext,
                variant = ArOsButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StepPractice(
    telemetry: TriggerTelemetry,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(ArOsTheme.colors.accent.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = ArOsTheme.colors.accent,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Practice the Trigger",
            style = ArOsTheme.typography.headline,
            color = ArOsTheme.colors.contentPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Press your phone's Volume Down button 5 times rapidly right now.",
            style = ArOsTheme.typography.body,
            color = ArOsTheme.colors.contentSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 1..5) {
                val filled = telemetry.currentCount >= i
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (filled) ArOsTheme.colors.accent else ArOsTheme.colors.surfaceRaised,
                            CircleShape
                        )
                        .border(1.dp, ArOsTheme.colors.hairline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$i",
                        style = ArOsTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = if (filled) Color.White else ArOsTheme.colors.contentTertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        if (telemetry.currentCount >= 5) {
            Text("Trigger detected successfully! ✓", style = ArOsTheme.typography.body, color = ArOsTheme.colors.success)
            Spacer(modifier = Modifier.height(16.dp))
            ArOsButton(
                text = "Continue",
                onClick = onNext,
                variant = ArOsButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            TextButton(onClick = onNext) {
                Text("Continue without testing", color = ArOsTheme.colors.contentTertiary)
            }
        }
    }
}

@Composable
private fun StepModeSelection(
    currentMode: SosMode,
    contactName: String,
    onSelectMode: (SosMode) -> Unit,
    onNext: () -> Unit
) {
    var showLiveConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choose Your Mode",
            style = ArOsTheme.typography.title1,
            color = ArOsTheme.colors.contentPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You can change this anytime from the Safety tab.",
            style = ArOsTheme.typography.body,
            color = ArOsTheme.colors.contentSecondary
        )
        Spacer(modifier = Modifier.height(32.dp))

        ArOsButton(
            text = "Stay in TEST mode (no calls)",
            onClick = {
                onSelectMode(SosMode.TEST)
                onNext()
            },
            variant = if (currentMode == SosMode.TEST) ArOsButtonVariant.Primary else ArOsButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Everything works (screen, countdown, haptics) but no call is placed.",
            style = ArOsTheme.typography.caption,
            color = ArOsTheme.colors.contentTertiary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
        ArOsButton(
            text = "Switch to LIVE (real calls)",
            onClick = {
                onSelectMode(SosMode.LIVE)
                onNext()
            },
            variant = if (currentMode == SosMode.LIVE) ArOsButtonVariant.CriticalSos else ArOsButtonVariant.Ghost,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Pressing Volume Down 5 times WILL place a real phone call to $contactName.",
            style = ArOsTheme.typography.caption,
            color = ArOsTheme.colors.contentTertiary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StepSummary(
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(ArOsTheme.colors.success.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = ArOsTheme.colors.success,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "AR/OS is Ready",
            style = ArOsTheme.typography.title1,
            color = ArOsTheme.colors.contentPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "All critical permissions and safety systems have been configured.",
            style = ArOsTheme.typography.body,
            color = ArOsTheme.colors.contentSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        ArOsButton(
            text = "See what AR/OS can do",
            onClick = onFinish,
            variant = ArOsButtonVariant.Primary,
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
