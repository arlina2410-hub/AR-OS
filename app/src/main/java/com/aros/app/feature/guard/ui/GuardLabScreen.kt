package com.aros.app.feature.guard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aros.app.designsystem.components.ArOsButton
import com.aros.app.designsystem.components.ArOsButtonVariant
import com.aros.app.designsystem.components.ArOsCard
import com.aros.app.designsystem.components.ArOsPillTone
import com.aros.app.designsystem.components.ArOsScreenScaffold
import com.aros.app.designsystem.components.ArOsStatusPill
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.guard.domain.BlockedTarget
import com.aros.app.feature.guard.domain.GuardConfig
import com.aros.app.feature.guard.domain.GuardRepository
import com.aros.app.feature.guard.domain.ObservedWindow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardLabScreen(
    guardRepository: GuardRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by guardRepository.configFlow.collectAsState(initial = GuardConfig())
    val observedWindows by guardRepository.observedWindows.collectAsState()
    val scope = rememberCoroutineScope()

    val nowMillis = System.currentTimeMillis()
    val isLearnModeActive = nowMillis < config.learnModeExpiresAtEpochMillis
    val remainingSeconds = if (isLearnModeActive) {
        ((config.learnModeExpiresAtEpochMillis - nowMillis) / 1000L).coerceAtLeast(0L)
    } else 0L

    ArOsScreenScaffold(
        title = "Guard Lab",
        subtitle = "Window inspector & defense rule builder",
        modifier = modifier.testTag("guard_lab_screen"),
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ArOsTheme.colors.contentPrimary
                )
            }
        },
        actions = {
            if (isLearnModeActive) {
                ArOsStatusPill(
                    label = "LEARN ON (${remainingSeconds}s)",
                    tone = ArOsPillTone.Warning
                )
            } else {
                ArOsStatusPill(
                    label = "LEARN OFF",
                    tone = ArOsPillTone.Neutral
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ArOsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Window Inspector (Learn Mode)",
                            style = ArOsTheme.typography.headline,
                            color = ArOsTheme.colors.contentPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Turn on Learn Mode for 10 minutes to observe system windows as you open them (e.g., your phone's power menu or quick settings). AR/OS only captures package names and class names—never keystrokes or screen contents.",
                            style = ArOsTheme.typography.caption,
                            color = ArOsTheme.colors.contentSecondary,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(14.dp))

                        if (!isLearnModeActive) {
                            ArOsButton(
                                text = "Turn On Learn Mode (10 min)",
                                onClick = {
                                    scope.launch {
                                        guardRepository.startLearnMode(10)
                                    }
                                },
                                variant = ArOsButtonVariant.Primary,
                                leadingIcon = Icons.Default.PlayArrow,
                                modifier = Modifier.fillMaxWidth().testTag("start_learn_mode_button")
                            )
                        } else {
                            ArOsButton(
                                text = "Stop Learn Mode",
                                onClick = {
                                    scope.launch {
                                        guardRepository.stopLearnMode()
                                    }
                                },
                                variant = ArOsButtonVariant.Secondary,
                                leadingIcon = Icons.Default.Stop,
                                modifier = Modifier.fillMaxWidth().testTag("stop_learn_mode_button")
                            )
                        }
                    }
                }
            }

            // Prompt 3B: Allowed During Calls / SOS Section
            item {
                ArOsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Allowed During Calls & SOS (${config.allowedCallSosPackages.size})",
                            style = ArOsTheme.typography.headline,
                            color = ArOsTheme.colors.contentPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Only these packages can stay open while a call or SOS is active. Leaving them immediately brings the AR Guard gate back.",
                            style = ArOsTheme.typography.caption,
                            color = ArOsTheme.colors.contentSecondary,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            config.allowedCallSosPackages.forEach { pkg ->
                                val isDefault = GuardConfig.DEFAULT_ALLOWED_CALL_SOS_PACKAGES.contains(pkg)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            ArOsTheme.colors.surfaceRaised,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pkg,
                                            style = ArOsTheme.typography.body,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ArOsTheme.colors.contentPrimary
                                        )
                                        Text(
                                            text = if (isDefault) "Default allowed dialer/SOS package" else "User-allowed call screen",
                                            style = ArOsTheme.typography.caption,
                                            color = if (isDefault) ArOsTheme.colors.accent else ArOsTheme.colors.contentSecondary
                                        )
                                    }
                                    if (!isDefault) {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    guardRepository.removeAllowedCallSosPackage(pkg)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove package",
                                                tint = ArOsTheme.colors.sosCritical
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Custom Blocked Targets section
            item {
                ArOsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Custom Blocked Windows (${config.customBlockedTargets.size})",
                            style = ArOsTheme.typography.headline,
                            color = ArOsTheme.colors.contentPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        if (config.customBlockedTargets.isEmpty()) {
                            Text(
                                text = "No custom targets yet. Turn on Learn Mode, trigger the window you want to block, then tap 'Block this window' below.",
                                style = ArOsTheme.typography.footnote,
                                color = ArOsTheme.colors.contentSecondary
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                config.customBlockedTargets.forEach { target ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                ArOsTheme.colors.surfaceRaised,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = target.packageName,
                                                style = ArOsTheme.typography.body,
                                                fontWeight = FontWeight.SemiBold,
                                                color = ArOsTheme.colors.contentPrimary
                                            )
                                            if (target.className.isNotBlank()) {
                                                Text(
                                                    text = target.className,
                                                    style = ArOsTheme.typography.caption,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = ArOsTheme.colors.contentSecondary
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    guardRepository.removeCustomBlockedTarget(target)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove rule",
                                                tint = ArOsTheme.colors.sosCritical
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Observed Windows header
            item {
                Text(
                    text = "Observed Windows (${observedWindows.size} / 50)",
                    style = ArOsTheme.typography.headline,
                    color = ArOsTheme.colors.contentPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (observedWindows.isEmpty()) {
                item {
                    ArOsCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (isLearnModeActive) {
                                    "Learn Mode is active. Switch between apps or pull down notification shade to see window events here."
                                } else {
                                    "No window events recorded. Start Learn Mode to observe windows."
                                },
                                style = ArOsTheme.typography.footnote,
                                color = ArOsTheme.colors.contentSecondary
                            )
                        }
                    }
                }
            } else {
                items(observedWindows) { window ->
                    val isAlreadyBlocked = config.customBlockedTargets.any {
                        it.packageName == window.packageName &&
                            (it.className.isBlank() || it.className == window.className)
                    }
                    val isAlreadyAllowed = config.allowedCallSosPackages.any {
                        it.equals(window.packageName, ignoreCase = true)
                    }

                    ObservedWindowCard(
                        window = window,
                        isAlreadyBlocked = isAlreadyBlocked,
                        isAlreadyAllowed = isAlreadyAllowed,
                        onBlock = {
                            scope.launch {
                                guardRepository.addCustomBlockedTarget(
                                    BlockedTarget(
                                        packageName = window.packageName,
                                        className = window.className,
                                        label = window.packageName
                                    )
                                )
                            }
                        },
                        onAllowCallSos = {
                            scope.launch {
                                guardRepository.addAllowedCallSosPackage(window.packageName)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ObservedWindowCard(
    window: ObservedWindow,
    isAlreadyBlocked: Boolean,
    isAlreadyAllowed: Boolean,
    onBlock: () -> Unit,
    onAllowCallSos: () -> Unit
) {
    ArOsCard {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = window.formattedTime12h,
                    style = ArOsTheme.typography.caption,
                    color = ArOsTheme.colors.accent
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isAlreadyAllowed) {
                        ArOsStatusPill(label = "CALL/SOS ALLOWED", tone = ArOsPillTone.Success)
                    }
                    if (isAlreadyBlocked) {
                        ArOsStatusPill(label = "BLOCKED", tone = ArOsPillTone.Danger)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = window.packageName,
                style = ArOsTheme.typography.body,
                fontWeight = FontWeight.Bold,
                color = ArOsTheme.colors.contentPrimary
            )
            if (window.className.isNotBlank()) {
                Text(
                    text = window.className,
                    style = ArOsTheme.typography.caption,
                    fontFamily = FontFamily.Monospace,
                    color = ArOsTheme.colors.contentSecondary
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isAlreadyBlocked) {
                    ArOsButton(
                        text = "Block while locked",
                        onClick = onBlock,
                        variant = ArOsButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (!isAlreadyAllowed) {
                    ArOsButton(
                        text = "Allow in calls / SOS",
                        onClick = onAllowCallSos,
                        variant = ArOsButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
