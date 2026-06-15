// File: app/src/main/java/com/miui/dynamicisland/ui/components/CutoutSafeIslandShell.kt
package com.miui.dynamicisland.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DEFAULT_DEAD_ZONE_WIDTH = 22.dp
private val DEFAULT_HORIZONTAL_PADDING = 8.dp

@Composable
fun CutoutSafeIslandShell(
    width: Dp,
    height: Dp,
    cornerRadius: Dp = 20.dp,
    centerDeadZoneWidth: Dp = DEFAULT_DEAD_ZONE_WIDTH,
    islandColor: Color = Color.Black,
    horizontalPadding: Dp = DEFAULT_HORIZONTAL_PADDING,
    leftContent: @Composable () -> Unit = {},
    rightContent: @Composable () -> Unit = {},
    centerContent: (@Composable () -> Unit)? = null,
    bottomContent: (@Composable () -> Unit)? = null,
    bottomPanelHeight: Dp = 0.dp,
    bottomPanelWidth: Dp = width,
    bottomCornerRadius: Dp = 16.dp,
    bottomPanelSpacing: Dp = 4.dp,
    progress: Float? = null,
    progressColor: Color = Color(0xFF0A84FF).copy(alpha = 0.3f)
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.wrapContentSize()
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(RoundedCornerShape(cornerRadius))
                .background(islandColor),
            contentAlignment = Alignment.Center
        ) {
            // Background Progress Fill
            if (progress != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(progressColor)
                        .align(Alignment.CenterStart)
                )
            }

            if (centerContent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding),
                    contentAlignment = Alignment.Center
                ) {
                    centerContent()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        leftContent()
                    }
                    Spacer(modifier = Modifier.width(centerDeadZoneWidth).height(height))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        rightContent()
                    }
                }
            }
        }
        if (bottomContent != null && bottomPanelHeight > 0.dp) {
            Spacer(modifier = Modifier.height(bottomPanelSpacing))
            Box(
                modifier = Modifier
                    .width(bottomPanelWidth)
                    .height(bottomPanelHeight)
                    .clip(RoundedCornerShape(bottomCornerRadius))
                    .background(islandColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.TopStart
            ) {
                bottomContent()
            }
        }
    }
}

@Composable
fun AnimatedCutoutSafeIslandShell(
    targetWidth: Dp,
    targetHeight: Dp,
    targetCornerRadius: Dp = 20.dp,
    centerDeadZoneWidth: Dp = DEFAULT_DEAD_ZONE_WIDTH,
    islandColor: Color = Color.Black,
    horizontalPadding: Dp = DEFAULT_HORIZONTAL_PADDING,
    targetBottomPanelHeight: Dp = 0.dp,
    bottomPanelWidth: Dp = targetWidth,
    bottomCornerRadius: Dp = 16.dp,
    bottomPanelSpacing: Dp = 4.dp,
    leftContent: @Composable () -> Unit = {},
    rightContent: @Composable () -> Unit = {},
    centerContent: (@Composable () -> Unit)? = null,
    bottomContent: (@Composable () -> Unit)? = null,
    progress: Float? = null,
    progressColor: Color = Color(0xFF0A84FF).copy(alpha = 0.3f)
) {
    // Explicit spring<Dp> to fix inference error
    val sizeSpring = spring<Dp>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val radiusSpring = spring<Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = sizeSpring,
        label = "shell_width"
    )
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = sizeSpring,
        label = "shell_height"
    )
    val animatedCornerRadius by animateDpAsState(
        targetValue = targetCornerRadius,
        animationSpec = radiusSpring,
        label = "shell_radius"
    )
    val animatedBottomHeight by animateDpAsState(
        targetValue = targetBottomPanelHeight,
        animationSpec = sizeSpring,
        label = "bottom_height"
    )

    CutoutSafeIslandShell(
        width = animatedWidth,
        height = animatedHeight,
        cornerRadius = animatedCornerRadius,
        centerDeadZoneWidth = centerDeadZoneWidth,
        islandColor = islandColor,
        horizontalPadding = horizontalPadding,
        leftContent = leftContent,
        rightContent = rightContent,
        centerContent = centerContent,
        bottomContent = bottomContent,
        bottomPanelHeight = animatedBottomHeight,
        bottomPanelWidth = bottomPanelWidth,
        bottomCornerRadius = bottomCornerRadius,
        bottomPanelSpacing = bottomPanelSpacing,
        progress = progress,
        progressColor = progressColor
    )
}
