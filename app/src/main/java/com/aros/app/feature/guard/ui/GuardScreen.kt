package com.aros.app.feature.guard.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aros.app.designsystem.components.ArOsButton
import com.aros.app.designsystem.components.ArOsButtonVariant
import com.aros.app.designsystem.components.ArOsCard
import com.aros.app.designsystem.components.ArOsPillTone
import com.aros.app.designsystem.components.ArOsRow
import com.aros.app.designsystem.components.ArOsScreenScaffold
import com.aros.app.designsystem.components.ArOsStatusPill
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.guard.domain.GuardConfig
import com.aros.app.feature.guard.domain.GuardEngine
import com.aros.app.feature.guard.domain.GuardRepository
import com.aros.app.feature.guard.domain.GuardStatus
import com.aros.app.feature.guard.domain.RelockDelay
import com.aros.app.feature.guard.platform.ArGuardAccessibilityService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardScreen(
    guardRepository: GuardRepository,
    guardEngine: GuardEngine,
    onNavigateToLab: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val config by guardRepository.configFlow.collectAsState(initial = GuardConfig())
    val isServiceConnected by guardRepository.isServiceConnected.collectAsState()

    val guardStatus = when {
        !config.isConfigured -> GuardStatus.NOT_SET_UP
        isServiceConnected -> GuardStatus.ARMED
        else -> GuardStatus.SERVICE_OFF
    }

    var showSetupWizard by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDisableGuardDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    ArOsScreenScaffold(
        title = "AR Guard",
        subtitle = "Full-screen device gate protecting your apps after unlock",
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ArOsTheme.colors.contentPrimary)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = ArOsTheme.spacing.screenMargin)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.md)
        ) {
            // Status Header Card
            item {
                ArOsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            when (guardStatus) {
                                                GuardStatus.ARMED -> ArOsTheme.colors.success.copy(alpha = 0.2f)
                                                GuardStatus.SERVICE_OFF -> ArOsTheme.colors.sosCritical.copy(alpha = 0.2f)
                                                GuardStatus.NOT_SET_UP -> ArOsTheme.colors.warning.copy(alpha = 0.2f)
                                            },
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (guardStatus) {
                                            GuardStatus.ARMED -> Icons.Default.Security
                                            GuardStatus.SERVICE_OFF -> Icons.Default.Warning
                                            GuardStatus.NOT_SET_UP -> Icons.Default.LockOpen
                                        },
                                        contentDescription = null,
                                        tint = when (guardStatus) {
                                            GuardStatus.ARMED -> ArOsTheme.colors.success
                                            GuardStatus.SERVICE_OFF -> ArOsTheme.colors.sosCritical
                                            GuardStatus.NOT_SET_UP -> ArOsTheme.colors.warning
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Gate Status",
                                        style = ArOsTheme.typography.caption,
                                        color = ArOsTheme.colors.contentSecondary
                                    )
                                    Text(
                                        text = when (guardStatus) {
                                            GuardStatus.ARMED -> "Armed & Protected"
                                            GuardStatus.SERVICE_OFF -> "Service is OFF"
                                            GuardStatus.NOT_SET_UP -> "Not Configured"
                                        },
                                        style = ArOsTheme.typography.headline,
                                        color = ArOsTheme.colors.contentPrimary
                                    )
                                }
                            }
                            ArOsStatusPill(
                                label = when (guardStatus) {
                                    GuardStatus.ARMED -> "ARMED"
                                    GuardStatus.SERVICE_OFF -> "SERVICE OFF"
                                    GuardStatus.NOT_SET_UP -> "SETUP NEEDED"
                                },
                                tone = when (guardStatus) {
                                    GuardStatus.ARMED -> ArOsPillTone.Success
                                    GuardStatus.SERVICE_OFF -> ArOsPillTone.Danger
                                    GuardStatus.NOT_SET_UP -> ArOsPillTone.Warning
                                }
                            )
                        }

                        if (guardStatus == GuardStatus.SERVICE_OFF) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "AR Guard accessibility service is disabled. Sideload protection gate cannot appear.",
                                style = ArOsTheme.typography.footnote,
                                color = ArOsTheme.colors.sosCritical
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            ArOsButton(
                                text = "Enable AR Guard Service",
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                },
                                variant = ArOsButtonVariant.Primary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // If not set up -> show Setup Card
            if (!config.isConfigured || showSetupWizard) {
                item {
                    SetupWizardCard(
                        guardEngine = guardEngine,
                        guardRepository = guardRepository,
                        isServiceConnected = isServiceConnected,
                        onComplete = { showSetupWizard = false }
                    )
                }
            } else {
                // Configured Controls: Lock Now & Test Lock
                item {
                    ArOsCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Lock Controls",
                                style = ArOsTheme.typography.headline,
                                color = ArOsTheme.colors.contentPrimary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ArOsButton(
                                    text = "Lock Now",
                                    onClick = { ArGuardAccessibilityService.triggerLockNow() },
                                    variant = ArOsButtonVariant.Primary,
                                    modifier = Modifier.weight(1f)
                                )
                                ArOsButton(
                                    text = "Test Lock (20s)",
                                    onClick = { ArGuardAccessibilityService.triggerTestLock() },
                                    variant = ArOsButtonVariant.Secondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Settings: Re-lock delay & Go to home
                item {
                    ArOsCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Lock Behaviour",
                                style = ArOsTheme.typography.headline,
                                color = ArOsTheme.colors.contentPrimary
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Lock again after screen off",
                                style = ArOsTheme.typography.body,
                                fontWeight = FontWeight.SemiBold,
                                color = ArOsTheme.colors.contentPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RelockDelay.entries.forEach { delayOption ->
                                    val isSelected = config.relockDelaySeconds == delayOption.seconds
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) ArOsTheme.colors.accent else ArOsTheme.colors.surfaceRaised
                                            )
                                            .clickable {
                                                coroutineScope.launch {
                                                    guardRepository.setRelockDelay(delayOption)
                                                }
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = delayOption.label,
                                            style = ArOsTheme.typography.caption,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) ArOsTheme.colors.contentOnAccent else ArOsTheme.colors.contentSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Go to home screen when locked",
                                        style = ArOsTheme.typography.body,
                                        color = ArOsTheme.colors.contentPrimary
                                    )
                                    Text(
                                        text = "Hides active apps behind the gate so private screens are never seen.",
                                        style = ArOsTheme.typography.caption,
                                        color = ArOsTheme.colors.contentSecondary
                                    )
                                }
                                Switch(
                                    checked = config.goToHomeOnLock,
                                    onCheckedChange = { checked ->
                                        coroutineScope.launch {
                                            guardRepository.setGoToHomeOnLock(checked)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = ArOsTheme.colors.accent)
                                )
                            }
                        }
                    }
                }

                // Security actions: Change PIN & Turn Off
                item {
                    ArOsCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Security & Credentials",
                                style = ArOsTheme.typography.headline,
                                color = ArOsTheme.colors.contentPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ArOsRow(
                                title = "Change AR Guard PIN",
                                subtitle = "Requires your current PIN",
                                onClick = { showChangePinDialog = true }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ArOsRow(
                                title = "Turn Off AR Guard",
                                subtitle = "Disable device lock gate (requires current PIN)",
                                onClick = { showDisableGuardDialog = true }
                            )
                        }
                    }
                }

                // Failed attempts log
                item {
                    ArOsCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Failed Attempts History",
                                    style = ArOsTheme.typography.headline,
                                    color = ArOsTheme.colors.contentPrimary
                                )
                                if (config.failedAttemptHistory.isNotEmpty()) {
                                    TextButton(onClick = { coroutineScope.launch { guardRepository.clearFailedAttemptHistory() } }) {
                                        Text("Clear", color = ArOsTheme.colors.accent, fontSize = 12.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (config.failedAttemptHistory.isEmpty()) {
                                Text(
                                    text = "Zero unauthorized unlock attempts logged.",
                                    style = ArOsTheme.typography.footnote,
                                    color = ArOsTheme.colors.contentSecondary
                                )
                            } else {
                                config.failedAttemptHistory.take(5).forEach { record ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Wrong PIN attempt #${record.totalFailedCount}",
                                            style = ArOsTheme.typography.footnote,
                                            color = ArOsTheme.colors.sosCritical
                                        )
                                        Text(
                                            text = record.formattedTime12h,
                                            style = ArOsTheme.typography.caption,
                                            color = ArOsTheme.colors.contentSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                // Lost Guard & Guard Lab Card
                item {
                    ArOsCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Lost Guard & Window Defense",
                                    style = ArOsTheme.typography.headline,
                                    color = ArOsTheme.colors.contentPrimary
                                )
                                val isLearnActive = System.currentTimeMillis() < config.learnModeExpiresAtEpochMillis
                                if (isLearnActive) {
                                    ArOsStatusPill(label = "LEARN ON", tone = ArOsPillTone.Warning)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Automatically closes the power/restart menu, notification shade, and phone settings while AR Guard is locked to prevent a thief from switching to airplane mode or disabling tracking.",
                                style = ArOsTheme.typography.caption,
                                color = ArOsTheme.colors.contentSecondary,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "During calls and SOS, only the call screen and the SOS screen stay open. Anything else brings the gate back.",
                                style = ArOsTheme.typography.footnote,
                                color = ArOsTheme.colors.accent,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ArOsRow(
                                title = "Open Guard Lab (Window Inspector)",
                                subtitle = "Observe window events, block targets & manage call screens",
                                onClick = onNavigateToLab
                            )
                        }
                    }
                }

                // Device Admin / Uninstall Protection Card
                item {
                    val dpm = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager }
                    val adminComponent = remember { android.content.ComponentName(context, com.aros.app.feature.guard.platform.ArGuardDeviceAdminReceiver::class.java) }
                    val isDeviceAdminActive = remember(dpm, adminComponent) {
                        dpm?.isAdminActive(adminComponent) ?: false
                    }

                    ArOsCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Uninstall Protection",
                                    style = ArOsTheme.typography.headline,
                                    color = ArOsTheme.colors.contentPrimary
                                )
                                ArOsStatusPill(
                                    label = if (isDeviceAdminActive) "ACTIVE" else "OFF",
                                    tone = if (isDeviceAdminActive) ArOsPillTone.Success else ArOsPillTone.Neutral
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Optionally registers AR/OS as a device administrator (with force-lock policy only). Prevents casual uninstallation of AR/OS without first deactivating admin in phone settings.",
                                style = ArOsTheme.typography.caption,
                                color = ArOsTheme.colors.contentSecondary,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (!isDeviceAdminActive) {
                                ArOsButton(
                                    text = "Enable Uninstall Protection",
                                    onClick = {
                                        try {
                                            val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                                putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                                putExtra(
                                                    android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                                    "Enables uninstall protection for AR Guard so the app cannot be removed without entering phone security credentials."
                                                )
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                                    variant = ArOsButtonVariant.Secondary,
                                    modifier = Modifier.fillMaxWidth().testTag("enable_device_admin_button")
                                )
                            } else {
                                Text(
                                    text = "Uninstall protection is active. To remove AR/OS, deactivate Device Admin in Android Settings > Security.",
                                    style = ArOsTheme.typography.footnote,
                                    color = ArOsTheme.colors.success
                                )
                            }
                        }
                    }
                }

                // Second-Layer Theft Protection Checklist (Android 17)
                item {
                    ArOsCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Theft-Protection Checklist (Android 17)",
                                style = ArOsTheme.typography.headline,
                                color = ArOsTheme.colors.contentPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "AR/OS is your first gate. For full physical security, ensure Android's native protections are switched ON in phone settings:",
                                style = ArOsTheme.typography.caption,
                                color = ArOsTheme.colors.contentSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("• Android Theft Detection Lock (accelerometer snatch lock)", style = ArOsTheme.typography.footnote, color = ArOsTheme.colors.contentPrimary)
                                Text("• Offline Device Lock (auto-locks if data severed)", style = ArOsTheme.typography.footnote, color = ArOsTheme.colors.contentPrimary)
                                Text("• Remote Lock via android.com/lock", style = ArOsTheme.typography.footnote, color = ArOsTheme.colors.contentPrimary)
                                Text("• Google Find Hub / Find My Device (with network off)", style = ArOsTheme.typography.footnote, color = ArOsTheme.colors.contentPrimary)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            ArOsButton(
                                text = "Open Android Security Settings",
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                                    } catch (_: Exception) {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                        } catch (_: Exception) {}
                                    }
                                },
                                variant = ArOsButtonVariant.Secondary,
                                modifier = Modifier.fillMaxWidth().testTag("open_security_settings_button")
                            )
                        }
                    }
                }

                // Tamper & Defense Log Card
                item {
                    ArOsCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Lost Guard Tamper Log (${config.tamperLogs.size})",
                                    style = ArOsTheme.typography.headline,
                                    color = ArOsTheme.colors.contentPrimary
                                )
                                if (config.tamperLogs.isNotEmpty()) {
                                    TextButton(onClick = { coroutineScope.launch { guardRepository.clearTamperLogs() } }) {
                                        Text("Clear", color = ArOsTheme.colors.accent, fontSize = 12.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (config.tamperLogs.isEmpty()) {
                                Text(
                                    text = "No defense actions or tamper events logged.",
                                    style = ArOsTheme.typography.footnote,
                                    color = ArOsTheme.colors.contentSecondary
                                )
                            } else {
                                config.tamperLogs.take(10).forEach { log ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = log.eventType.replace('_', ' '),
                                                style = ArOsTheme.typography.footnote,
                                                fontWeight = FontWeight.Bold,
                                                color = ArOsTheme.colors.contentPrimary
                                            )
                                            Text(
                                                text = log.detail,
                                                style = ArOsTheme.typography.caption,
                                                color = ArOsTheme.colors.contentSecondary
                                            )
                                        }
                                        Text(
                                            text = log.formattedTime12h,
                                            style = ArOsTheme.typography.caption,
                                            color = ArOsTheme.colors.accent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Honest Limits text (Required by spec)
            item {
                ArOsCard {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = ArOsTheme.colors.contentSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AR Guard cannot stop a wipe from recovery mode or a restart into safe mode. Keep Google Find Hub and Android theft protection turned on as your second layer.",
                            style = ArOsTheme.typography.caption,
                            color = ArOsTheme.colors.contentSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        ChangePinDialog(
            guardRepository = guardRepository,
            onDismiss = { showChangePinDialog = false }
        )
    }

    // Disable Guard Dialog
    if (showDisableGuardDialog) {
        DisableGuardDialog(
            guardRepository = guardRepository,
            onDismiss = { showDisableGuardDialog = false }
        )
    }
}

@Composable
private fun SetupWizardCard(
    guardEngine: GuardEngine,
    guardRepository: GuardRepository,
    isServiceConnected: Boolean,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyguardManager = remember { context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager }
    val isDeviceSecure = keyguardManager?.isDeviceSecure ?: false

    var step by remember { mutableStateOf(1) }
    var pinFirst by remember { mutableStateOf("") }
    var pinSecond by remember { mutableStateOf("") }
    var generatedRecoveryCode by remember { mutableStateOf("") }
    var wroteRecoveryDown by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    ArOsCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Guided Setup (Step $step of 5)",
                style = ArOsTheme.typography.caption,
                color = ArOsTheme.colors.accent,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            when (step) {
                1 -> {
                    Text(
                        text = "1. Device Lock Screen Check",
                        style = ArOsTheme.typography.headline,
                        color = ArOsTheme.colors.contentPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AR Guard works as a second layer of defense after your phone's own lock screen is unlocked.",
                        style = ArOsTheme.typography.body,
                        color = ArOsTheme.colors.contentSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (!isDeviceSecure) {
                        Box(
                            modifier = Modifier
                                .background(ArOsTheme.colors.warning.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Warning: Your phone does not have a screen lock set. Set a PIN, pattern, or password in Android Settings first.",
                                style = ArOsTheme.typography.caption,
                                color = ArOsTheme.colors.warning
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ArOsTheme.colors.success, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Phone screen lock detected ✓", style = ArOsTheme.typography.body, color = ArOsTheme.colors.success)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    ArOsButton(
                        text = "Continue to Set PIN",
                        onClick = { step = 2 },
                        variant = ArOsButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                2 -> {
                    Text(
                        text = "2. Set AR Guard PIN",
                        style = ArOsTheme.typography.headline,
                        color = ArOsTheme.colors.contentPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose 6 to 12 digits. Do not use your phone's lock screen PIN.",
                        style = ArOsTheme.typography.body,
                        color = ArOsTheme.colors.contentSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = pinFirst,
                        onValueChange = { if (it.length <= 12 && it.all { c -> c.isDigit() }) pinFirst = it },
                        label = { Text("Enter 6–12 digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArOsTheme.colors.accent,
                            focusedTextColor = ArOsTheme.colors.contentPrimary,
                            unfocusedTextColor = ArOsTheme.colors.contentPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pinSecond,
                        onValueChange = { if (it.length <= 12 && it.all { c -> c.isDigit() }) pinSecond = it },
                        label = { Text("Re-enter PIN to confirm") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArOsTheme.colors.accent,
                            focusedTextColor = ArOsTheme.colors.contentPrimary,
                            unfocusedTextColor = ArOsTheme.colors.contentPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = pinError ?: "", color = ArOsTheme.colors.sosCritical, style = ArOsTheme.typography.caption)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ArOsButton(
                        text = "Save PIN & Continue",
                        onClick = {
                            if (!guardEngine.validatePinFormat(pinFirst)) {
                                pinError = "PIN must be between 6 and 12 digits."
                            } else if (pinFirst != pinSecond) {
                                pinError = "PINs do not match."
                            } else {
                                pinError = null
                                generatedRecoveryCode = guardEngine.generateRecoveryCode()
                                step = 3
                            }
                        },
                        variant = ArOsButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                3 -> {
                    Text(
                        text = "3. Save Recovery Code",
                        style = ArOsTheme.typography.headline,
                        color = ArOsTheme.colors.contentPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Write down this 16-character code. It is shown ONCE and will never be displayed again.",
                        style = ArOsTheme.typography.body,
                        color = ArOsTheme.colors.contentSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ArOsTheme.colors.surfaceRaised, RoundedCornerShape(12.dp))
                            .border(1.dp, ArOsTheme.colors.accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = generatedRecoveryCode.chunked(4).joinToString("-"),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            color = ArOsTheme.colors.accent
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = wroteRecoveryDown,
                            onCheckedChange = { wroteRecoveryDown = it },
                            colors = CheckboxDefaults.colors(checkedColor = ArOsTheme.colors.accent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "I wrote down my recovery code in a safe place",
                            style = ArOsTheme.typography.caption,
                            color = ArOsTheme.colors.contentPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "If you lose both the PIN and the recovery code, the only way back is to restart the phone in safe mode and uninstall AR/OS, which erases its data.",
                        style = ArOsTheme.typography.caption,
                        color = ArOsTheme.colors.contentTertiary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ArOsButton(
                        text = "Save Credentials",
                        enabled = wroteRecoveryDown && !isSaving,
                        onClick = {
                            isSaving = true
                            coroutineScope.launch {
                                guardRepository.setupGuard(pinFirst, generatedRecoveryCode)
                                isSaving = false
                                step = 4
                            }
                        },
                        variant = ArOsButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                4 -> {
                    Text(
                        text = "4. Enable Accessibility Service",
                        style = ArOsTheme.typography.headline,
                        color = ArOsTheme.colors.contentPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AR Guard requires accessibility access to display the lock gate over your apps. It does not read your screen or typing.",
                        style = ArOsTheme.typography.body,
                        color = ArOsTheme.colors.contentSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    if (!isServiceConnected) {
                        ArOsButton(
                            text = "Open Accessibility Settings",
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            variant = ArOsButtonVariant.Primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .background(ArOsTheme.colors.surfaceRaised, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Setting greyed out on Motorola?",
                                    style = ArOsTheme.typography.footnote,
                                    fontWeight = FontWeight.Bold,
                                    color = ArOsTheme.colors.warning
                                )
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
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ArOsTheme.colors.success, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AR Guard service active & running ✓", style = ArOsTheme.typography.body, color = ArOsTheme.colors.success)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        ArOsButton(
                            text = "Proceed to Lock Test",
                            onClick = { step = 5 },
                            variant = ArOsButtonVariant.Primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                5 -> {
                    Text(
                        text = "5. Test the Lock",
                        style = ArOsTheme.typography.headline,
                        color = ArOsTheme.colors.contentPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Verify the lock gate works. The test displays for at most 20 seconds with a visible Close button. On real locks, no close button exists.",
                        style = ArOsTheme.typography.body,
                        color = ArOsTheme.colors.contentSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ArOsButton(
                        text = "Launch Test Lock (20s)",
                        onClick = {
                            ArGuardAccessibilityService.triggerTestLock()
                        },
                        variant = ArOsButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ArOsButton(
                        text = "Finish Setup",
                        onClick = onComplete,
                        variant = ArOsButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangePinDialog(
    guardRepository: GuardRepository,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change AR Guard PIN", style = ArOsTheme.typography.headline) },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { currentPin = it },
                    label = { Text("Current PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text("New PIN (6–12 digits)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it },
                    label = { Text("Confirm New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage ?: "", color = ArOsTheme.colors.sosCritical, style = ArOsTheme.typography.caption)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPin != confirmPin) {
                        errorMessage = "New PINs do not match."
                    } else if (newPin.length !in 6..12 || !newPin.all { it.isDigit() }) {
                        errorMessage = "New PIN must be 6 to 12 digits."
                    } else {
                        isSubmitting = true
                        coroutineScope.launch {
                            val success = guardRepository.changePin(currentPin, newPin)
                            isSubmitting = false
                            if (success) {
                                onDismiss()
                            } else {
                                errorMessage = "Current PIN is incorrect."
                            }
                        }
                    }
                },
                enabled = !isSubmitting
            ) {
                Text("Change PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DisableGuardDialog(
    guardRepository: GuardRepository,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Turn Off AR Guard", style = ArOsTheme.typography.headline) },
        text = {
            Column {
                Text(
                    text = "Enter your current PIN to remove the device lock gate.",
                    style = ArOsTheme.typography.body,
                    color = ArOsTheme.colors.contentSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { currentPin = it },
                    label = { Text("Current PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage ?: "", color = ArOsTheme.colors.sosCritical, style = ArOsTheme.typography.caption)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSubmitting = true
                    coroutineScope.launch {
                        val success = guardRepository.disableGuard(currentPin)
                        isSubmitting = false
                        if (success) {
                            onDismiss()
                        } else {
                            errorMessage = "Current PIN is incorrect."
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ArOsTheme.colors.sosCritical),
                enabled = !isSubmitting
            ) {
                Text("Turn Off Guard")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
