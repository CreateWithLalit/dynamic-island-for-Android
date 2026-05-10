package com.miui.dynamicisland.ui.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import com.miui.dynamicisland.manager.IslandCalibration
import com.miui.dynamicisland.manager.IslandStateManager
import com.miui.dynamicisland.ui.island.DynamicIsland

interface IslandBoundsListener {
    fun updatePillBounds(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    )
}

@Composable
fun IslandOverlayLayout(
    calibration: IslandCalibration = IslandCalibration.default(),
    boundsListener: IslandBoundsListener? = null,
    stateManager: IslandStateManager = IslandStateManager.getInstance()
) {
    val currentState by stateManager.currentState.collectAsState()

    Box(
        modifier = Modifier
            .wrapContentWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.TopCenter
    ) {
        DynamicIsland(
            state = currentState,
            calibration = calibration,
            modifier = Modifier.onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                val size = coordinates.size

                boundsListener?.updatePillBounds(
                    left = position.x,
                    top = position.y,
                    right = position.x + size.width,
                    bottom = position.y + size.height
                )
            }
        )
    }
}
