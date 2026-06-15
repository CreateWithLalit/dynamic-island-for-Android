package com.miui.dynamicisland.ui.weather

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun RainEffect(
    intensity: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        
        // Create rain drops
        val drops = remember(intensity, widthPx, heightPx) { 
            List((100 * intensity).toInt()) {
                RainDrop(
                    x = Random.nextFloat() * widthPx,
                    y = Random.nextFloat() * heightPx,
                    speed = 800f + Random.nextFloat() * 400f,
                    length = 15f + Random.nextFloat() * 20f,
                    opacity = 0.3f + Random.nextFloat() * 0.4f
                )
            }
        }
    
        val infiniteTransition = rememberInfiniteTransition(label = "rain")
        val time by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rainTime"
        )
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            drops.forEach { drop ->
                val currentY = (drop.y + drop.speed * time) % size.height
                val windOffset = sin(currentY * 0.01f) * 5f
                
                drawLine(
                    color = Color(0xFFB0C4DE).copy(alpha = drop.opacity),
                    start = Offset(drop.x + windOffset, currentY),
                    end = Offset(drop.x + windOffset + 2f, currentY + drop.length),
                    strokeWidth = 1.5f
                )
            }
        }
    }
}

data class RainDrop(
    val x: Float,
    val y: Float,
    val speed: Float,
    val length: Float,
    val opacity: Float
)

@Composable
fun LightningEffect(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    var flashAlpha by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        
        while (active) {
            // Random interval between 3-8 seconds
            delay((3000 + Random.nextInt(5000)).toLong())
            
            // Flash sequence
            flashAlpha = 0.8f
            delay(50)
            flashAlpha = 0.2f
            delay(50)
            flashAlpha = 0.6f
            delay(100)
            flashAlpha = 0f
        }
    }
    
    val alpha by animateFloatAsState(
        targetValue = flashAlpha,
        animationSpec = tween(100),
        label = "lightning"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = alpha))
    )
}

@Composable
fun SnowEffect(
    intensity: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        
        val flakes = remember(intensity, widthPx, heightPx) {
            List((50 * intensity).toInt()) {
                SnowFlake(
                    x = Random.nextFloat() * widthPx,
                    y = Random.nextFloat() * heightPx,
                    speed = 100f + Random.nextFloat() * 100f,
                    radius = 2f + Random.nextFloat() * 3f,
                    opacity = 0.5f + Random.nextFloat() * 0.5f,
                    horizontalOscillation = 20f + Random.nextFloat() * 30f
                )
            }
        }
    
        val infiniteTransition = rememberInfiniteTransition(label = "snow")
        val time by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(5000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "snowTime"
        )
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            flakes.forEach { flake ->
                val currentY = (flake.y + flake.speed * time * 5f) % size.height
                val horizontalOffset = sin(currentY * 0.02f) * flake.horizontalOscillation
                
                drawCircle(
                    color = Color.White.copy(alpha = flake.opacity),
                    center = Offset(flake.x + horizontalOffset, currentY),
                    radius = flake.radius
                )
            }
        }
    }
}

data class SnowFlake(
    val x: Float,
    val y: Float,
    val speed: Float,
    val radius: Float,
    val opacity: Float,
    val horizontalOscillation: Float
)
