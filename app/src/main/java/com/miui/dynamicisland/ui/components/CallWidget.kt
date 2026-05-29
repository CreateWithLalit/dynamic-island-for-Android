// File: app/src/main/java/com/miui/dynamicisland/ui/components/CallWidget.kt
package com.miui.dynamicisland.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
private val CallControlBg = Color(0xFF2C2C2E)
private val CallTextPrimary = Color.White
private val CallTextDim = Color.White.copy(alpha = 0.6f)
private val CallOrange = Color(0xFFFF9F0A)

@Composable
fun CallWidget(
    state: IslandState.Call,
    slot: CallSlot? = null,
    isExpanded: Boolean = false,
    onCallAction: (CallAction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (isExpanded) {
        CallExpandedContent(state, onCallAction, modifier)
        return
    }

    when (slot) {
        CallSlot.LEFT -> CallLeftSlot(state, modifier)
        CallSlot.RIGHT -> CallRightSlot(state, modifier)
        CallSlot.BOTTOM -> CallBottomSlot(state, onCallAction, modifier)
        null -> {}
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
                .background(if (state.isOngoing) CallGreen else CallOrange)
        )
        Icon(
            imageVector = Icons.Default.Call,
            contentDescription = "Call",
            tint = if (state.isOngoing) CallGreen else CallOrange,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = state.callerName.ifBlank { "Call" },
            color = CallTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CallRightSlot(
    state: IslandState.Call,
    modifier: Modifier = Modifier
) {
    if (state.isOngoing) {
        Text(
            text = formatDuration(state.duration),
            color = CallGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = modifier.padding(end = 12.dp)
        )
    } else {
        Icon(
            imageVector = Icons.Default.Call,
            contentDescription = "Incoming",
            tint = CallOrange,
            modifier = modifier.size(18.dp).padding(end = 10.dp)
        )
    }
}

@Composable
private fun CallExpandedContent(
    state: IslandState.Call,
    onCallAction: (CallAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.DarkGray)
        ) {
            if (state.callerPhoto != null) {
                Image(
                    bitmap = state.callerPhoto.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Call,
                    null,
                    Modifier.align(Alignment.Center).size(24.dp),
                    Color.White
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Name & Subtext
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.callerName.ifBlank { "Unknown" },
                color = CallTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = state.callerSubtext,
                color = CallTextDim,
                fontSize = 12.sp,
                maxLines = 1
            )
            if (state.isOngoing) {
                Text(
                    text = formatDuration(state.duration),
                    color = CallGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Action Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.isIncoming) {
                // Decline (Red)
                CallActionButton(
                    icon = Icons.Default.CallEnd,
                    color = CallRed,
                    onClick = { onCallAction(CallAction.Decline) }
                )
                // Accept (Green)
                CallActionButton(
                    icon = Icons.Default.Call,
                    color = CallGreen,
                    onClick = { onCallAction(CallAction.Accept) }
                )
            } else if (state.isOngoing) {
                // End Call (Red)
                CallActionButton(
                    icon = Icons.Default.CallEnd,
                    color = CallRed,
                    onClick = { onCallAction(CallAction.End) }
                )
            }
        }
    }
}

@Composable
private fun CallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(color)
            .padding(8.dp)
            .scale(if (icon == Icons.Default.CallEnd) 1f else 1f), // adjustments if needed
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(42.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CallBottomSlot(
    state: IslandState.Call,
    onCallAction: (CallAction) -> Unit,
    modifier: Modifier = Modifier
) {
    // Legacy support or extra actions
}

private fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
