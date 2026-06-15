package com.miui.dynamicisland.ui.timer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.miui.dynamicisland.ui.components.TimerSlot
import com.miui.dynamicisland.ui.island.TimerAction
import com.miui.dynamicisland.ui.states.IslandState

@Composable
fun TimerPill(
    state: IslandState.Timer,
    slot: TimerSlot,
    isExpanded: Boolean = false,
    onAction: (TimerAction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (isExpanded) {
        ExpandedTimerView(state, onAction, modifier)
    } else {
        CompactTimerView(state, slot, modifier)
    }
}
