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
            stiffness = Spring.StiffnessLow   // Apple HIG: low stiffness for smooth pop
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
            .padding(start = 8.dp),   // Apple HIG internal padding
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)   // slightly larger gap
    ) {
        Icon(
            imageVector = if (state.isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
            contentDescription = if (state.isConnected) "Bluetooth connected" else "Bluetooth",
            modifier = Modifier.size(20.dp).alpha(iconAlpha),   // 20dp (Apple HIG: 24 but BT icon smaller looks balanced)
            tint = if (state.isConnected) BtBlue else BtDim
        )
        Text(
            text = state.deviceName.ifBlank { "Bluetooth" },
            color = BtTextPrimary,
            fontSize = 14.sp,            // Apple HIG: 14pt minimum
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // connected dot indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (state.isConnected) BtBlue else BtDim)
        )
        // battery level if available
        state.batteryLevel?.let { rawLevel ->
            val level = rawLevel.coerceIn(0, 100)
            Text(
                text = "$level%",
                color = if (level <= 20) BtBatteryRed else BtBatteryGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}