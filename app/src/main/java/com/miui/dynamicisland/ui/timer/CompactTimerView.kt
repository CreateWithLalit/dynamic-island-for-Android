package com.miui.dynamicisland.ui.timer

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.ui.components.TimerSlot
import com.miui.dynamicisland.ui.states.IslandState
import java.util.Locale

@Composable
fun CompactTimerView(
    state: IslandState.Timer,
    slot: TimerSlot,
    modifier: Modifier = Modifier
) {
    val isUrgent = if (state.mode is IslandState.Timer.TimerMode.Countdown) {
        state.mode.remainingMs < 10000 && state.status is IslandState.Timer.TimerStatus.Running
    } else false

    val isFinished = state.status is IslandState.Timer.TimerStatus.Finished

    val backgroundColor = when {
        isFinished -> Color(0xFFFF3B30).copy(alpha = 0.3f)
        isUrgent -> Color(0xFFFF9500).copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    // Urgency Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "urgency_pulse")
    val pulseAlpha by if (isUrgent || isFinished) {
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    } else {
        rememberUpdatedState(0f)
    }

    Row(
        modifier = modifier
            .fillMaxHeight()
            .background(if (isUrgent || isFinished) backgroundColor.copy(alpha = pulseAlpha) else Color.Transparent)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (slot == TimerSlot.LEFT) Arrangement.Start else Arrangement.End
    ) {
        if (slot == TimerSlot.LEFT) {
            TimerIcon(state)
        } else {
            TimerText(state)
            Spacer(modifier = Modifier.width(6.dp))
            TimerIcon(state)
        }

        if (slot == TimerSlot.LEFT) {
            Spacer(modifier = Modifier.width(6.dp))
            TimerText(state)
        }
    }
}

@Composable
private fun TimerIcon(state: IslandState.Timer) {
    Icon(
        imageVector = Icons.Default.Timer,
        contentDescription = null,
        tint = Color(0xFFFF9500),
        modifier = Modifier.size(18.dp)
    )
}

@Composable
private fun TimerText(state: IslandState.Timer) {
    val text = when (val mode = state.mode) {
        is IslandState.Timer.TimerMode.Countdown -> formatTime(mode.remainingMs)
        is IslandState.Timer.TimerMode.Stopwatch -> formatStopwatch(mode.elapsedMs)
        else -> "0:00"
    }

    Text(
        text = text,
        color = Color(0xFFFF9500),
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Default,
        maxLines = 1
    )
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

private fun formatStopwatch(ms: Long): String {
    val centiseconds = (ms / 10) % 100
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    
    return if (minutes < 1) {
        String.format(Locale.US, "%02d.%02d", seconds, centiseconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
