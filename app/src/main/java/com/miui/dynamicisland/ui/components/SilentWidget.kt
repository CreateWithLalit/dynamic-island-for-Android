// File: app/src/main/java/com/miui/dynamicisland/ui/components/SilentWidget.kt
// Purpose: Silent / vibrate / ring mode – left animated icon, right label

package com.miui.dynamicisland.ui.components

import android.media.AudioManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.ui.states.IslandState

private val SilentOrange = Color(0xFFFF9F0A)
private val RingGreen = Color(0xFF30D158)

@Composable
fun SilentWidget(
    state: IslandState.Silent,
    slot: SilentSlot,
    modifier: Modifier = Modifier
) {
    when (slot) {
        SilentSlot.LEFT -> SilentLeftSlot(state, modifier)
        SilentSlot.RIGHT -> SilentRightSlot(state, modifier)
    }
}

@Composable
private fun SilentLeftSlot(
    state: IslandState.Silent,
    modifier: Modifier = Modifier
) {
    var triggered by remember { mutableStateOf(false) }
    val popScale by animateFloatAsState(
        targetValue = if (triggered) 1f else 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    LaunchedEffect(state.isSilent, state.ringerMode) { triggered = false; triggered = true }

    val infiniteTransition = rememberInfiniteTransition(label = "vibrate_shimmer")
    val vibrateShift by infiniteTransition.animateFloat(
        initialValue = -1.5f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(80, easing = LinearEasing), RepeatMode.Reverse)
    )

    val iconAlpha by animateFloatAsState(
        targetValue = if (state.isSilent || state.ringerMode == AudioManager.RINGER_MODE_VIBRATE) 1f else 0.85f,
        animationSpec = tween(200)
    )

    val isVibrate = state.ringerMode == AudioManager.RINGER_MODE_VIBRATE
    val icon = when {
        isVibrate -> Icons.Default.Vibration
        state.isSilent -> Icons.Default.NotificationsOff
        else -> Icons.Default.Notifications
    }
    val iconColor = when {
        isVibrate -> SilentOrange
        state.isSilent -> SilentOrange
        else -> RingGreen
    }

    Icon(
        imageVector = icon,
        contentDescription = when {
            isVibrate -> "Vibrate"
            state.isSilent -> "Silent"
            else -> "Ring"
        },
        modifier = modifier
            .size(20.dp)
            .scale(popScale)
            .alpha(iconAlpha)
            .offset(x = if (isVibrate) vibrateShift.dp else 0.dp)
            .padding(start = 8.dp),
        tint = iconColor
    )
}

@Composable
private fun SilentRightSlot(
    state: IslandState.Silent,
    modifier: Modifier = Modifier
) {
    val isVibrate = state.ringerMode == AudioManager.RINGER_MODE_VIBRATE
    val label = when {
        isVibrate -> "Vibrate"
        state.isSilent -> "Silent"
        else -> "Ring"
    }
    val labelColor = when {
        isVibrate -> SilentOrange
        state.isSilent -> SilentOrange
        else -> RingGreen
    }
    Text(
        text = label,
        color = labelColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(end = 8.dp)
    )
}