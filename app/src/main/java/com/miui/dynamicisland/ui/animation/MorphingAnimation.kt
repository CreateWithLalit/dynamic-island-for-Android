// File: app/src/main/java/com/miui/dynamicisland/ui/animation/MorphingAnimation.kt
// Purpose: Spring animations for island resizing (Apple HIG compliant)

package com.miui.dynamicisland.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import com.miui.dynamicisland.manager.IslandSizeManager
import com.miui.dynamicisland.ui.states.IslandState
import kotlinx.coroutines.delay

// Apple-style spring specs for Float values
object SpringSpecs {
    val IslandSpring = spring<Float>(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessLow
    )

    val BouncySpring = spring<Float>(
        dampingRatio = 0.5f,
        stiffness = Spring.StiffnessLow
    )

    val QuickSpring = spring<Float>(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMedium
    )
}

@Composable
fun IslandMorphingAnimation(
    targetState: IslandState,
    sizeManager: IslandSizeManager,
    onDimensionsChanged: (width: Dp, height: Dp, cornerRadius: Dp) -> Unit
) {
    val targetDimensions = sizeManager.getDimensionsForState(targetState)

    // Animate raw Float values (dp units)
    val widthAnim = remember { Animatable(targetDimensions.width.value) }
    val heightAnim = remember { Animatable(targetDimensions.height.value) }
    val radiusAnim = remember { Animatable(targetDimensions.cornerRadius.value) }

    LaunchedEffect(targetState) {
        val animationSpec = when (targetState) {
            is IslandState.Notification -> SpringSpecs.BouncySpring
            is IslandState.Call -> SpringSpecs.IslandSpring
            is IslandState.Charging -> SpringSpecs.QuickSpring
            else -> SpringSpecs.IslandSpring
        }

        widthAnim.animateTo(targetDimensions.width.value, animationSpec)
        heightAnim.animateTo(targetDimensions.height.value, animationSpec)
        radiusAnim.animateTo(targetDimensions.cornerRadius.value, animationSpec)
    }

    LaunchedEffect(widthAnim.value, heightAnim.value, radiusAnim.value) {
        // Positional arguments – no named parameters (fixes "named arguments prohibited" error)
        onDimensionsChanged(Dp(widthAnim.value), Dp(heightAnim.value), Dp(radiusAnim.value))
    }
}

@Composable
fun ChargingPulseAnimation(
    isCharging: Boolean,
    onPulse: (scale: Float, alpha: Float) -> Unit
) {
    val scaleAnim = remember { Animatable(1f) }
    val alphaAnim = remember { Animatable(1f) }

    LaunchedEffect(isCharging) {
        if (!isCharging) {
            scaleAnim.animateTo(1f, SpringSpecs.QuickSpring)
            alphaAnim.animateTo(1f, SpringSpecs.QuickSpring)
            return@LaunchedEffect
        }

        while (true) {
            scaleAnim.animateTo(1.05f, SpringSpecs.QuickSpring)
            alphaAnim.animateTo(0.8f, SpringSpecs.QuickSpring)
            delay(1000)
            scaleAnim.animateTo(1f, SpringSpecs.QuickSpring)
            alphaAnim.animateTo(1f, SpringSpecs.QuickSpring)
            delay(1500)
        }
    }

    LaunchedEffect(scaleAnim.value, alphaAnim.value) {
        onPulse(scaleAnim.value, alphaAnim.value)
    }
}