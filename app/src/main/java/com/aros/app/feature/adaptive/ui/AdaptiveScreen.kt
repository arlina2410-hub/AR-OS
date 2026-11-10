package com.aros.app.feature.adaptive.ui

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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.components.ArOsCard
import com.aros.app.designsystem.components.ArOsEmptyState
import com.aros.app.designsystem.components.ArOsGroup
import com.aros.app.designsystem.components.ArOsScreenScaffold
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.adaptive.data.AdaptiveInsightEntity
import com.aros.app.feature.adaptive.domain.SovereigntyScoreBreakdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveScreen(
    scoreBreakdown: SovereigntyScoreBreakdown,
    insights: List<AdaptiveInsightEntity>,
    onDismissInsight: (Long) -> Unit
) {
    val score = scoreBreakdown.overallScore
    val scoreColor = when {
        score >= 80 -> ArOsTheme.colors.success
        score >= 60 -> ArOsTheme.colors.warning
        else -> ArOsTheme.colors.danger
    }

    ArOsScreenScaffold(
        title = "Adaptive Insights"
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.md)
        ) {
            // 1. Overall Sovereignty Score Dial
            item {
                ArOsCard(
                    modifier = Modifier
                        .padding(horizontal = ArOsTheme.spacing.screenMargin)
                        .testTag("sovereignty_score_card")
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SOVEREIGNTY SCORE",
                            style = ArOsTheme.typography.caption,
                            color = ArOsTheme.colors.contentTertiary
                        )

                        Spacer(modifier = Modifier.height(ArOsTheme.spacing.md))

                        Box(
                            modifier = Modifier
                                .size(120.dp)
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
                                    text = "Sovereign State",
                                    style = ArOsTheme.typography.caption,
                                    color = ArOsTheme.colors.contentTertiary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(ArOsTheme.spacing.md))
                        Text(
                            text = "Heuristic Synthesis: Focus (25%) + Intentionality (30%) + Routine (25%) + Privacy (20%)",
                            style = ArOsTheme.typography.footnote,
                            color = ArOsTheme.colors.contentSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // 2. Sub-Score Pillars
            item {
                ArOsGroup(header = "Pillar Performance") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ArOsTheme.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.md)
                    ) {
                        ScorePillarRow(
                            label = "Activity Intentionality",
                            score = scoreBreakdown.intentionalRatioScore,
                            color = ArOsTheme.colors.accent
                        )

                        ScorePillarRow(
                            label = "Deep Focus Mastery",
                            score = scoreBreakdown.focusMasteryScore,
                            color = Color(0xFF8B5CF6)
                        )

                        ScorePillarRow(
                            label = "Daily Routine Adherence",
                            score = scoreBreakdown.routineConsistencyScore,
                            color = ArOsTheme.colors.success
                        )

                        ScorePillarRow(
                            label = "Device Privacy Hygiene",
                            score = scoreBreakdown.digitalHygieneScore,
                            color = ArOsTheme.colors.warning
                        )
                    }
                }
            }

            // 3. Offline Heuristics Notice
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
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Intelligence",
                            tint = ArOsTheme.colors.accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(ArOsTheme.spacing.sm))
                        Text(
                            text = "Adaptive Intelligence executes 100% on-device heuristics. No cloud LLMs, background network calls, or third-party telemetry.",
                            style = ArOsTheme.typography.footnote,
                            color = ArOsTheme.colors.contentSecondary,
                            lineHeight = ArOsTheme.typography.footnote.lineHeight
                        )
                    }
                }
            }

            // 4. Localized Insights Stream
            if (insights.isEmpty()) {
                item {
                    ArOsEmptyState(
                        icon = Icons.Default.Psychology,
                        title = "No Adaptive Observations Yet",
                        description = "As you complete routines and log focus sessions, heuristic observations will appear here."
                    )
                }
            } else {
                item {
                    ArOsGroup(header = "Adaptive Observations") {
                        insights.forEachIndexed { index, insight ->
                            val isPositive = insight.scoreImpact >= 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = ArOsTheme.spacing.md,
                                        vertical = ArOsTheme.spacing.sm
                                    ),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isPositive) ArOsTheme.colors.success.copy(alpha = 0.15f) else ArOsTheme.colors.warning.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isPositive) ArOsTheme.colors.success else ArOsTheme.colors.warning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(ArOsTheme.spacing.md))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = insight.headline,
                                            style = ArOsTheme.typography.headline,
                                            color = ArOsTheme.colors.contentPrimary
                                        )

                                        Box(
                                            modifier = Modifier
                                                .clip(ArOsTheme.shapes.xs)
                                                .background(
                                                    if (isPositive) ArOsTheme.colors.success.copy(alpha = 0.15f) else ArOsTheme.colors.danger.copy(alpha = 0.15f)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${if (isPositive) "+" else ""}${insight.scoreImpact}",
                                                color = if (isPositive) ArOsTheme.colors.success else ArOsTheme.colors.danger,
                                                style = ArOsTheme.typography.caption
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = insight.body,
                                        style = ArOsTheme.typography.footnote,
                                        color = ArOsTheme.colors.contentSecondary,
                                        lineHeight = ArOsTheme.typography.footnote.lineHeight
                                    )
                                }

                                IconButton(
                                    onClick = { onDismissInsight(insight.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = ArOsTheme.colors.contentTertiary,
                                        modifier = Modifier.size(16.dp)
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

@Composable
private fun ScorePillarRow(
    label: String,
    score: Int,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = ArOsTheme.typography.body,
                color = ArOsTheme.colors.contentPrimary
            )
            Text(
                text = "$score%",
                style = ArOsTheme.typography.numericBody,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (score.toFloat() / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(ArOsTheme.shapes.pill),
            color = color,
            trackColor = ArOsTheme.colors.surfaceRaised
        )
    }
}
