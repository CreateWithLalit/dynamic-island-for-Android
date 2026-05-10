// File: app/src/main/java/com/miui/dynamicisland/manager/PriorityManager.kt
// No changes required – works fine with current priorities.

package com.miui.dynamicisland.manager

import com.miui.dynamicisland.ui.states.IslandState

class PriorityManager {
    companion object {
        const val PRIORITY_CRITICAL = 40
        const val PRIORITY_HIGH = 25
        const val PRIORITY_MEDIUM = 15
        const val PRIORITY_LOW = 10
        const val PRIORITY_BACKGROUND = 5
    }

    fun shouldInterrupt(current: IslandState, incoming: IslandState): Boolean {
        if (current is IslandState.Call && incoming !is IslandState.Call) return false
        if (incoming.priority >= PRIORITY_CRITICAL) return true
        if (current::class == incoming::class) return true
        return incoming.priority > current.priority
    }

    fun getOptimalDuration(state: IslandState): Long {
        return when (state) {
            is IslandState.Charging -> if (state.isCharging) 3000L else 2000L
            is IslandState.Silent -> 2000L
            is IslandState.Notification -> if (isMessagingApp(state.packageName)) 5000L else 3500L
            is IslandState.Volume -> 1500L
            is IslandState.Bluetooth -> 2500L
            else -> state.durationMs
        }
    }

    private fun isMessagingApp(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower.contains("whatsapp") || lower.contains("telegram") ||
                lower.contains("messages") || lower.contains("messenger")
    }

    fun canCoexist(state1: IslandState, state2: IslandState): Boolean {
        return (state1 is IslandState.Media && state2 is IslandState.Notification) ||
                (state1 is IslandState.Notification && state2 is IslandState.Media)
    }
}