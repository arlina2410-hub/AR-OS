package com.aros.app.feature.sos.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.components.ArOsCard
import com.aros.app.designsystem.components.ArOsGroup
import com.aros.app.designsystem.components.ArOsScreenScaffold
import com.aros.app.designsystem.theme.ArOsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpLimitsScreen() {
    ArOsScreenScaffold(
        title = "System Limits"
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
                            text = "OPERATIONAL LIMITS & SYSTEM ARCHITECTURE",
                            style = ArOsTheme.typography.caption,
                            color = ArOsTheme.colors.contentTertiary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Understanding AR SOS",
                            style = ArOsTheme.typography.headline,
                            color = ArOsTheme.colors.contentPrimary
                        )
                    }
                }
            }

            // How to Trigger Card
            item {
                ArOsCard(
                    modifier = Modifier.padding(horizontal = ArOsTheme.spacing.screenMargin)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = ArOsTheme.colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How to Trigger AR SOS",
                                style = ArOsTheme.typography.headline,
                                color = ArOsTheme.colors.contentPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "• Unlocked / Home Screen / Any App:",
                            style = ArOsTheme.typography.body,
                            color = ArOsTheme.colors.contentPrimary
                        )
                        Text(
                            text = "Press physical Volume Down 5 times rapidly.",
                            style = ArOsTheme.typography.footnote,
                            color = ArOsTheme.colors.contentSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "• Phone Screen Sleeping / Locked:",
                            style = ArOsTheme.typography.body,
                            color = ArOsTheme.colors.contentPrimary
                        )
                        Text(
                            text = "Press the Power button once so the lock screen wakes up (no unlock needed), then press Volume Down 5 times rapidly.",
                            style = ArOsTheme.typography.footnote,
                            color = ArOsTheme.colors.contentSecondary
                        )
                    }
                }
            }

            // Critical Operating Boundaries
            item {
                ArOsCard(
                    modifier = Modifier.padding(horizontal = ArOsTheme.spacing.screenMargin)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = ArOsTheme.colors.warning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Critical Operating Boundaries",
                                style = ArOsTheme.typography.headline,
                                color = ArOsTheme.colors.contentPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "1. Phone Powered OFF:\nAR SOS CANNOT operate when the device is completely powered down.",
                            style = ArOsTheme.typography.footnote,
                            color = ArOsTheme.colors.contentSecondary,
                            lineHeight = ArOsTheme.typography.footnote.lineHeight
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "2. No Signal / Airplane Mode:\nAR SOS requires cellular carrier service to place phone calls. If out of range, calls cannot connect.",
                            style = ArOsTheme.typography.footnote,
                            color = ArOsTheme.colors.contentSecondary,
                            lineHeight = ArOsTheme.typography.footnote.lineHeight
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "3. Official Emergency Services:\nAR SOS routes to your personal trusted guardians. For 911 / 112 / 999, always use your phone's native emergency dialer.",
                            style = ArOsTheme.typography.footnote,
                            color = ArOsTheme.colors.contentSecondary,
                            lineHeight = ArOsTheme.typography.footnote.lineHeight
                        )
                    }
                }
            }

            // Motorola Moto G57 Power Guidance
            item {
                ArOsCard(
                    modifier = Modifier.padding(horizontal = ArOsTheme.spacing.screenMargin)
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
                                text = "Motorola Moto G57 Power Guidance",
                                style = ArOsTheme.typography.headline,
                                color = ArOsTheme.colors.contentPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Motorola Hello UI aggressively manages background processes. To prevent AR SOS from being suspended:\n\n• Set App Battery Usage to 'Unrestricted' in App Info.\n• Keep Accessibility service enabled.\n• Do not force-stop AR/OS.",
                            style = ArOsTheme.typography.footnote,
                            color = ArOsTheme.colors.contentSecondary,
                            lineHeight = ArOsTheme.typography.footnote.lineHeight
                        )
                    }
                }
            }
        }
    }
}
