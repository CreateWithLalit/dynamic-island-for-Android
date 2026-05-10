// File: app/src/main/java/com/miui/dynamicisland/ui/components/IslandPill.kt
// Purpose: Pill-shaped container with Apple HIG dimensions (126dp x 37dp, 18.5dp radius)

package com.miui.dynamicisland.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.miui.dynamicisland.ui.theme.IslandBlack

private val PillGlow = Color.White.copy(alpha = 0.06f)

// Apple HIG: Compact pill = 126dp x 37dp, radius = 18.5dp
private val DEFAULT_WIDTH = 126.dp
private val DEFAULT_HEIGHT = 37.dp
private val DEFAULT_RADIUS = 18.5.dp

@Composable
fun IslandPill(
    width: Dp = DEFAULT_WIDTH,
    height: Dp = DEFAULT_HEIGHT,
    cornerRadius: Dp = DEFAULT_RADIUS,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .size(width, height)
            .clip(shape)
            .background(IslandBlack, shape)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(PillGlow, Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.3f
                    )
                )
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun IslandPillIdle(
    width: Dp = DEFAULT_WIDTH,
    height: Dp = DEFAULT_HEIGHT,
    cornerRadius: Dp = DEFAULT_RADIUS,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "idle_breath")
    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 0.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_breath_alpha"
    )
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .size(width, height)
            .clip(shape)
            .background(IslandBlack, shape)
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = breathAlpha),
                    radius = size.minDimension * 0.6f,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            }
    )
}

@Composable
fun IslandPillContent(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp), // Apple HIG internal padding
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        content = content
    )
}