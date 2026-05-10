package com.miui.dynamicisland.ui.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.miui.dynamicisland.ui.states.IslandState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun IslandContentTransition(
    targetState: IslandState,
    modifier: Modifier = Modifier,
    content: @Composable (IslandState) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        label = "island_content_transition",
        transitionSpec = {
            val isExpanding = targetState.priority > initialState.priority

            val enterTransition = if (isExpanding) {
                slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) { height -> height } + fadeIn(animationSpec = tween(200))
            } else {
                slideInVertically(
                    animationSpec = tween(200)
                ) { height -> -height } + fadeIn(animationSpec = tween(200))
            }

            val exitTransition = if (isExpanding) {
                slideOutVertically(
                    animationSpec = tween(150)
                ) { height -> -height } + fadeOut(animationSpec = tween(150))
            } else {
                slideOutVertically(
                    animationSpec = tween(150)
                ) { height -> height } + fadeOut(animationSpec = tween(150))
            }

            enterTransition
                .togetherWith(exitTransition)
                .using(
                    SizeTransform(clip = true) { _, _ ->
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    }
                )
        }
    ) { state ->
        content(state)
    }
}
