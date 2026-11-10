package com.aros.app.feature.focus.domain

import com.aros.app.feature.focus.data.FocusDao
import com.aros.app.feature.focus.data.FocusSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

enum class FocusMode(val label: String, val minutes: Int) {
    DEEP_WORK("Deep Work", 45),
    POMODORO("Pomodoro", 25),
    SHORT_RESET("Focus Sprint", 15),
    BREATH("Mindful Reset", 5)
}

sealed interface FocusTimerState {
    data class Idle(val mode: FocusMode = FocusMode.POMODORO) : FocusTimerState
    data class Running(val secondsRemaining: Int, val totalSeconds: Int, val mode: FocusMode) : FocusTimerState
    data class Paused(val secondsRemaining: Int, val totalSeconds: Int, val mode: FocusMode) : FocusTimerState
    data class Completed(val durationMinutes: Int, val mode: FocusMode) : FocusTimerState
}

class FocusController(
    private val dao: FocusDao,
    private val scope: CoroutineScope
) {
    private val _timerState = MutableStateFlow<FocusTimerState>(FocusTimerState.Idle(FocusMode.POMODORO))
    val timerState: StateFlow<FocusTimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartTime: Long = 0L
    private var interruptionCount: Int = 0

    val recentSessions = dao.getAllFlow()

    val todayFocusMinutes = dao.getTodayFocusMinutesFlow(getStartOfTodayMillis())

    fun selectMode(mode: FocusMode) {
        if (_timerState.value is FocusTimerState.Idle) {
            _timerState.value = FocusTimerState.Idle(mode)
        }
    }

    fun start() {
        val current = _timerState.value
        val mode = when (current) {
            is FocusTimerState.Idle -> current.mode
            is FocusTimerState.Completed -> current.mode
            is FocusTimerState.Paused -> current.mode
            else -> FocusMode.POMODORO
        }

        val totalSecs = mode.minutes * 60
        sessionStartTime = System.currentTimeMillis()
        interruptionCount = 0

        _timerState.value = FocusTimerState.Running(
            secondsRemaining = totalSecs,
            totalSeconds = totalSecs,
            mode = mode
        )

        startTimerLoop(totalSecs, totalSecs, mode)
    }

    fun startTimer() = start()

    fun pause() {
        val current = _timerState.value
        if (current is FocusTimerState.Running) {
            timerJob?.cancel()
            interruptionCount++
            _timerState.value = FocusTimerState.Paused(
                secondsRemaining = current.secondsRemaining,
                totalSeconds = current.totalSeconds,
                mode = current.mode
            )
        }
    }

    fun pauseTimer() = pause()

    fun resume() {
        val current = _timerState.value
        if (current is FocusTimerState.Paused) {
            _timerState.value = FocusTimerState.Running(
                secondsRemaining = current.secondsRemaining,
                totalSeconds = current.totalSeconds,
                mode = current.mode
            )
            startTimerLoop(current.secondsRemaining, current.totalSeconds, current.mode)
        }
    }

    fun resumeTimer() = resume()

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        val mode = when (val c = _timerState.value) {
            is FocusTimerState.Running -> c.mode
            is FocusTimerState.Paused -> c.mode
            is FocusTimerState.Completed -> c.mode
            is FocusTimerState.Idle -> c.mode
        }
        _timerState.value = FocusTimerState.Idle(mode)
    }

    fun cancelTimer() = cancel()

    fun recordManualInterruption() {
        interruptionCount++
    }

    fun logInterruption() = recordManualInterruption()

    private fun startTimerLoop(startSeconds: Int, totalSeconds: Int, mode: FocusMode) {
        timerJob?.cancel()
        timerJob = scope.launch {
            var remaining = startSeconds
            while (remaining > 0) {
                delay(1000L)
                remaining--
                val cur = _timerState.value
                if (cur is FocusTimerState.Running) {
                    _timerState.value = cur.copy(secondsRemaining = remaining)
                }
            }

            // Completed!
            _timerState.value = FocusTimerState.Completed(mode.minutes, mode)
            dao.insert(
                FocusSessionEntity(
                    startTime = sessionStartTime,
                    endTime = System.currentTimeMillis(),
                    durationMinutes = mode.minutes,
                    sessionType = mode.name,
                    completed = true,
                    interruptions = interruptionCount
                )
            )
        }
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
