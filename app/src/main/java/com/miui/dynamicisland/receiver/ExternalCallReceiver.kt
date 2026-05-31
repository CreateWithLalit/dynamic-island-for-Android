package com.miui.dynamicisland.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.miui.dynamicisland.manager.IslandStateManager
import com.miui.dynamicisland.ui.states.IslandState

/**
 * Listens for call updates from the external Dialer App.
 * Hinglish: Ye receiver doosre dialer app se data receive karke island ko update karega.
 */
class ExternalCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.miui.dynamicisland.CALL_UPDATE") return

        val state = intent.getStringExtra("state") ?: "DISCONNECTED"
        val number = intent.getStringExtra("number") ?: "Unknown"
        val name = intent.getStringExtra("name")
        val duration = intent.getLongExtra("duration", 0L)

        val stateManager = IslandStateManager.getInstance()

        when (state) {
            "RINGING" -> {
                stateManager.pushState(
                    IslandState.Call(
                        callerName = name ?: number,
                        callerSubtext = if (name != null) number else "Incoming Call",
                        isIncoming = true,
                        isOngoing = false,
                        isExpanded = true
                    )
                )
            }
            "ONGOING" -> {
                val current = stateManager.currentState.value
                val wasExpanded = (current as? IslandState.Call)?.isExpanded ?: false
                stateManager.pushState(
                    IslandState.Call(
                        callerName = name ?: number,
                        callerSubtext = if (name != null) number else "Ongoing Call",
                        isIncoming = false,
                        isOngoing = true,
                        duration = duration,
                        isExpanded = wasExpanded
                    )
                )
            }
            "DISCONNECTED" -> {
                stateManager.removeState(IslandState.Call::class)
            }
        }
    }
}
