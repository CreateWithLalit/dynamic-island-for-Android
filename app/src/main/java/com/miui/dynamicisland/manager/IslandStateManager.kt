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
    private var expandCollapseJob: Job? = null

    private val _currentState = MutableStateFlow<IslandState>(IslandState.Idle)
    val currentState: StateFlow<IslandState> = _currentState.asStateFlow()

    private val _allStates = MutableStateFlow<List<IslandState>>(emptyList())
    val allStates: StateFlow<List<IslandState>> = _allStates.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

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

    fun expandCurrentState(ms: Long = 5000L, forceAutoCollapse: Boolean = false) {
        val current = _currentState.value
        if (current is IslandState.Idle) return

        val expandedState = current.withExpanded(true)
        pushState(expandedState)

        expandCollapseJob?.cancel()

        // Auto-collapse logic
        val shouldCollapse = forceAutoCollapse || (current !is IslandState.Media && current !is IslandState.Call && current !is IslandState.Notification)

        if (shouldCollapse) {
            expandCollapseJob = scope.launch {
                delay(ms)
                collapseCurrentState()
            }
        }
    }

    fun collapseCurrentState() {
        expandCollapseJob?.cancel()
        expandCollapseJob = null
        _currentIndex.value = 0
        val current = _currentState.value
        if (!current.isExpanded) return
        val collapsedState = current.withExpanded(false)
        pushState(collapsedState)
    }

    fun nextState() {
        val states = _allStates.value
        if (states.size <= 1) return
        val nextIdx = (_currentIndex.value + 1) % states.size
        _currentIndex.value = nextIdx
        val isCurrentlyExpanded = _currentState.value.isExpanded
        _currentState.value = states[nextIdx].withExpanded(isCurrentlyExpanded)
    }

    fun previousState() {
        val states = _allStates.value
        if (states.size <= 1) return
        val prevIdx = if (_currentIndex.value <= 0) states.size - 1 else _currentIndex.value - 1
        _currentIndex.value = prevIdx
        val isCurrentlyExpanded = _currentState.value.isExpanded
        _currentState.value = states[prevIdx].withExpanded(isCurrentlyExpanded)
    }

    fun clearAll() {
        expandCollapseJob?.cancel()
        expandCollapseJob = null
        timeoutJobs.values.forEach { it.cancel() }
        timeoutJobs.clear()
        activeStates.clear()
        _currentState.value = IslandState.Idle
    }

    private fun updateCurrentState() {
        val sortedStates = activeStates.values
            .sortedWith(compareByDescending<ActiveState> { it.state.priority }.thenByDescending { it.sequence })
            .map { it.state }

        _allStates.value = sortedStates

        if (sortedStates.isEmpty()) {
            _currentState.value = IslandState.Idle
            _currentIndex.value = 0
            return
        }

        // Keep current index within bounds if it was manually changed (e.g. while expanded)
        val idx = _currentIndex.value.coerceIn(0, (sortedStates.size - 1).coerceAtLeast(0))
        _currentIndex.value = idx
        
        // Use the state as it is in the activeStates map. 
        // This allows expandCurrentState to work by pushing an expanded version.
        _currentState.value = sortedStates[idx]
    }
}
