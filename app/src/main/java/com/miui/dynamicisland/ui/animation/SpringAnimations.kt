package com.miui.dynamicisland.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

object SpringAnimations {

    val IslandSpring: SpringSpec<Dp> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val QuickSpring: SpringSpec<Dp> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    val ElasticSpring: SpringSpec<Dp> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    val BouncySpring: SpringSpec<Dp> = spring(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val FloatSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val BouncyFloatSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val ElasticFloatSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}

suspend fun animateDpSpring(
    target: Dp,
    current: Animatable<Dp, AnimationVector1D>,
    spec: SpringSpec<Dp> = SpringAnimations.IslandSpring
) {
    current.animateTo(
        targetValue = target,
        animationSpec = spec
    )
}

suspend fun morphIsland(
    targetWidth: Dp,
    targetHeight: Dp,
    widthAnim: Animatable<Dp, AnimationVector1D>,
    heightAnim: Animatable<Dp, AnimationVector1D>,
    spec: SpringSpec<Dp> = SpringAnimations.IslandSpring
) = coroutineScope {
    launch {
        widthAnim.animateTo(
            targetValue = targetWidth,
            animationSpec = spec
        )
    }

    launch {
        heightAnim.animateTo(
            targetValue = targetHeight,
            animationSpec = spec
        )
    }
}

suspend fun morphIslandFull(
    targetWidth: Dp,
    targetHeight: Dp,
    targetRadius: Dp,
    widthAnim: Animatable<Dp, AnimationVector1D>,
    heightAnim: Animatable<Dp, AnimationVector1D>,
    radiusAnim: Animatable<Dp, AnimationVector1D>,
    spec: SpringSpec<Dp> = SpringAnimations.IslandSpring
) = coroutineScope {
    launch {
        widthAnim.animateTo(
            targetValue = targetWidth,
            animationSpec = spec
        )
    }

    launch {
        heightAnim.animateTo(
            targetValue = targetHeight,
            animationSpec = spec
        )
    }

    launch {
        radiusAnim.animateTo(
            targetValue = targetRadius,
            animationSpec = spec
        )
    }
}

@Composable
fun rememberIslandAnimatables(
    initialWidth: Dp = 120.dp,
    initialHeight: Dp = 36.dp
): Pair<Animatable<Dp, AnimationVector1D>, Animatable<Dp, AnimationVector1D>> {
    val widthAnim = remember {
        Animatable(
            initialValue = initialWidth,
            typeConverter = Dp.VectorConverter
        )
    }

    val heightAnim = remember {
        Animatable(
            initialValue = initialHeight,
            typeConverter = Dp.VectorConverter
        )
    }

    return widthAnim to heightAnim
}

@Composable
fun rememberIslandAnimatablesWithRadius(
    initialWidth: Dp = 120.dp,
    initialHeight: Dp = 36.dp,
    initialRadius: Dp = 18.dp
): Triple<
        Animatable<Dp, AnimationVector1D>,
        Animatable<Dp, AnimationVector1D>,
        Animatable<Dp, AnimationVector1D>
        > {
    val widthAnim = remember {
        Animatable(
            initialValue = initialWidth,
            typeConverter = Dp.VectorConverter
        )
    }

    val heightAnim = remember {
        Animatable(
            initialValue = initialHeight,
            typeConverter = Dp.VectorConverter
        )
    }

    val radiusAnim = remember {
        Animatable(
            initialValue = initialRadius,
            typeConverter = Dp.VectorConverter
        )
    }

    return Triple(widthAnim, heightAnim, radiusAnim)
}

@Composable
fun rememberFloatAnimatable(
    initialValue: Float = 0f
): Animatable<Float, AnimationVector1D> {
    return remember {
        Animatable(initialValue)
    }
}
