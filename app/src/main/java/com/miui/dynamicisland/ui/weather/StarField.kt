package com.miui.dynamicisland.ui.weather

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun StarryNight(
    modifier: Modifier = Modifier
) {
    val stars = remember { List(150) {
        Star(
            x = Random.nextFloat(),
            y = Random.nextFloat() * 0.7f, // Keep upper 70%
            size = 1f + Random.nextFloat() * 2.5f,
            twinklePhase = Random.nextFloat() * 360f,
            twinkleSpeed = 2f + Random.nextFloat() * 3f
        )
    }}
    
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starTime"
    )
    
    Canvas(modifier = modifier) {
        stars.forEach { star ->
            val twinkle = 0.5f + 0.5f * sin(
                (time * star.twinkleSpeed + star.twinklePhase) * (PI / 180f).toFloat()
            )
            
            drawCircle(
                color = Color.White.copy(alpha = 0.6f * twinkle),
                center = Offset(star.x * size.width, star.y * size.height),
                radius = star.size
            )
            
            // Cross sparkle for bright stars
            if (star.size > 2f) {
                val sparkleLength = star.size * 2f * twinkle
                drawLine(
                    color = Color.White.copy(alpha = 0.3f * twinkle),
                    start = Offset(
                        star.x * size.width - sparkleLength,
                        star.y * size.height
                    ),
                    end = Offset(
                        star.x * size.width + sparkleLength,
                        star.y * size.height
                    ),
                    strokeWidth = 0.5f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.3f * twinkle),
                    start = Offset(
                        star.x * size.width,
                        star.y * size.height - sparkleLength
                    ),
                    end = Offset(
                        star.x * size.width,
                        star.y * size.height + sparkleLength
                    ),
                    strokeWidth = 0.5f
                )
            }
        }
    }
}

data class Star(
    val x: Float,
    val y: Float,
    val size: Float,
    val twinklePhase: Float,
    val twinkleSpeed: Float
)
