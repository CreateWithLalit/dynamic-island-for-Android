package com.miui.dynamicisland.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.ui.island.TimerAction
import com.miui.dynamicisland.ui.states.IslandState
import java.util.Locale

@Composable
fun ExpandedTimerView(
    state: IslandState.Timer,
    onAction: (TimerAction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ─── Control Buttons ──────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isRunning = state.status is IslandState.Timer.TimerStatus.Running
            
            // Pause/Resume Button
            TimerControlButton(
                icon = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                backgroundColor = if (isRunning) Color(0xFFFF9500) else Color(0xFF34C759),
                contentColor = Color.Black,
                onClick = { onAction(TimerAction.PauseResume) }
            )

            // Close/Stop Button (X)
            TimerControlButton(
                icon = Icons.Default.Close,
                backgroundColor = Color(0xFF3A3A3C),
                contentColor = Color.White,
                onClick = { onAction(TimerAction.Reset) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // ─── Label & Time ─────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            val label = when (val mode = state.mode) {
                is IslandState.Timer.TimerMode.Countdown -> mode.label ?: "Timer"
                is IslandState.Timer.TimerMode.Stopwatch -> "Stopwatch"
                else -> ""
            }

            Text(
                text = label,
                color = Color(0xFFFF9500).copy(alpha = 0.8f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(end = 12.dp)
            )

            val timeText = when (val mode = state.mode) {
                is IslandState.Timer.TimerMode.Countdown -> formatTime(mode.remainingMs)
                is IslandState.Timer.TimerMode.Stopwatch -> formatStopwatchExpanded(mode.elapsedMs)
                else -> "0:00"
            }

            Text(
                text = timeText,
                color = Color(0xFFFF9500),
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Default
            )
        }
    }
}

@Composable
private fun TimerControlButton(
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun formatStopwatchExpanded(ms: Long): String {
    val centiseconds = (ms / 10) % 100
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    val hours = (ms / (1000 * 60 * 60))
    
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d.%02d", hours, minutes, seconds, centiseconds)
    } else {
        String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centiseconds)
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    val hours = (ms / (1000 * 60 * 60))
    
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
