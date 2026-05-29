// File: app/src/main/java/com/miui/dynamicisland/manager/IslandStateManager.kt
// Purpose: Manages state priority, timeouts, and expansion
// Hinglish: Is file mein hum island ki states aur unka timing manage karte hain.

package com.miui.dynamicisland.manager

import com.miui.dynamicisland.ui.states.IslandState
import com.miui.dynamicisland.ui.states.withExpanded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class IslandStateManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: IslandStateManager? = null

        fun getInstance(): IslandStateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: IslandStateManager().also { INSTANCE = it }
            }
        }
    }

    private data class ActiveState(val state: IslandState, val sequence: Long)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val activeStates = LinkedHashMap<KClass<out IslandState>, ActiveState>()
    private val timeoutJobs = mutableMapOf<KClass<out IslandState>, Job>()

    private val _currentState = MutableStateFlow<IslandState>(IslandState.Idle)
    val currentState: StateFlow<IslandState> = _currentState.asStateFlow()

    private var sequenceCounter = 0L

    fun pushState(state: IslandState) {
        val key = state::class
        timeoutJobs.remove(key)?.cancel()

        sequenceCounter += 1L
        activeStates[key] = ActiveState(state = state, sequence = sequenceCounter)

        if (state.durationMs != Long.MAX_VALUE && !state.isExpanded) {
            timeoutJobs[key] = scope.launch {
                delay(state.durationMs)
                removeState(key)
            }
        }
        updateCurrentState()
    }

    fun removeState(stateType: Class<out IslandState>) { removeState(stateType.kotlin) }

    fun removeState(stateType: KClass<out IslandState>) {
        timeoutJobs.remove(stateType)?.cancel()
        activeStates.remove(stateType)
        updateCurrentState()
    }

    fun expandCurrentState(ms: Long = 5000L) {
        val current = _currentState.value
        if (current is IslandState.Idle || current.isExpanded) return

        val expandedState = current.withExpanded(true)
        pushState(expandedState)

        // Auto-collapse for non-media states (Exclude Notifications as requested)
        if (current !is IslandState.Media && current !is IslandState.Call && current !is IslandState.Notification) {
            scope.launch {
                delay(ms)
                val collapsedState = current.withExpanded(false)
                pushState(collapsedState)
            }
        }
    }

    fun collapseCurrentState() {
        val current = _currentState.value
        if (!current.isExpanded) return
        val collapsedState = current.withExpanded(false)
        pushState(collapsedState)
    }

    fun clearAll() {
        timeoutJobs.values.forEach { it.cancel() }
        timeoutJobs.clear()
        activeStates.clear()
        _currentState.value = IslandState.Idle
    }

    private fun updateCurrentState() {
        val nextState = activeStates.values
            .maxWithOrNull(compareBy<ActiveState> { it.state.priority }.thenBy { it.sequence })
            ?.state ?: IslandState.Idle
        _currentState.value = nextState
    }
}
