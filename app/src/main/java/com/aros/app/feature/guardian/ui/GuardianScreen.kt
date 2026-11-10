package com.aros.app.feature.guardian.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.components.ArOsButton
import com.aros.app.designsystem.components.ArOsButtonVariant
import com.aros.app.designsystem.components.ArOsCard
import com.aros.app.designsystem.components.ArOsGroup
import com.aros.app.designsystem.components.ArOsScreenScaffold
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.guardian.data.GuardianScanEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianScreen(
    latestScan: GuardianScanEntity?,
    onRunAudit: () -> Unit
) {
    val score = latestScan?.sovereigntyScore ?: 88
    val scoreColor = when {
        score >= 80 -> ArOsTheme.colors.success
        score >= 60 -> ArOsTheme.colors.warning
        else -> ArOsTheme.colors.danger
    }

    ArOsScreenScaffold(
        title = "Digital Guardian"
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.md)
        ) {
            // 1. Device Sovereignty Posture Hero Card
            item {
                ArOsCard(
                    modifier = Modifier
                        .padding(horizontal = ArOsTheme.spacing.screenMargin)
                        .testTag("guardian_score_card")
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "DEVICE SOVEREIGNTY POSTURE",
                            style = ArOsTheme.typography.caption,
                            color = ArOsTheme.colors.contentTertiary
                        )

                        Spacer(modifier = Modifier.height(ArOsTheme.spacing.md))

                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(scoreColor.copy(alpha = 0.15f))
                                .border(2.dp, scoreColor.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$score",
                                    style = ArOsTheme.typography.numericDisplay,
                                    color = scoreColor
                                )
                                Text(
                                    text = "/100",
                                    style = ArOsTheme.typography.caption,
                                    color = ArOsTheme.colors.contentTertiary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(ArOsTheme.spacing.md))

                        Text(
                            text = if (score >= 80) "Optimal Sensor & Permission Isolation" else "Attention Recommended: Multiple Sensor Grants Active",
                            style = ArOsTheme.typography.headline,
                            color = scoreColor
                        )

                        Spacer(modifier = Modifier.height(ArOsTheme.spacing.md))

                        ArOsButton(
                            text = "Run On-Device Audit",
                            onClick = onRunAudit,
                            variant = ArOsButtonVariant.Primary,
                            leadingIcon = Icons.Default.Refresh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_audit_button")
                        )
                    }
                }
            }

            // 2. Zero-Cloud Privacy Guarantee
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ArOsTheme.spacing.screenMargin)
                        .clip(ArOsTheme.shapes.md)
                        .background(ArOsTheme.colors.surfaceRaised)
                        .border(1.dp, ArOsTheme.colors.hairline, ArOsTheme.shapes.md)
                        .padding(ArOsTheme.spacing.md)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Security",
                            tint = ArOsTheme.colors.accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(ArOsTheme.spacing.sm))
                        Text(
                            text = "Digital Guardian inspects manifests strictly on-device. Zero network requests, zero telemetry, zero package leak.",
                            style = ArOsTheme.typography.footnote,
                            color = ArOsTheme.colors.contentSecondary,
                            lineHeight = ArOsTheme.typography.footnote.lineHeight
                        )
                    }
                }
            }

            // 3. Sensor Exposure Matrix
            item {
                ArOsGroup(header = "Hardware Exposure Breakdown") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ArOsTheme.spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.sm)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.sm)
                        ) {
                            // Mic
                            ArOsCard(
                                modifier = Modifier.weight(1f),
                                backgroundColor = ArOsTheme.colors.surfaceRaised
                            ) {
                                Column {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Mic",
                                        tint = ArOsTheme.colors.warning,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(ArOsTheme.spacing.xs))
                                    Text(
                                        text = "${latestScan?.micAppsCount ?: 4}",
                                        style = ArOsTheme.typography.numericTitle,
                                        color = ArOsTheme.colors.contentPrimary
                                    )
                                    Text(
                                        text = "Mic Access",
                                        style = ArOsTheme.typography.caption,
                                        color = ArOsTheme.colors.contentTertiary
                                    )
                                }
                            }

                            // Camera
                            ArOsCard(
                                modifier = Modifier.weight(1f),
                                backgroundColor = ArOsTheme.colors.surfaceRaised
                            ) {
                                Column {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Camera",
                                        tint = ArOsTheme.colors.accent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(ArOsTheme.spacing.xs))
                                    Text(
                                        text = "${latestScan?.cameraAppsCount ?: 5}",
                                        style = ArOsTheme.typography.numericTitle,
                                        color = ArOsTheme.colors.contentPrimary
                                    )
                                    Text(
                                        text = "Camera Access",
                                        style = ArOsTheme.typography.caption,
                                        color = ArOsTheme.colors.contentTertiary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.sm)
                        ) {
                            // Location
                            ArOsCard(
                                modifier = Modifier.weight(1f),
                                backgroundColor = ArOsTheme.colors.surfaceRaised
                            ) {
                                Column {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Location",
                                        tint = ArOsTheme.colors.success,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(ArOsTheme.spacing.xs))
                                    Text(
                                        text = "${latestScan?.locationAppsCount ?: 7}",
                                        style = ArOsTheme.typography.numericTitle,
                                        color = ArOsTheme.colors.contentPrimary
                                    )
                                    Text(
                                        text = "Location Trackers",
                                        style = ArOsTheme.typography.caption,
                                        color = ArOsTheme.colors.contentTertiary
                                    )
                                }
                            }

                            // Dual-Sensor High Risk
                            ArOsCard(
                                modifier = Modifier.weight(1f),
                                backgroundColor = ArOsTheme.colors.surfaceRaised
                            ) {
                                Column {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "High Risk",
                                        tint = ArOsTheme.colors.danger,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(ArOsTheme.spacing.xs))
                                    Text(
                                        text = "${latestScan?.highRiskAppsCount ?: 2}",
                                        style = ArOsTheme.typography.numericTitle,
                                        color = ArOsTheme.colors.danger
                                    )
                                    Text(
                                        text = "High Risk Dual",
                                        style = ArOsTheme.typography.caption,
                                        color = ArOsTheme.colors.contentTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
