// File: app/src/main/java/com/miui/dynamicisland/ui/components/MediaWidget.kt
// Purpose: Split into MediaCompactWidget and MediaExpandedWidget per Image 2 & 3
// Hinglish: Is file mein media player ki compact aur expanded UI handle hoti hai.

package com.miui.dynamicisland.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.miui.dynamicisland.ui.island.MediaAction
import com.miui.dynamicisland.ui.states.IslandState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val MediaTextPrimary = Color.White
private val MediaTextSecondary = Color(0xFF8E8E93)
private val MediaTrackColor = Color(0xFF2C2C2E)
private val MediaBackground = Color(0xFF0A0A0A)

@Composable
fun MediaWidget(
    state: IslandState.Media,
    slot: MediaSlot,
    isExpanded: Boolean = false,
    isFixed: Boolean = false,
    onMediaAction: (MediaAction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (isExpanded) {
        MediaExpandedWidget(state, onMediaAction, modifier)
    } else {
        MediaCompactWidget(state, slot, isFixed, onMediaAction, modifier)
    }
}

// 🖼️ Image 2: Compact Media State
@Composable
private fun MediaCompactWidget(
    state: IslandState.Media,
    slot: MediaSlot,
    isFixed: Boolean,
    onMediaAction: (MediaAction) -> Unit,
    modifier: Modifier = Modifier
) {
    when (slot) {
        MediaSlot.LEFT -> {
            Box(modifier = modifier.padding(start = 10.dp), contentAlignment = Alignment.CenterStart) {
                if (state.albumArt != null) {
                    Image(
                        bitmap = state.albumArt.asImageBitmap(),
                        contentDescription = "Art",
                        modifier = Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else if (state.albumArtUri != null) {
                    AsyncImage(
                        model = state.albumArtUri,
                        contentDescription = "Art",
                        modifier = Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    if (state.packageName.isNotBlank()) {
                        IosAppIcon(
                            packageName = state.packageName,
                            appName = state.packageName.substringAfterLast('.').ifBlank { "Music" },
                            size = 24.dp
                        )
                    } else {
                        IosGlyphIcon(
                            icon = Icons.Default.MusicNote,
                            contentDescription = "Music",
                            containerSize = 24.dp,
                            iconSize = 16.dp,
                            tint = MediaTextSecondary
                        )
                    }
                }
            }
        }
        MediaSlot.RIGHT -> {
            val infiniteTransition = rememberInfiniteTransition(label = "waveform")
            val bar1 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(450), RepeatMode.Reverse))
            val bar2 by infiniteTransition.animateFloat(1f, 0.4f, infiniteRepeatable(tween(350), RepeatMode.Reverse))
            val bar3 by infiniteTransition.animateFloat(0.5f, 0.9f, infiniteRepeatable(tween(550), RepeatMode.Reverse))

            Box(
                modifier = modifier
                    .padding(end = 10.dp)
                    .clickable { onMediaAction(MediaAction.PlayPause) },
                contentAlignment = Alignment.CenterEnd
            ) {
                if (state.isPlaying) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WaveformBar(bar1)
                        WaveformBar(bar2)
                        WaveformBar(bar3)
                    }
                } else {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp), MediaTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun WaveformBar(heightFactor: Float) {
    Box(Modifier.width(2.5.dp).height(16.dp * heightFactor).clip(RoundedCornerShape(2.dp)).background(MediaTextSecondary))
}

// 🖼️ Image 3: Expanded Media Player
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MediaExpandedWidget(
    state: IslandState.Media,
    onMediaAction: (MediaAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (state.duration > 0) (state.position.toFloat() / state.duration) else 0f
    val stateManager = remember { com.miui.dynamicisland.manager.IslandStateManager.getInstance() }

    // Dynamic dominant color extraction
    var dominantColor by remember { mutableStateOf(Color(0xFF3B82F6)) }
    LaunchedEffect(state.albumArt) {
        state.albumArt?.let { bitmap ->
            withContext(Dispatchers.Default) {
                val palette = Palette.from(bitmap).maximumColorCount(16).generate()
                val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch
                swatch?.rgb?.let { colorInt ->
                    dominantColor = Color(colorInt)
                }
            }
        }
    }
    
    val glowBrush = remember(dominantColor) {
        Brush.radialGradient(
            colors = listOf(
                dominantColor.copy(alpha = 0.15f),
                dominantColor.copy(alpha = 0.05f),
                Color.Transparent
            ),
            radius = 600f
        )
    }

    // EQ animation
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    val eqBars = List(4) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 600 + index * 200,
                    easing = LinearOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eq$index"
        )
    }

    // Album art breathing animation - Smoother & more organic
    val albumScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .combinedClickable(
                onClick = { stateManager.collapseCurrentState() },
                onDoubleClick = { onMediaAction(MediaAction.LaunchApp) }
            )
    ) {
        // Layer 0: Optional blurred background (very subtle)
        if (state.albumArt != null) {
            Image(
                bitmap = state.albumArt.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp)
                    .alpha(0.07f),
                colorFilter = ColorFilter.tint(Color.Black, BlendMode.Multiply)
            )
        } else if (state.albumArtUri != null) {
            AsyncImage(
                model = state.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp)
                    .alpha(0.07f),
                colorFilter = ColorFilter.tint(Color.Black, BlendMode.Multiply)
            )
        }

        // Layer 1: Ambient glow behind album art & background accents
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (state.isPlaying) (glowAlpha * 0.375f) else 0.20f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            dominantColor.copy(alpha = 0.8f),
                            dominantColor.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(200f, 150f),
                        radius = 1200f
                    )
                )
        )

        Box(
            modifier = Modifier
                .size(600.dp)
                .offset(x = (-150).dp, y = (-100).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            dominantColor.copy(alpha = 0.25f),
                            dominantColor.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .align(Alignment.TopStart)
                .blur(60.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Layer 2 & 3: Album Art + Song Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Glow shadow - adapted to dominant color with pulsing effect
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .graphicsLayer {
                                scaleX = if (state.isPlaying) albumScale * 1.02f else 1f
                                scaleY = if (state.isPlaying) albumScale * 1.02f else 1f
                                alpha = if (state.isPlaying) glowAlpha else 0.4f
                            }
                            .shadow(
                                elevation = 16.dp,
                                shape = CircleShape,
                                ambientColor = dominantColor,
                                spotColor = dominantColor
                            )
                            .background(dominantColor.copy(alpha = 0.15f), CircleShape)
                    )

                    // Actual image with breathing
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer {
                                val s = if (state.isPlaying) albumScale else 1f
                                scaleX = s
                                scaleY = s
                                // Subtle dynamic shadow depth based on scale
                                shadowElevation = (8 * s).dp.toPx()
                            }
                            .clip(CircleShape)
                    ) {
                        if (state.albumArt != null) {
                            Image(
                                bitmap = state.albumArt.asImageBitmap(),
                                contentDescription = "Art",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (state.albumArtUri != null) {
                            AsyncImage(
                                model = state.albumArtUri,
                                contentDescription = "Art",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF1C1C1E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MusicNote, null, Modifier.size(32.dp), MediaTextSecondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Song Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = state.title,
                        color = MediaTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = state.artist,
                        color = MediaTextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // EQ Indicator
                if (state.isPlaying) {
                    Row(
                        modifier = Modifier.size(20.dp, 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        eqBars.forEach { barHeight ->
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height((6 + barHeight.value * 10).dp)
                                    .background(
                                        color = dominantColor.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Layer 5: Progress Bar
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Slider(
                    value = progress,
                    onValueChange = { onMediaAction(MediaAction.Seek(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = dominantColor.copy(alpha = 0.8f),
                        inactiveTrackColor = MediaTrackColor
                    ),
                    thumb = {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Time labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = state.formattedPosition,
                        color = MediaTextSecondary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = state.formattedRemainingTime,
                        color = MediaTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Layer 6: Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val durationMs = state.duration.coerceAtLeast(0L)
                fun seekBy(deltaMs: Long) {
                    if (durationMs <= 0L) return
                    val newPosition = (state.position + deltaMs).coerceIn(0L, durationMs)
                    onMediaAction(MediaAction.Seek(newPosition.toFloat() / durationMs.toFloat()))
                }

                GlassButton(
                    onClick = { seekBy(-10_000L) },
                    icon = { Icon(Icons.Default.Replay10, null, Modifier.size(20.dp), Color.White) },
                    size = 42.dp
                )

                GlassButton(
                    onClick = { onMediaAction(MediaAction.Previous) },
                    icon = { Icon(Icons.Default.SkipPrevious, null, Modifier.size(20.dp), Color.White) },
                    size = 42.dp
                )

                GlassButton(
                    onClick = { onMediaAction(MediaAction.PlayPause) },
                    icon = {
                        Icon(
                            if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            null,
                            Modifier.size(28.dp),
                            Color.White
                        )
                    },
                    size = 56.dp,
                    isPrimary = true
                )

                GlassButton(
                    onClick = { onMediaAction(MediaAction.Next) },
                    icon = { Icon(Icons.Default.SkipNext, null, Modifier.size(20.dp), Color.White) },
                    size = 42.dp
                )

                GlassButton(
                    onClick = { seekBy(10_000L) },
                    icon = { Icon(Icons.Default.Forward10, null, Modifier.size(20.dp), Color.White) },
                    size = 42.dp
                )
            }
        }

        // Layer 7: Subtle vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.15f)
                        ),
                        radius = 800f
                    )
                )
        )
    }
}

@Composable
private fun GlassButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    size: Dp,
    isPrimary: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "press"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(
                color = Color.White.copy(
                    alpha = if (isPressed) 0.15f else 0.08f
                )
            )
            .border(
                width = 1.dp,
                color = if (isPrimary) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.12f),
                shape = CircleShape
            )
            .shadow(
                elevation = if (isPrimary) 4.dp else 2.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.2f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun ControlIcon(icon: ImageVector, onClick: () -> Unit, size: androidx.compose.ui.unit.Dp = 28.dp) {
    IconButton(onClick = onClick, modifier = Modifier.size(size)) {
        Icon(icon, null, Modifier.size(size), Color.White)
    }
}
