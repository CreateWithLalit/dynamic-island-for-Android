// File: app/src/main/java/com/miui/dynamicisland/ui/components/ChargingWidget.kt
// Purpose: Charging expansion logic and iOS bolt animation
// Hinglish: Is file mein charging animation aur Apple style lightning bolt handle hota hai.

package com.miui.dynamicisland.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.ui.states.IslandState

private val ChargingGreen = Color(0xFF30D158)
private val ChargingTextDim = Color.White.copy(alpha = 0.6f)

@Composable
fun ChargingWidget(
    state: IslandState.Charging,
    slot: ChargingSlot,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isExpanded) {
        ExpandedChargingWidget(state, modifier)
    } else {
        when (slot) {
            ChargingSlot.LEFT -> ChargingLeftSlot(state, modifier)
            ChargingSlot.RIGHT -> ChargingRightSlot(state, modifier)
        }
    }
}

@Composable
private fun ChargingLeftSlot(state: IslandState.Charging, modifier: Modifier = Modifier) {
    Text(
        text = if (state.isCharging) "Charging" else "Disconnected",
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(start = 8.dp)
    )
}

@Composable
private fun ChargingRightSlot(state: IslandState.Charging, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.wrapContentWidth().padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("${state.batteryLevel}%", color = ChargingGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        IOSBoltIcon(Modifier.size(16.dp))
    }
}

@Composable
private fun ExpandedChargingWidget(state: IslandState.Charging, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = if (state.chargeMethod == IslandState.Charging.ChargeMethod.WIRELESS) "Wireless Charging" else "Wired Charging",
                color = ChargingTextDim,
                fontSize = 13.sp
            )
            Text("${state.batteryLevel}% Charged", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        IOSBoltIcon(Modifier.size(36.dp), color = ChargingGreen)
    }
}

@Composable
fun IOSBoltIcon(modifier: Modifier = Modifier, color: Color = ChargingGreen) {
    val infiniteTransition = rememberInfiniteTransition(label = "bolt_flicker")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val path = Path().apply {
            moveTo(width * 0.6f, 0f)
            lineTo(width * 0.1f, height * 0.6f)
            lineTo(width * 0.45f, height * 0.6f)
            lineTo(width * 0.35f, height)
            lineTo(width * 0.9f, height * 0.4f)
            lineTo(width * 0.55f, height * 0.4f)
            close()
        }
        drawPath(path = path, color = color.copy(alpha = alpha))
    }
}
