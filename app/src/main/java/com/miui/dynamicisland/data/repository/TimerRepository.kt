package com.miui.dynamicisland.data.repository

import android.content.Context
import com.miui.dynamicisland.manager.IslandStateManager
import com.miui.dynamicisland.ui.states.IslandState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimerRepository(private val context: Context) {
    private val stateManager = IslandStateManager.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null

    // State
    private var mode: IslandState.Timer.TimerMode = IslandState.Timer.TimerMode.Idle
    private var status: IslandState.Timer.TimerStatus = IslandState.Timer.TimerStatus.Paused
    private var startTime: Long = 0L
    private var pausedAt: Long = 0L

    fun startCountdown(durationMs: Long, label: String? = null) {
        timerJob?.cancel()
        startTime = System.currentTimeMillis()
        val totalDuration = durationMs
        mode = IslandState.Timer.TimerMode.Countdown(totalDuration, totalDuration, label)
        status = IslandState.Timer.TimerStatus.Running
        
        startJob()
    }

    fun startStopwatch() {
        timerJob?.cancel()
        startTime = System.currentTimeMillis()
        mode = IslandState.Timer.TimerMode.Stopwatch(0L)
        status = IslandState.Timer.TimerStatus.Running
        
        startJob()
    }

    private fun startJob() {
        timerJob = scope.launch {
            while (status == IslandState.Timer.TimerStatus.Running) {
                val now = System.currentTimeMillis()
                val currentMode = mode
                when (currentMode) {
                    is IslandState.Timer.TimerMode.Countdown -> {
                        val elapsed = now - startTime
                        val remaining = (currentMode.totalDurationMs - elapsed).coerceAtLeast(0L)
                        mode = currentMode.copy(remainingMs = remaining)
                        
                        if (remaining <= 0) {
                            status = IslandState.Timer.TimerStatus.Finished
                            updateIsland()
                            stateManager.expandCurrentState(5000L)
                            break
                        }
                    }
                    is IslandState.Timer.TimerMode.Stopwatch -> {
                        val elapsed = now - startTime
                        mode = currentMode.copy(elapsedMs = elapsed)
                    }
                    else -> break
                }
                updateIsland()
                delay(if (mode is IslandState.Timer.TimerMode.Stopwatch) 10 else 100)
            }
        }
    }

    fun pauseResume() {
        if (status == IslandState.Timer.TimerStatus.Running) {
            status = IslandState.Timer.TimerStatus.Paused
            pausedAt = System.currentTimeMillis()
            timerJob?.cancel()
        } else if (status == IslandState.Timer.TimerStatus.Paused) {
            status = IslandState.Timer.TimerStatus.Running
            val pauseDuration = System.currentTimeMillis() - pausedAt
            startTime += pauseDuration
            startJob()
        }
        updateIsland()
    }

    fun reset() {
        timerJob?.cancel()
        mode = IslandState.Timer.TimerMode.Idle
        status = IslandState.Timer.TimerStatus.Paused
        stateManager.removeState(IslandState.Timer::class.java)
    }

    fun lap() {
        val currentMode = mode
        if (currentMode is IslandState.Timer.TimerMode.Stopwatch) {
            val currentLaps = currentMode.laps.toMutableList()
            currentLaps.add(currentMode.elapsedMs)
            mode = currentMode.copy(laps = currentLaps)
            updateIsland()
        }
    }

    fun addOneMinute() {
        val currentMode = mode
        if (currentMode is IslandState.Timer.TimerMode.Countdown) {
            mode = currentMode.copy(
                totalDurationMs = currentMode.totalDurationMs + 60000,
                remainingMs = currentMode.remainingMs + 60000
            )
            // If it was finished, restart it
            if (status == IslandState.Timer.TimerStatus.Finished) {
                status = IslandState.Timer.TimerStatus.Running
                startTime = System.currentTimeMillis() - (currentMode.totalDurationMs - currentMode.remainingMs)
                startJob()
            }
            updateIsland()
        }
    }

    private fun updateIsland() {
        val current = stateManager.currentState.value
        val isExpanded = (current as? IslandState.Timer)?.isExpanded ?: false
        stateManager.pushState(IslandState.Timer(mode, status, isExpanded))
    }
}
