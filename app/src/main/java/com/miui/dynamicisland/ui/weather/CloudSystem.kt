package com.miui.dynamicisland.ui.weather

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

data class CloudLayer(
    val speed: Float,
    val yOffset: Float,
    val scale: Float,
    val opacity: Float,
    val cloudCount: Int
)

@Composable
fun AnimatedClouds(
    weatherCondition: WeatherCondition,
    timeOfDay: TimeOfDay,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")
    
    // Different speeds for parallax depth - made slower for premium feel
    val layers = listOf(
        CloudLayer(0.08f, 0.2f, 0.6f, 0.3f, 3),   // Far
        CloudLayer(0.15f, 0.35f, 0.8f, 0.5f, 4),   // Mid
        CloudLayer(0.25f, 0.5f, 1.0f, 0.7f, 5)      // Near
    )
    
    Box(modifier = modifier) {
        layers.forEachIndexed { index, layer ->
            val offsetX by infiniteTransition.animateFloat(
                initialValue = -200f,
                targetValue = 1200f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (30000 / layer.speed).toInt(),
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "cloudLayer$index"
            )
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                repeat(layer.cloudCount) { i ->
                    val cloudX = (offsetX + i * 250f * layer.scale) % size.width
                    val cloudY = size.height * layer.yOffset + 
                        sin((cloudX + i * 100) * 0.01f) * 20f
                    
                    drawCloud(
                        center = Offset(cloudX, cloudY),
                        scale = layer.scale,
                        baseColor = when(timeOfDay) {
                            TimeOfDay.Night -> Color(0xFF4A5568)
                            TimeOfDay.Dawn, TimeOfDay.Dusk -> Color(0xFF8B7D6B)
                            else -> Color(0xFFFFFFFF)
                        }.copy(alpha = layer.opacity),
                        weatherCondition = weatherCondition
                    )
                }
            }
        }
    }
}

// Draw a cloud using multiple overlapping circles for fluffy effect
fun DrawScope.drawCloud(
    center: Offset,
    scale: Float,
    baseColor: Color,
    weatherCondition: WeatherCondition
) {
    val baseRadius = 30f * scale
    
    // Cloud puffs - overlapping circles create fluffy shape
    val puffs = listOf(
        Offset(-baseRadius * 0.8f, baseRadius * 0.2f) to baseRadius * 0.9f,
        Offset(0f, 0f) to baseRadius,
        Offset(baseRadius * 0.8f, baseRadius * 0.3f) to baseRadius * 0.85f,
        Offset(-baseRadius * 0.3f, -baseRadius * 0.4f) to baseRadius * 0.7f,
        Offset(baseRadius * 0.4f, -baseRadius * 0.3f) to baseRadius * 0.75f
    )
    
    puffs.forEach { (offset, radius) ->
        drawCircle(
            color = baseColor.copy(
                alpha = baseColor.alpha * (0.7f + 0.3f * (1f - offset.y / baseRadius))
            ),
            center = Offset(center.x + offset.x, center.y + offset.y),
            radius = radius
        )
    }
    
    // Add shadow/depth for storm clouds
    if (weatherCondition == WeatherCondition.Storm) {
        drawCircle(
            color = Color(0xFF2D3748).copy(alpha = 0.4f),
            center = Offset(center.x, center.y + baseRadius * 0.3f),
            radius = baseRadius * 1.2f
        )
    }
}
