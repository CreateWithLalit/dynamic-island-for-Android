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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke

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
        text = "${state.batteryLevel}%",
        color = ChargingGreen,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(start = 8.dp)
    )
}

@Composable
private fun ChargingRightSlot(state: IslandState.Charging, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(end = 8.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        IosBatteryIcon(
            level = state.batteryLevel,
            isCharging = state.isCharging,
            modifier = Modifier.size(width = 24.dp, height = 12.dp)
        )
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
            Text("${state.batteryLevel}% Charged", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        IosBatteryIcon(
            level = state.batteryLevel,
            isCharging = state.isCharging,
            modifier = Modifier.size(width = 48.dp, height = 24.dp)
        )
    }
}

@Composable
fun IosBatteryIcon(
    level: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
    color: Color = ChargingGreen
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bolt_flicker")
    val boltAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bolt_alpha"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val r = h * 0.2f
        val strokeWidth = w * 0.05f
        val innerPadding = strokeWidth * 1.5f
        
        // 1. Battery Body (Outline)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            size = size.copy(width = w * 0.92f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
            style = Stroke(width = strokeWidth)
        )
        
        // 2. Battery Tip (Cap)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.94f, h * 0.35f),
            size = androidx.compose.ui.geometry.Size(w * 0.06f, h * 0.3f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.5f, r * 0.5f)
        )
        
        // 3. Battery Fill
        val fillWidth = (w * 0.92f - (innerPadding * 2)) * (level / 100f)
        if (fillWidth > 0) {
            drawRoundRect(
                color = if (isCharging) color else if (level <= 20) Color(0xFFFF3B30) else Color.White,
                topLeft = androidx.compose.ui.geometry.Offset(innerPadding, innerPadding),
                size = androidx.compose.ui.geometry.Size(fillWidth, h - (innerPadding * 2)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.7f, r * 0.7f)
            )
        }
        
        // 4. Charging Bolt (Overlay if charging)
        if (isCharging) {
            val boltW = w * 0.4f
            val boltH = h * 0.8f
            val boltLeft = (w * 0.92f - boltW) / 2f
            val boltTop = (h - boltH) / 2f
            
            val path = Path().apply {
                moveTo(boltLeft + boltW * 0.6f, boltTop)
                lineTo(boltLeft + boltW * 0.1f, boltTop + boltH * 0.6f)
                lineTo(boltLeft + boltW * 0.45f, boltTop + boltH * 0.6f)
                lineTo(boltLeft + boltW * 0.35f, boltTop + boltH)
                lineTo(boltLeft + boltW * 0.9f, boltTop + boltH * 0.4f)
                lineTo(boltLeft + boltW * 0.55f, boltTop + boltH * 0.4f)
                close()
            }
            drawPath(path = path, color = Color.Black.copy(alpha = 0.5f)) // Shadow/Contrast
            drawPath(path = path, color = Color.White.copy(alpha = boltAlpha))
        }
    }
}
