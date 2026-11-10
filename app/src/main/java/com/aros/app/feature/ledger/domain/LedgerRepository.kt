package com.aros.app.feature.ledger.domain

import com.aros.app.feature.ledger.data.ActivityLedgerDao
import com.aros.app.feature.ledger.data.ActivityLedgerEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class DailyLedgerSummary(
    val totalPhoneUnlocks: Int = 1,
    val intentionalRatioPercent: Int = 85,
    val totalFocusMinutes: Int = 60,
    val hourlyActivityBuckets: List<Int> = List(24) { 0 }
)

class LedgerRepository(
    private val dao: ActivityLedgerDao,
    private val ioDispatcher: CoroutineDispatcher,
    scope: CoroutineScope
) {
    val recentEntries: Flow<List<ActivityLedgerEntity>> = dao.getAllFlow()

    val dailySummaryFlow: Flow<DailyLedgerSummary> = recentEntries.map {
        getDailySummary()
    }

    init {
        scope.launch {
            if (dao.getEventsSince(0).isEmpty()) {
                seedDemoLedgerData()
            }
        }
    }

    private suspend fun seedDemoLedgerData() = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val oneHour = 3600_000L
        val sampleEvents = listOf(
            ActivityLedgerEntity(timestamp = now - (oneHour * 10), eventCategory = "ROUTINE_DONE", durationSeconds = 900, appLabel = "Morning Ignition", isIntentional = true),
            ActivityLedgerEntity(timestamp = now - (oneHour * 8), eventCategory = "FOCUS_START", durationSeconds = 2700, appLabel = "Deep Work Block", isIntentional = true),
            ActivityLedgerEntity(timestamp = now - (oneHour * 6), eventCategory = "PHONE_UNLOCK", durationSeconds = 120, appLabel = "Quick Check", isIntentional = false),
            ActivityLedgerEntity(timestamp = now - (oneHour * 4), eventCategory = "DISCIPLINE_PASS", durationSeconds = 60, appLabel = "Breath Gate Passed", isIntentional = true),
            ActivityLedgerEntity(timestamp = now - (oneHour * 2), eventCategory = "PHONE_UNLOCK", durationSeconds = 45, appLabel = "Impulsive Glance", isIntentional = false),
            ActivityLedgerEntity(timestamp = now - (oneHour * 1), eventCategory = "FOCUS_START", durationSeconds = 1500, appLabel = "Pomodoro Sprint", isIntentional = true)
        )
        sampleEvents.forEach { dao.insert(it) }
    }

    suspend fun logActivity(
        category: String,
        durationSeconds: Long,
        appLabel: String,
        isIntentional: Boolean
    ) = withContext(ioDispatcher) {
        dao.insert(
            ActivityLedgerEntity(
                timestamp = System.currentTimeMillis(),
                eventCategory = category,
                durationSeconds = durationSeconds,
                appLabel = appLabel,
                isIntentional = isIntentional
            )
        )
    }

    suspend fun logIntentionalEvent(label: String, durationSeconds: Long) {
        logActivity(
            category = "INTENTIONAL",
            durationSeconds = durationSeconds,
            appLabel = label,
            isIntentional = true
        )
    }

    suspend fun getDailySummary(): DailyLedgerSummary = withContext(ioDispatcher) {
        val startOfToday = getStartOfTodayMillis()
        val eventsToday = dao.getEventsSince(startOfToday)

        val buckets = IntArray(24) { 0 }
        var unlocks = 0
        var intentionalCount = 0
        var focusMinutes = 0

        val cal = Calendar.getInstance()
        for (event in eventsToday) {
            cal.timeInMillis = event.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
            buckets[hour]++

            if (event.eventCategory == "PHONE_UNLOCK") {
                unlocks++
            }
            if (event.isIntentional) {
                intentionalCount++
            }
            if (event.eventCategory == "FOCUS_START") {
                focusMinutes += (event.durationSeconds / 60).toInt()
            }
        }

        val total = eventsToday.size
        val ratio = if (total > 0) ((intentionalCount * 100) / total).coerceIn(0, 100) else 100

        DailyLedgerSummary(
            totalPhoneUnlocks = unlocks.coerceAtLeast(1),
            intentionalRatioPercent = ratio,
            totalFocusMinutes = focusMinutes,
            hourlyActivityBuckets = buckets.toList()
        )
    }

    private fun getStartOfTodayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
