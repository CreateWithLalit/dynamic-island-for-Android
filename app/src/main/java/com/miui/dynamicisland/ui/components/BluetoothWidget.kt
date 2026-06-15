// File: app/src/main/java/com/miui/dynamicisland/ui/components/BluetoothWidget.kt
// Purpose: Bluetooth connection status – 3D style buds and individual battery levels

package com.miui.dynamicisland.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.ui.states.IslandState

private val BtBlue = Color(0xFF0A84FF)
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

    Row(
        modifier = modifier
            .scale(popScale)
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AirPods3DView(
                modifier = Modifier.size(24.dp),
                showCase = false
            )
            if (state.isConnected) {
                Icon(
                    imageVector = Icons.Default.BluetoothConnected,
                    contentDescription = null,
                    tint = BtBlue,
                    modifier = Modifier.size(10.dp).background(Color.Black, CircleShape).padding(1.dp)
                )
            }
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
private fun BluetoothRightSlot(
    state: IslandState.Bluetooth,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(end = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        BatteryRing(level = state.batteryLevel, size = 22.dp, strokeWidth = 2.5.dp, fontSize = 0.sp)
    }
}

@Composable
fun BluetoothExpandedWidget(
    state: IslandState.Bluetooth,
    modifier: Modifier = Modifier
) {
    val deviceName = state.deviceName.ifBlank { "Bluetooth" }
    
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row: Title and Connection Status
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deviceName,
                    color = BtTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (state.isConnected) "Connected" else "Connecting...",
                    color = if (state.isConnected) BtBatteryGreen else BtBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Realistic Glossy Bluetooth Icon
            IosGlyphIcon(
                icon = if (state.isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                contentDescription = "Bluetooth",
                backgroundColor = BtBlue,
                tint = Color.White,
                containerSize = 40.dp,
                iconSize = 24.dp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Center: High-Fidelity 3D-style Render of the Buds
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            AirPods3DView(
                modifier = Modifier.size(160.dp),
                showCase = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom: Individual Battery Levels with premium styling
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BatteryIndicator(label = "L", level = state.batteryLeft)
            BatteryIndicator(label = "Case", level = state.batteryCase)
            BatteryIndicator(label = "R", level = state.batteryRight)
        }
    }
}

@Composable
private fun BatteryIndicator(label: String, level: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BatteryRing(level = level, size = 52.dp, strokeWidth = 5.dp, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = BtDim,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AirPods3DView(modifier: Modifier = Modifier, showCase: Boolean = true) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Colors for premium shading
        val baseColor = Color.White
        val highlightColor = Color.White
        val midShadow = Color(0xFFF2F2F7)
        val deepShadow = Color(0xFFD1D1D6)
        val plasticSheen = Color.White.copy(alpha = 0.8f)
        
        if (showCase) {
            // ── Case Render ───────────────────────────────────────────────────
            val caseWidth = w * 0.48f
            val caseHeight = h * 0.58f
            val caseTop = h * 0.38f
            val caseLeft = w * 0.26f
            
            val caseRect = androidx.compose.ui.geometry.Rect(
                Offset(caseLeft, caseTop),
                Size(caseWidth, caseHeight)
            )
            
            // Case Body Main Gradient
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(highlightColor, midShadow, deepShadow),
                    start = Offset(caseRect.left, caseRect.top),
                    end = Offset(caseRect.right, caseRect.bottom)
                ),
                topLeft = caseRect.topLeft,
                size = caseRect.size,
                cornerRadius = CornerRadius(caseWidth * 0.35f)
            )

            // Glossy Highlight on Case Top-Left
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(plasticSheen, Color.Transparent),
                    startY = caseRect.top,
                    endY = caseRect.top + caseHeight * 0.4f
                ),
                topLeft = caseRect.topLeft.plus(Offset(caseWidth * 0.1f, caseHeight * 0.05f)),
                size = Size(caseWidth * 0.8f, caseHeight * 0.3f),
                cornerRadius = CornerRadius(caseWidth * 0.2f)
            )
            
            // Case Lid Seam (Realistic 3D depth)
            val lidLineY = caseRect.top + caseHeight * 0.32f
            drawLine(
                color = Color.Black.copy(alpha = 0.15f),
                start = Offset(caseRect.left, lidLineY),
                end = Offset(caseRect.right, lidLineY),
                strokeWidth = 1.5.dp.toPx()
            )
            
            // Case LED (Soft Glow)
            drawCircle(
                color = BtBatteryGreen.copy(alpha = 0.4f),
                radius = 4.dp.toPx(),
                center = Offset(w * 0.5f, caseRect.top + caseHeight * 0.55f)
            )
            drawCircle(
                color = BtBatteryGreen,
                radius = 1.8.dp.toPx(),
                center = Offset(w * 0.5f, caseRect.top + caseHeight * 0.55f)
            )
        }

        // ── Earbuds ─────────────────────────────────────────────────────────
        
        // Left Earbud
        withTransform({
            val tx = if (showCase) -w * 0.18f else 0f
            val ty = if (showCase) -h * 0.12f else 0f
            translate(left = tx, top = ty)
            rotate(-12f, Offset(w * 0.32f, h * 0.38f))
        }) {
            drawDetailedEarbud(this, w, h, baseColor, deepShadow, isRight = false)
        }

        // Right Earbud
        withTransform({
            val tx = if (showCase) w * 0.18f else 0f
            val ty = if (showCase) -h * 0.12f else 0f
            translate(left = tx, top = ty)
            rotate(12f, Offset(w * 0.68f, h * 0.38f))
        }) {
            drawDetailedEarbud(this, w, h, baseColor, deepShadow, isRight = true)
        }
    }
}

