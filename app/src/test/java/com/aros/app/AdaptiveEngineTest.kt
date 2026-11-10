package com.aros.app

import com.aros.app.feature.adaptive.data.AdaptiveInsightDao
import com.aros.app.feature.adaptive.data.AdaptiveInsightEntity
import com.aros.app.feature.adaptive.domain.AdaptiveEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveEngineTest {

    private val fakeDao = object : AdaptiveInsightDao {
        override fun getVisibleInsightsFlow(): Flow<List<AdaptiveInsightEntity>> = emptyFlow()
        override suspend fun getAllList(): List<AdaptiveInsightEntity> = emptyList()
        override suspend fun insert(entity: AdaptiveInsightEntity): Long = 1L
        override suspend fun dismissInsight(id: Long) {}
        override suspend fun clearAll() {}
    }

    private val engine = AdaptiveEngine(
        dao = fakeDao,
        ioDispatcher = Dispatchers.Unconfined,
        scope = CoroutineScope(Dispatchers.Unconfined)
    )

    @Test
    fun testU16_ComputesWeightedSovereigntyScore() {
        val breakdown = engine.computeSovereigntyScore(
            routineRatio = 1.0f, // 100
            focusMinutes = 60,   // 100
            guardianScore = 100, // 100
            intentionalRatio = 100 // 100
        )
        assertEquals(100, breakdown.overallScore)
        assertEquals(100, breakdown.routineConsistencyScore)
        assertEquals(100, breakdown.focusMasteryScore)
        assertEquals(100, breakdown.digitalHygieneScore)
        assertEquals(100, breakdown.intentionalRatioScore)
    }

    @Test
    fun testU16_ComputesPartialSovereigntyScore() {
        // Routine: 50% * 0.25 = 12.5
        // Focus: 30m/60m = 50% * 0.25 = 12.5
        // Hygiene: 80 * 0.20 = 16.0
        // Intentional: 70 * 0.30 = 21.0
        // Total expected = 12.5 + 12.5 + 16.0 + 21.0 = 62
        val breakdown = engine.computeSovereigntyScore(
            routineRatio = 0.5f,
            focusMinutes = 30,
            guardianScore = 80,
            intentionalRatio = 70
        )
        assertEquals(62, breakdown.overallScore)
        assertEquals(50, breakdown.routineConsistencyScore)
        assertEquals(50, breakdown.focusMasteryScore)
        assertEquals(80, breakdown.digitalHygieneScore)
        assertEquals(70, breakdown.intentionalRatioScore)
    }

    @Test
    fun testU16_ClampsFloorToTen() {
        val breakdown = engine.computeSovereigntyScore(
            routineRatio = 0f,
            focusMinutes = 0,
            guardianScore = 0,
            intentionalRatio = 0
        )
        assertTrue(breakdown.overallScore >= 10)
    }
}
