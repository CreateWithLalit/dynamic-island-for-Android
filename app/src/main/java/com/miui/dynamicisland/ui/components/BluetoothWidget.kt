// File: app/src/main/java/com/miui/dynamicisland/ui/components/BluetoothWidget.kt
// Purpose: Bluetooth connection status – left icon + device name, right battery %

package com.miui.dynamicisland.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.ui.states.IslandState

// Apple HIG colors (dark mode)
private val BtBlue = Color(0xFF0A84FF)      // connected blue
private val BtDim = Color.White.copy(alpha = 0.4f)
private val BtTextPrimary = Color.White
private val BtBatteryGreen = Color(0xFF30D158)
private val BtBatteryRed = Color(0xFFFF3B30)

@Composable
fun BluetoothWidget(
    state: IslandState.Bluetooth,
    slot: BluetoothSlot,
    modifier: Modifier = Modifier
) {
    when (slot) {
        BluetoothSlot.LEFT -> BluetoothLeftSlot(state, modifier)
        BluetoothSlot.RIGHT -> BluetoothRightSlot(state, modifier)
    }
}

@Composable
private fun BluetoothLeftSlot(
    state: IslandState.Bluetooth,
    modifier: Modifier = Modifier
) {
    var triggered by remember { mutableStateOf(false) }
    val popScale by animateFloatAsState(
        targetValue = if (triggered) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bt_pop"
    )
    LaunchedEffect(state.isConnected, state.deviceName) {
        triggered = false; triggered = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bt_scan")
    val scanAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bt_scan_alpha"
    )

    val iconAlpha by animateFloatAsState(
        targetValue = if (state.isConnected) 1f else scanAlpha,
        animationSpec = tween(100)
    )

    Row(
        modifier = modifier
            .scale(popScale)
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (state.isConnected) {
            AirPodsIcon(
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
        } else {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = "Bluetooth",
                modifier = Modifier.size(20.dp).alpha(iconAlpha),
                tint = BtDim
            )
        }
        Text(
            text = state.deviceName.ifBlank { "Bluetooth" },
            color = BtTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AirPodsIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.08f

        // Left Earbud
        val leftEar = Path().apply {
            // Head (rounded top)
            addOval(androidx.compose.ui.geometry.Rect(w * 0.12f, h * 0.25f, w * 0.42f, h * 0.55f))
            // Stem
            moveTo(w * 0.27f, h * 0.55f)
            lineTo(w * 0.27f, h * 0.85f)
        }
        
        // Right Earbud
        val rightEar = Path().apply {
            // Head (rounded top)
            addOval(androidx.compose.ui.geometry.Rect(w * 0.58f, h * 0.25f, w * 0.88f, h * 0.55f))
            // Stem
            moveTo(w * 0.73f, h * 0.55f)
            lineTo(w * 0.73f, h * 0.85f)
        }

        drawPath(leftEar, color = tint, style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawPath(rightEar, color = tint, style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        
        // Center dot (connectivity)
        drawCircle(
            color = tint.copy(alpha = 0.8f),
            radius = w * 0.05f,
            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.7f)
        )
    }
}

@Composable
private fun BluetoothRightSlot(
    state: IslandState.Bluetooth,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // battery level if available
        state.batteryLevel?.let { rawLevel ->
            val level = rawLevel.coerceIn(0, 100)
            Text(
                text = "$level%",
                color = if (level <= 20) BtBatteryRed else BtBatteryGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            IosBatteryIcon(
                level = level,
                isCharging = false, 
                modifier = Modifier.size(width = 24.dp, height = 12.dp),
                color = if (level <= 20) BtBatteryRed else BtBatteryGreen
            )
        } ?: run {
            // fallback connected dot indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (state.isConnected) BtBlue else BtDim)
            )
        }
    }
}