private fun drawDetailedEarbud(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    w: Float,
    h: Float,
    baseColor: Color,
    shadowColor: Color,
    isRight: Boolean
) {
    with(drawScope) {
        val headX = if (isRight) w * 0.58f else w * 0.08f
        val headSize = w * 0.34f
        val headRect = androidx.compose.ui.geometry.Rect(headX, h * 0.18f, headX + headSize, h * 0.18f + headSize * 0.9f)
        
        // Earbud Head Shadow (Depth)
        drawOval(
            color = Color.Black.copy(alpha = 0.05f),
            topLeft = headRect.topLeft.plus(Offset(2.dp.toPx(), 4.dp.toPx())),
            size = headRect.size
        )

        // Earbud Head Base
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFF2F2F7), shadowColor),
                center = headRect.center,
                radius = headRect.width * 0.7f
            ),
            topLeft = headRect.topLeft,
            size = headRect.size
        )
        
        // Glossy Highlight on Head
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White, Color.Transparent),
                startY = headRect.top,
                endY = headRect.top + headRect.height * 0.4f
            ),
            topLeft = headRect.topLeft.plus(Offset(headRect.width * 0.2f, headRect.height * 0.1f)),
            size = Size(headRect.width * 0.6f, headRect.height * 0.3f)
        )
        
        // Speaker Mesh (Acoustic detail)
        val meshWidth = headRect.width * 0.35f
        val meshHeight = headRect.height * 0.5f
        val meshX = if (isRight) headRect.left + headRect.width * 0.08f else headRect.right - headRect.width * 0.43f
        drawOval(
            color = Color(0xFF1C1C1E),
            topLeft = Offset(meshX, headRect.top + headRect.height * 0.22f),
            size = Size(meshWidth, meshHeight)
        )
        
        // Sub-mesh details
        drawOval(
            color = Color(0xFF3A3A3C),
            topLeft = Offset(meshX + 2.dp.toPx(), headRect.top + headRect.height * 0.28f),
            size = Size(meshWidth * 0.6f, meshHeight * 0.6f)
        )
        
        // Earbud Stem
        val stemX = if (isRight) headRect.center.x - w * 0.04f else headRect.center.x + w * 0.04f
        val stemStart = headRect.bottom - h * 0.12f
        val stemEnd = h * 0.78f
        
        // Stem Shadow
        drawLine(
            color = Color.Black.copy(alpha = 0.08f),
            start = Offset(stemX + 2.dp.toPx(), stemStart + 2.dp.toPx()),
            end = Offset(stemX + 2.dp.toPx(), stemEnd),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )

        // Stem Body
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color.White, Color(0xFFF2F2F7), shadowColor),
                start = Offset(stemX, stemStart),
                end = Offset(stemX, stemEnd)
            ),
            start = Offset(stemX, stemStart),
            end = Offset(stemX, stemEnd),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )

        // Bottom Chrome Contact (Realistic finish)
        drawLine(
            color = Color(0xFFD1D1D6),
            start = Offset(stemX, stemEnd - 4.dp.toPx()),
            end = Offset(stemX, stemEnd),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun BatteryRing(
    level: Int?, 
    size: Dp = 46.dp, 
    strokeWidth: Dp = 4.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp
) {
    val progress = (level ?: 0).coerceIn(0, 100) / 100f
    val ringColor = if ((level ?: 100) <= 20) BtBatteryRed else BtBatteryGreen

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val sw = strokeWidth.toPx()
            // Track
            drawCircle(color = Color.White.copy(alpha = 0.08f), style = Stroke(width = sw))
            
            // Progress with subtle shadow/glow
            drawArc(
                color = ringColor.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = sw + 2.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
        }
        if (fontSize > 0.sp) {
            Text(
                text = level?.let { "$it%" } ?: "--",
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
    }
}
