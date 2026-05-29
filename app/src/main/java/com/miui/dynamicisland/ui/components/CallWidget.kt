// File: app/src/main/java/com/miui/dynamicisland/ui/components/CallWidget.kt
// Purpose: Call status – left icon + caller name, right duration/label

package com.miui.dynamicisland.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.ui.island.CallAction
import com.miui.dynamicisland.ui.states.IslandState
import java.util.Locale
import java.util.concurrent.TimeUnit

private val CallGreen = Color(0xFF30D158)
private val CallRed = Color(0xFFFF3B30)
private val CallOrange = Color(0xFFFF9F0A)
private val CallControlBg = Color(0xFF2C2C2E)
private val CallTextPrimary = Color.White
private val CallTextDim = Color.White.copy(alpha = 0.6f)

@Composable
fun CallWidget(
    state: IslandState.Call,
    slot: CallSlot,
    onCallAction: (CallAction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (slot) {
        CallSlot.LEFT -> CallLeftSlot(state, modifier)
        CallSlot.RIGHT -> CallRightSlot(state, modifier)
        CallSlot.BOTTOM -> CallBottomSlot(state, onCallAction, modifier)
    }
}

@Composable
private fun CallLeftSlot(
    state: IslandState.Call,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "call_pulse")
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_pulse"
    )
    val statusLabel = when {
        state.isOngoing -> "Ongoing Call"
        state.isIncoming -> "Incoming Call"
        else -> "Call"
    }
    val statusColor = when {
        state.isOngoing -> CallGreen
        state.isIncoming -> CallOrange
        else -> CallTextDim
    }

    Row(
        modifier = modifier.padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(if (state.isOngoing) dotScale else 1f)
                .clip(CircleShape)
                .background(
                    when {
                        state.isOngoing -> CallGreen
                        state.isIncoming -> CallOrange
                        else -> CallTextDim
                    }
                )
        )
        IosDrawableOrGlyphIcon(
            drawableNameCandidates = listOf(
                "ios_call",
                "ic_ios_call",
                "ios_phone",
                "ic_ios_phone"
            ),
            fallbackIcon = Icons.Default.Call,
            contentDescription = "Call",
            containerSize = 24.dp,
            iconSize = 16.dp,
            tint = CallTextPrimary
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = state.callerName.ifBlank { "Unknown" },
                color = CallTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statusLabel,
                color = statusColor,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CallRightSlot(
    state: IslandState.Call,
    modifier: Modifier = Modifier
) {
    val label = when {
        state.isOngoing -> formatDuration(state.duration)
        state.isIncoming -> "Ringing"
        else -> ""
    }
    val labelColor = when {
        state.isOngoing -> CallGreen
        state.isIncoming -> CallOrange
        else -> CallTextDim
    }
    if (label.isNotEmpty()) {
        Text(
            text = label,
            color = labelColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = modifier.padding(end = 8.dp)
        )
    }
}

@Composable
private fun CallBottomSlot(
    state: IslandState.Call,
    onCallAction: (CallAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.isIncoming) {
            // Decline Button
            IconButton(
                onClick = { onCallAction(CallAction.Decline) },
                modifier = Modifier.size(52.dp).clip(CircleShape).background(CallRed)
            ) {
                IosDrawableOrGlyphIcon(
                    drawableNameCandidates = listOf("ios_call_end", "ic_ios_call_end", "ios_decline"),
                    fallbackIcon = Icons.Default.CallEnd,
                    contentDescription = "Decline",
                    containerSize = 26.dp,
                    iconSize = 18.dp,
                    backgroundColor = Color.Transparent,
                    tint = Color.White
                )
            }
            // Accept Button
            IconButton(
                onClick = { onCallAction(CallAction.Accept) },
                modifier = Modifier.size(52.dp).clip(CircleShape).background(CallGreen)
            ) {
                IosDrawableOrGlyphIcon(
                    drawableNameCandidates = listOf("ios_call", "ic_ios_call", "ios_accept"),
                    fallbackIcon = Icons.Default.Call,
                    contentDescription = "Accept",
                    containerSize = 26.dp,
                    iconSize = 18.dp,
                    backgroundColor = Color.Transparent,
                    tint = Color.White
                )
            }
        } else if (state.isOngoing) {
            var isMuted by remember { mutableStateOf(false) }
            // Mute Button
            IconButton(
                onClick = {
                    isMuted = !isMuted
                    onCallAction(CallAction.Mute)
                },
                modifier = Modifier.size(50.dp).clip(CircleShape).background(if (isMuted) Color.White.copy(alpha = 0.2f) else CallControlBg)
            ) {
                Icon(if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, "Mute", tint = if (isMuted) CallRed else Color.White)
            }
            // End Call Button
            IconButton(
                onClick = { onCallAction(CallAction.End) },
                modifier = Modifier.size(52.dp).clip(CircleShape).background(CallRed)
            ) {
                Icon(Icons.Default.CallEnd, "End Call", tint = Color.White)
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val safeDuration = durationMs.coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(safeDuration)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(safeDuration) % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}