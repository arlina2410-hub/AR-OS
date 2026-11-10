package com.aros.app.feature.routine.domain

import com.aros.app.feature.routine.data.RoutineDao
import com.aros.app.feature.routine.data.RoutineEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RoutineCategory(val label: String) {
    MORNING("Morning"),
    FOCUS("Focus Block"),
    EVENING("Wind-down"),
    HEALTH("Health & Body")
}

data class RoutineItem(
    val id: Long = 0,
    val title: String,
    val description: String,
    val timeMinutesOfDay: Int,
    val daysMask: Int = 127, // All days
    val durationMinutes: Int = 15,
    val completedToday: Boolean = false,
    val streakCount: Int = 0,
    val category: RoutineCategory = RoutineCategory.MORNING,
    val lastCompletedDate: String? = null
) {
    val formattedTime: String
        get() = com.aros.app.core.TimeFormatter.formatMinuteOfDay(timeMinutesOfDay)
}

class RoutineController(
    private val dao: RoutineDao,
    private val scope: CoroutineScope
) {
    val routines: Flow<List<RoutineItem>> = dao.getAllFlow().map { list ->
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        list.map { entity ->
            val isDoneToday = entity.completedToday && (entity.lastCompletedDate == todayStr)
            RoutineItem(
                id = entity.id,
                title = entity.title,
                description = entity.description,
                timeMinutesOfDay = entity.timeMinutesOfDay,
                daysMask = entity.daysMask,
                durationMinutes = entity.durationMinutes,
                completedToday = isDoneToday,
                streakCount = entity.streakCount,
                category = try { RoutineCategory.valueOf(entity.category) } catch (_: Exception) { RoutineCategory.MORNING },
                lastCompletedDate = entity.lastCompletedDate
            )
        }
    }

    init {
        scope.launch {
            // Seed starter sovereign routines if empty
            if (dao.getAllList().isEmpty()) {
                seedDefaultRoutines()
            }
        }
    }

    private suspend fun seedDefaultRoutines() {
        val defaults = listOf(
            RoutineEntity(
                title = "Morning Ignition",
                description = "Zero-screen wake up, hydration & breathwork",
                timeMinutesOfDay = 420, // 7:00 AM
                daysMask = 127,
                durationMinutes = 15,
                completedToday = false,
                streakCount = 3,
                category = RoutineCategory.MORNING.name
            ),
            RoutineEntity(
                title = "Deep Focus Block 1",
                description = "High-leverage uninterrupted creative work",
                timeMinutesOfDay = 540, // 9:00 AM
                daysMask = 62, // Mon-Fri
                durationMinutes = 90,
                completedToday = false,
                streakCount = 5,
                category = RoutineCategory.FOCUS.name
            ),
            RoutineEntity(
                title = "Midday Reset & Movement",
                description = "Disconnect from devices, stretch and sunlight",
                timeMinutesOfDay = 780, // 1:00 PM
                daysMask = 127,
                durationMinutes = 20,
                completedToday = false,
                streakCount = 2,
                category = RoutineCategory.HEALTH.name
            ),
            RoutineEntity(
                title = "Digital Sunset",
                description = "Screen lock-down, review daily ledger & wind-down",
                timeMinutesOfDay = 1290, // 9:30 PM
                daysMask = 127,
                durationMinutes = 30,
                completedToday = false,
                streakCount = 4,
                category = RoutineCategory.EVENING.name
            )
        )
        defaults.forEach { dao.insert(it) }
    }

    suspend fun toggleRoutineCompletion(routine: RoutineItem) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val newCompleted = !routine.completedToday
        val newStreak = if (newCompleted) {
            routine.streakCount + 1
        } else {
            (routine.streakCount - 1).coerceAtLeast(0)
        }
        val dateRecord = if (newCompleted) todayStr else routine.lastCompletedDate

        dao.updateCompletion(
            id = routine.id,
            completed = newCompleted,
            streak = newStreak,
            date = dateRecord
        )
    }

    suspend fun addRoutine(
        title: String,
        description: String,
        timeMinutesOfDay: Int,
        durationMinutes: Int,
        category: RoutineCategory
    ) {
        val entity = RoutineEntity(
            title = title.trim(),
            description = description.trim(),
            timeMinutesOfDay = timeMinutesOfDay,
            daysMask = 127,
            durationMinutes = durationMinutes,
            completedToday = false,
            streakCount = 0,
            category = category.name
        )
        dao.insert(entity)
    }

    suspend fun deleteRoutine(id: Long) {
        dao.deleteById(id)
    }
}
