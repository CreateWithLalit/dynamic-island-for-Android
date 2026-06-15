package com.miui.dynamicisland.ui.weather

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedSun(
    sunPosition: Offset, // Position along the arc
    intensity: Float, // 0.0 to 1.0 based on time of day
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sun")
    
    // Ray rotation
    val rayRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rayRotation"
    )
    
    // Core pulse
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )
    
    Canvas(modifier = modifier) {
        val center = sunPosition
        val baseRadius = size.minDimension * 0.06f * intensity
        
        // Outer corona (bloom effect)
        for (i in 4 downTo 1) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0.25f / i),
                        Color(0xFFFFA500).copy(alpha = 0.0f)
                    ),
                    center = center,
                    radius = baseRadius * (2f + i * 1.2f)
                ),
                center = center,
                radius = baseRadius * (2f + i * 1.2f)
            )
        }
        
        // Light rays
        val rayCount = 12
        val rayLength = baseRadius * 3f
        rotate(rayRotation, center) {
            for (i in 0 until rayCount) {
                val angle = (i * 360f / rayCount) * (PI / 180f).toFloat()
                val start = Offset(
                    center.x + cos(angle) * baseRadius * 1.2f,
                    center.y + sin(angle) * baseRadius * 1.2f
                )
                val end = Offset(
                    center.x + cos(angle) * rayLength,
                    center.y + sin(angle) * rayLength
                )
                
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = 0.6f * intensity),
                            Color(0xFFFFA500).copy(alpha = 0.0f)
                        ),
                        start = start,
                        end = end
                    ),
                    start = start,
                    end = end,
                    strokeWidth = 2f
                )
            }
        }
        
        // Sun core with pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFFE0),
                    Color(0xFFFFD700),
                    Color(0xFFFFA500).copy(alpha = 0.5f)
                ),
                center = center,
                radius = baseRadius * corePulse
            ),
            center = center,
            radius = baseRadius * corePulse
        )
    }
}

@Composable
fun AnimatedMoon(
    position: Offset,
    phase: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = position
        val radius = size.minDimension * 0.06f
        
        // Moon glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE8F6FF).copy(alpha = 0.3f),
                    Color(0xFF87CEEB).copy(alpha = 0.0f)
                ),
                center = center,
                radius = radius * 3f
            ),
            center = center,
            radius = radius * 3f
        )
        
        // Moon core
        drawCircle(
            color = Color(0xFFE8F6FF),
            center = center,
            radius = radius
        )
        
        // Craters
        drawCircle(
            color = Color(0xFFBDC3C7).copy(alpha = 0.4f),
            radius = radius * 0.2f,
            center = Offset(center.x - radius * 0.3f, center.y - radius * 0.2f)
        )
        drawCircle(
            color = Color(0xFFBDC3C7).copy(alpha = 0.4f),
            radius = radius * 0.15f,
            center = Offset(center.x + radius * 0.4f, center.y + radius * 0.3f)
        )
    }
}
