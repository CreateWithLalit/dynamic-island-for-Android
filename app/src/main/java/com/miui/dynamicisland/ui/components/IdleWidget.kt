// File: app/src/main/java/com/miui/dynamicisland/ui/components/IdleWidget.kt
// Purpose: Green privacy dot that breathes when no active state (🖼️ Image 1)
// Hinglish: Is file mein breathing green dot handle hota hai jo idle state mein dikhta hai.

package com.miui.dynamicisland.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PrivacyDotGreen = Color(0xFF30D158)

@Composable
fun IdleWidget(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "idle_breathe")
    
    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .scale(breathScale)
                .alpha(breathAlpha)
                .background(PrivacyDotGreen, CircleShape)
        )
    }
}
