package com.miui.dynamicisland.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.ui.states.IslandState

@Composable
fun TimerControls(
    state: IslandState.Timer,
    onPauseResume: () -> Unit,
    onReset: () -> Unit,
    onSecondaryAction: () -> Unit, // Lap for Stopwatch, +1:00 for Countdown
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset Button
        ControlButton(
            icon = Icons.Default.Refresh,
            backgroundColor = Color(0xFF2C2C2E),
            onClick = onReset
        )

        // Secondary Action (Lap or +1:00)
        if (state.mode is IslandState.Timer.TimerMode.Stopwatch) {
            ControlButton(
                icon = Icons.Default.Flag,
                backgroundColor = Color(0xFF2C2C2E),
                onClick = onSecondaryAction
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2C2C2E))
                    .clickable { onSecondaryAction() },
                contentAlignment = Alignment.Center
            ) {
                Text("+1m", color = Color.White, fontSize = 12.sp)
            }
        }

        // Play/Pause Button
        val isRunning = state.status is IslandState.Timer.TimerStatus.Running
        ControlButton(
            icon = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
            backgroundColor = if (isRunning) Color(0xFFFF9500) else Color(0xFF34C759),
            onClick = onPauseResume,
            size = 48.dp
        )
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 44.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}
