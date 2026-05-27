package com.miui.dynamicisland.ui.bluetooth

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DynamicBluetoothIslandOverlay(
    batteryLevel: Int,
    deviceName: String?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val islandWidth by animateDpAsState(
        targetValue = if (isExpanded) 320.dp else 160.dp,
        animationSpec = spring(stiffness = 450f, dampingRatio = 0.75f),
        label = "islandWidth"
    )
    val islandHeight by animateDpAsState(
        targetValue = if (isExpanded) 85.dp else 40.dp,
        animationSpec = spring(stiffness = 450f, dampingRatio = 0.75f),
        label = "islandHeight"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 28.dp else 22.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f),
        label = "cornerRadius"
    )

    Box(
        modifier = Modifier
            .width(islandWidth)
            .height(islandHeight)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.Black)
            .clickable { onToggleExpand() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        val normalized = if (batteryLevel >= 0) batteryLevel.coerceIn(0, 100) / 100f else 0f

        if (!isExpanded) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.size(18.dp).background(Color.White, CircleShape))
                BatteryGauge(progress = normalized)
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.size(28.dp).background(Color.White, CircleShape))
                Column {
                    Text(
                        text = deviceName ?: "Bluetooth Device",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (batteryLevel >= 0) "Connected • $batteryLevel%" else "Searching...",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun BatteryGauge(progress: Float) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val strokeWidth = 2.5.dp.toPx()
        drawCircle(
            color = Color(0x33FFFFFF),
            style = Stroke(width = strokeWidth)
        )
        drawArc(
            color = if (progress <= 0.2f) Color.Red else Color(0xFF34C759),
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

