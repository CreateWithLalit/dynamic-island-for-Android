// File: app/src/main/java/com/miui/dynamicisland/ui/components/MediaWidget.kt
// Purpose: Split into MediaCompactWidget and MediaExpandedWidget per Image 2 & 3
// Hinglish: Is file mein media player ki compact aur expanded UI handle hoti hai.

package com.miui.dynamicisland.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.miui.dynamicisland.ui.island.MediaAction
import com.miui.dynamicisland.ui.states.IslandState

private val MediaTextPrimary = Color.White
private val MediaTextSecondary = Color.White.copy(alpha = 0.6f)
private val MediaTrackColor = Color.White.copy(alpha = 0.2f)

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
    Box(Modifier.width(2.5.dp).height(16.dp * heightFactor).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFD9501)))
}

// 🖼️ Image 3: Expanded Media Player
@Composable
private fun MediaExpandedWidget(
    state: IslandState.Media,
    onMediaAction: (MediaAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (state.duration > 0) (state.position.toFloat() / state.duration) else 0f
    val waveformColor = Color(0xFFFF3B30)
    val metaGrey = Color(0xFFCFC4C5)
    val trackDark = Color(0xFF2A2A2E)

    val infiniteTransition = rememberInfiniteTransition(label = "expanded_waveform")
    val bar1 by infiniteTransition.animateFloat(0.35f, 1f, infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse))
    val bar2 by infiniteTransition.animateFloat(1f, 0.45f, infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse))
    val bar3 by infiniteTransition.animateFloat(0.55f, 0.9f, infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse))

    Column(modifier = modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier.size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1C1C1E)),
                contentAlignment = Alignment.Center
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
                    if (state.packageName.isNotBlank()) {
                        IosAppIcon(
                            packageName = state.packageName,
                            appName = state.packageName.substringAfterLast('.').ifBlank { "Music" },
                            size = 56.dp,
                            contentPadding = 4.dp
                        )
                    } else {
                        IosGlyphIcon(
                            icon = Icons.Default.MusicNote,
                            contentDescription = "Music",
                            containerSize = 56.dp,
                            iconSize = 28.dp,
                            tint = MediaTextSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.title,
                    color = MediaTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    state.artist,
                    color = metaGrey,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(3.dp).height(18.dp * bar1).clip(RoundedCornerShape(2.dp)).background(waveformColor))
                Box(Modifier.width(3.dp).height(18.dp * bar2).clip(RoundedCornerShape(2.dp)).background(waveformColor))
                Box(Modifier.width(3.dp).height(18.dp * bar3).clip(RoundedCornerShape(2.dp)).background(waveformColor))
            }
        }

        Spacer(Modifier.height(14.dp))

        Slider(
            value = progress,
            onValueChange = { onMediaAction(MediaAction.Seek(it)) },
            modifier = Modifier.fillMaxWidth().height(1.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.White,
                inactiveTrackColor = trackDark
            )
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(state.formattedPosition, color = metaGrey, fontSize = 10.sp)
            Text(state.formattedRemainingTime, color = metaGrey, fontSize = 10.sp)
        }

        Spacer(Modifier.height(14.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onMediaAction(MediaAction.Previous) }) {
                Icon(Icons.Default.SkipPrevious, null, Modifier.size(28.dp), Color.White)
            }
            IconButton(onClick = { onMediaAction(MediaAction.PlayPause) }) {
                Icon(
                    if (state.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    null,
                    Modifier.size(44.dp),
                    Color.White
                )
            }
            IconButton(onClick = { onMediaAction(MediaAction.Next) }) {
                Icon(Icons.Default.SkipNext, null, Modifier.size(28.dp), Color.White)
            }
        }
    }
}

@Composable
private fun ControlIcon(icon: ImageVector, onClick: () -> Unit, size: androidx.compose.ui.unit.Dp = 28.dp) {
    IconButton(onClick = onClick, modifier = Modifier.size(size)) {
        Icon(icon, null, Modifier.size(size), Color.White)
    }
}
