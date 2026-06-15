package com.miui.dynamicisland.ui.weather

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AnimatedSkyBackground(
    currentTime: LocalTime,
    weatherCondition: WeatherCondition,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sky")
    
    // Subtle gradient shift animation
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientShift"
    )
    
    // Interpolate between current and next time-of-day colors
    val currentTOD = TimeOfDay.fromHour(currentTime.hour)
    val nextTOD = TimeOfDay.fromHour((currentTime.hour + 1) % 24)
    val progress = currentTime.minute / 60f
    
    val colors = lerpSkyColors(
        skyGradients[currentTOD]!!,
        skyGradients[nextTOD]!!,
        progress
    )
    
    // Add subtle movement to gradient
    val brush = Brush.verticalGradient(
        colors = colors.map { it.copy(alpha = 0.9f + 0.1f * sin(gradientOffset * 2 * PI).toFloat()) },
        startY = 0f,
        endY = Float.POSITIVE_INFINITY
    )
    
    Box(modifier = modifier.background(brush))
}

fun lerpSkyColors(start: List<Color>, end: List<Color>, fraction: Float): List<Color> {
    return start.zip(end).map { (s, e) ->
        lerp(s, e, fraction)
    }
}
