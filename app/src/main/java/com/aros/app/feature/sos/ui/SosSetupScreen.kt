package com.aros.app.feature.sos.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.components.ArOsButton
import com.aros.app.designsystem.components.ArOsButtonVariant
import com.aros.app.designsystem.components.ArOsCard
import com.aros.app.designsystem.components.ArOsGroup
import com.aros.app.designsystem.components.ArOsIconButton
import com.aros.app.designsystem.components.ArOsRow
import com.aros.app.designsystem.components.ArOsScreenScaffold
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.sos.domain.ReadinessAction
import com.aros.app.feature.sos.domain.ReadinessCheckResult
import com.aros.app.feature.sos.domain.ReadinessReport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosSetupScreen(
    readinessReport: ReadinessReport?,
    onNavigateBack: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onRequestPhonePermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    val context = LocalContext.current

    ArOsScreenScaffold(
        title = "Safety Setup",
        navigationIcon = {
            ArOsIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onNavigateBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.md)
        ) {
            item {
                ArOsCard(
                    modifier = Modifier.padding(horizontal = ArOsTheme.spacing.screenMargin)
                ) {
                    Column {
                        Text(
                            text = "SYSTEM READINESS & PERMISSIONS",
                            style = ArOsTheme.typography.caption,
                            color = ArOsTheme.colors.contentTertiary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pipeline Prerequisites",
                            style = ArOsTheme.typography.headline,
                            color = ArOsTheme.colors.contentPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "All checklist gates must be passing for reliable 5-press hardware detection and outgoing call execution.",
                            style = ArOsTheme.typography.footnote,
                            color = ArOsTheme.colors.contentSecondary
                        )
                    }
                }
            }

            // Checklist of Readiness Points
            item {
                val checks = readinessReport?.checks ?: emptyList()
                ArOsGroup(header = "Readiness Verification Gates") {
                    checks.forEachIndexed { index, check ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = ArOsTheme.spacing.md,
                                    vertical = ArOsTheme.spacing.sm
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (check.isPassing) ArOsTheme.colors.success.copy(alpha = 0.15f) else ArOsTheme.colors.danger.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (check.isPassing) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (check.isPassing) ArOsTheme.colors.success else ArOsTheme.colors.danger,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(ArOsTheme.spacing.md))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = check.title,
                                    style = ArOsTheme.typography.body,
                                    color = ArOsTheme.colors.contentPrimary
                                )
                                Text(
                                    text = check.description,
                                    style = ArOsTheme.typography.footnote,
                                    color = ArOsTheme.colors.contentSecondary
                                )
                            }

                            if (!check.isPassing && check.action != null) {
                                ArOsButton(
                                    text = "Fix",
                                    onClick = {
                                        when (check.action) {
                                            ReadinessAction.ENABLE_ACCESSIBILITY -> openAccessibilitySettings(context)
                                            ReadinessAction.GRANT_CALL_PHONE -> onRequestPhonePermission()
                                            ReadinessAction.GRANT_NOTIFICATIONS -> onRequestNotificationPermission()
                                            ReadinessAction.ADD_CONTACT -> onNavigateToContacts()
                                            ReadinessAction.DISABLE_BATTERY_OPTIMIZATION -> openBatterySettings(context)
                                            ReadinessAction.OPEN_RESTRICTED_SETTINGS -> openAppSettings(context)
                                            else -> openAppSettings(context)
                                        }
                                    },
                                    variant = ArOsButtonVariant.Secondary
                                )
                            }
                        }
                    }
                }
            }

            // Motorola / Android 13+ Restricted Setting Walkthrough
            item {
                ArOsCard(
                    modifier = Modifier
                        .padding(horizontal = ArOsTheme.spacing.screenMargin)
                        .testTag("restricted_settings_guide")
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = ArOsTheme.colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Motorola Restricted Setting Unlock",
                                style = ArOsTheme.typography.headline,
                                color = ArOsTheme.colors.contentPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "If Android grays out the AR/OS Accessibility toggle with 'Restricted setting':",
                            style = ArOsTheme.typography.footnote,
                            color = ArOsTheme.colors.contentSecondary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        StepText(step = "1", text = "Tap 'Open App Info' below to access system settings.")
                        StepText(step = "2", text = "Tap the 3 dots (⋮) in the top-right corner.")
                        StepText(step = "3", text = "Select 'Allow restricted settings' and confirm your PIN/fingerprint.")
                        StepText(step = "4", text = "Return to Accessibility settings and toggle 'AR/OS' ON.")

                        Spacer(modifier = Modifier.height(ArOsTheme.spacing.md))

                        ArOsButton(
                            text = "Open AR/OS App Info Settings",
                            onClick = { openAppSettings(context) },
                            variant = ArOsButtonVariant.Primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepText(step: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$step.",
            style = ArOsTheme.typography.footnote,
            color = ArOsTheme.colors.accent
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = ArOsTheme.typography.footnote,
            color = ArOsTheme.colors.contentSecondary
        )
    }
}

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

private fun openBatterySettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppSettings(context)
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
