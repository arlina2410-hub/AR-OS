package com.aros.app.feature.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.components.ArOsButton
import com.aros.app.designsystem.components.ArOsButtonVariant
import com.aros.app.designsystem.components.ArOsCard
import com.aros.app.designsystem.components.ArOsScreenScaffold
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.shell.AppDestination

data class FeatureInfo(
    val title: String,
    val subtitle: String,
    val status: String,
    val icon: ImageVector,
    val destination: AppDestination
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatArOsCanDoScreen(
    onNavigateTo: (AppDestination) -> Unit,
    onNavigateBack: () -> Unit
) {
    val features = listOf(
        FeatureInfo(
            title = "Safety SOS",
            subtitle = "Calls your trusted contact when you press Volume Down 5 times, completely offline.",
            status = "Active",
            icon = Icons.Default.HealthAndSafety,
            destination = AppDestination.SAFETY
        ),
        FeatureInfo(
            title = "Daily Rhythms",
            subtitle = "Track daily kickoffs, deep work routines, and wind-downs with 12-hour schedules.",
            status = "Active",
            icon = Icons.Default.Schedule,
            destination = AppDestination.ROUTINES
        ),
        FeatureInfo(
            title = "Focus Timers",
            subtitle = "Flow state and sprint timers that log interruptions and time spent on deep work.",
            status = "Active",
            icon = Icons.Default.CenterFocusStrong,
            destination = AppDestination.FOCUS
        ),
        FeatureInfo(
            title = "Digital Guardian",
            subtitle = "Inspects installed apps on your phone to calculate your local privacy score.",
            status = "Limited",
            icon = Icons.Default.Shield,
            destination = AppDestination.GUARDIAN
        ),
        FeatureInfo(
            title = "Activity Ledger",
            subtitle = "Visual timeline showing intentional focus versus phone usage spikes.",
            status = "Active",
            icon = Icons.Default.QueryStats,
            destination = AppDestination.LEDGER
        ),
        FeatureInfo(
            title = "Focus Rules",
            subtitle = "Adds gentle friction (breathing exercises or typing pledge) before distraction apps.",
            status = "Limited",
            icon = Icons.Default.Lock,
            destination = AppDestination.DISCIPLINE
        ),
        FeatureInfo(
            title = "Insights",
            subtitle = "On-device habit analysis summarizing your sovereignty trends and daily routines.",
            status = "Active",
            icon = Icons.Default.Insights,
            destination = AppDestination.ADAPTIVE
        )
    )

    ArOsScreenScaffold(
        title = "What AR/OS Can Do",
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ArOsTheme.colors.contentPrimary
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = ArOsTheme.spacing.screenMargin)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.md)
        ) {
            item {
                Text(
                    text = "A quiet, offline operating companion built for your personal Motorola phone. Zero internet, zero cloud, zero trackers.",
                    style = ArOsTheme.typography.body,
                    color = ArOsTheme.colors.contentSecondary
                )
            }

            items(features) { feature ->
                ArOsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = feature.icon,
                            contentDescription = null,
                            tint = ArOsTheme.colors.accent,
                            modifier = Modifier.padding(end = 12.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = feature.title,
                                    style = ArOsTheme.typography.headline,
                                    color = ArOsTheme.colors.contentPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "• ${feature.status}",
                                    style = ArOsTheme.typography.caption,
                                    color = if (feature.status == "Active") ArOsTheme.colors.success else ArOsTheme.colors.warning
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = feature.subtitle,
                                style = ArOsTheme.typography.caption,
                                color = ArOsTheme.colors.contentSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        ArOsButton(
                            text = "Open",
                            onClick = { onNavigateTo(feature.destination) },
                            variant = ArOsButtonVariant.Secondary,
                            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
                        )
                    }
                }
            }
        }
    }
}
