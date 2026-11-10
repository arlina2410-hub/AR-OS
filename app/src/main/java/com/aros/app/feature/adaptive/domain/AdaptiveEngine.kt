package com.aros.app.feature.adaptive.domain

import com.aros.app.feature.adaptive.data.AdaptiveInsightDao
import com.aros.app.feature.adaptive.data.AdaptiveInsightEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SovereigntyScoreBreakdown(
    val overallScore: Int = 88, // 0..100
    val routineConsistencyScore: Int = 85, // 0..100
    val focusMasteryScore: Int = 90, // 0..100
    val digitalHygieneScore: Int = 88, // 0..100
    val intentionalRatioScore: Int = 90 // 0..100
)

class AdaptiveEngine(
    private val dao: AdaptiveInsightDao,
    private val ioDispatcher: CoroutineDispatcher,
    scope: CoroutineScope
) {
    val visibleInsights: Flow<List<AdaptiveInsightEntity>> = dao.getVisibleInsightsFlow()

    private val _scoreBreakdown = MutableStateFlow(SovereigntyScoreBreakdown())
    val scoreBreakdown: StateFlow<SovereigntyScoreBreakdown> = _scoreBreakdown.asStateFlow()

    init {
        scope.launch {
            if (dao.getAllList().isEmpty()) {
                seedInitialInsights()
            }
        }
    }

    private suspend fun seedInitialInsights() = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val defaultInsights = listOf(
            AdaptiveInsightEntity(
                timestamp = now - 3600_000L,
                headline = "High Intentionality Period Detected",
                body = "Your phone opens this morning were 88% intentional, outperforming your 7-day average.",
                scoreImpact = +5,
                isDismissed = false
            ),
            AdaptiveInsightEntity(
                timestamp = now - (7200_000L * 2),
                headline = "Routine Cadence Optimal",
                body = "Morning Ignition routine completed 3 days in a row without interruptions.",
                scoreImpact = +3,
                isDismissed = false
            ),
            AdaptiveInsightEntity(
                timestamp = now - (3600_000L * 8),
                headline = "Midday Screen Friction Opportunity",
                body = "Activity ledger shows peak impulsive screen opens between 2 PM and 3 PM. Recommended: enable Breath Gate on social apps.",
                scoreImpact = -2,
                isDismissed = false
            )
        )
        defaultInsights.forEach { dao.insert(it) }
    }

    fun computeSovereigntyScore(
        routineRatio: Float, // 0.0..1.0
        focusMinutes: Int,
        guardianScore: Int,
        intentionalRatio: Int
    ): SovereigntyScoreBreakdown {
        val routineScore = (routineRatio * 100).toInt().coerceIn(0, 100)
        val focusScore = ((focusMinutes.toFloat() / 60f) * 100).toInt().coerceIn(0, 100)
        val hygieneScore = guardianScore.coerceIn(0, 100)
        val intentionalScore = intentionalRatio.coerceIn(0, 100)

        // Weighted average: 30% Intentionality + 25% Focus + 25% Routine + 20% Hygiene
        val overall = (
                (intentionalScore * 0.30f) +
                        (focusScore * 0.25f) +
                        (routineScore * 0.25f) +
                        (hygieneScore * 0.20f)
                ).toInt().coerceIn(10, 100)

        val breakdown = SovereigntyScoreBreakdown(
            overallScore = overall,
            routineConsistencyScore = routineScore,
            focusMasteryScore = focusScore,
            digitalHygieneScore = hygieneScore,
            intentionalRatioScore = intentionalScore
        )
        _scoreBreakdown.value = breakdown
        return breakdown
    }

    suspend fun dismissInsight(id: Long) = withContext(ioDispatcher) {
        dao.dismissInsight(id)
    }

    suspend fun generateDynamicInsight(headline: String, body: String, impact: Int) = withContext(ioDispatcher) {
        dao.insert(
            AdaptiveInsightEntity(
                timestamp = System.currentTimeMillis(),
                headline = headline,
                body = body,
                scoreImpact = impact,
                isDismissed = false
            )
        )
    }
}
