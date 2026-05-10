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
import androidx.compose.ui.graphics.vector.ImageVector
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
        MediaCompactWidget(state, slot, isFixed, modifier)
    }
}

// 🖼️ Image 2: Compact Media State
@Composable
private fun MediaCompactWidget(
    state: IslandState.Media,
    slot: MediaSlot,
    isFixed: Boolean,
    modifier: Modifier = Modifier
) {
    when (slot) {
        MediaSlot.LEFT -> {
            Box(modifier = modifier.padding(start = 8.dp), contentAlignment = Alignment.CenterStart) {
                if (state.albumArtUri != null) {
                    AsyncImage(
                        model = state.albumArtUri,
                        contentDescription = "Art",
                        modifier = Modifier.size(26.dp).clip(CircleShape)
                    )
                } else {
                    Icon(Icons.Default.MusicNote, null, Modifier.size(24.dp), MediaTextSecondary)
                }
            }
        }
        MediaSlot.RIGHT -> {
            val infiniteTransition = rememberInfiniteTransition(label = "waveform")
            val bar1 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(450), RepeatMode.Reverse))
            val bar2 by infiniteTransition.animateFloat(1f, 0.4f, infiniteRepeatable(tween(350), RepeatMode.Reverse))
            val bar3 by infiniteTransition.animateFloat(0.5f, 0.9f, infiniteRepeatable(tween(550), RepeatMode.Reverse))

            Box(modifier = modifier.padding(end = 8.dp), contentAlignment = Alignment.CenterEnd) {
                if (state.isPlaying) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
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

    Column(modifier = modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF1C1C1E)), contentAlignment = Alignment.Center) {
                if (state.albumArtUri != null) {
                    AsyncImage(model = state.albumArtUri, contentDescription = "Art", modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.MusicNote, null, Modifier.size(32.dp), MediaTextSecondary)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(state.title, color = MediaTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(state.artist, color = MediaTextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(4) { Box(Modifier.width(2.dp).height(12.dp).clip(CircleShape).background(Color(0xFFFD9501).copy(alpha = 0.8f))) }
            }
        }

        Spacer(Modifier.height(20.dp))

        Slider(
            value = progress,
            onValueChange = { onMediaAction(MediaAction.Seek(it)) },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = MediaTrackColor)
        )
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(state.formattedPosition, color = MediaTextSecondary, fontSize = 12.sp)
            Text(state.formattedRemainingTime, color = MediaTextSecondary, fontSize = 12.sp)
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            ControlIcon(Icons.Rounded.Replay10, onClick = {})
            ControlIcon(Icons.Default.SkipPrevious, onClick = { onMediaAction(MediaAction.Previous) }, size = 36.dp)
            Box(Modifier.size(56.dp).clip(CircleShape).background(Color.White).clickable { onMediaAction(MediaAction.PlayPause) }, contentAlignment = Alignment.Center) {
                Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, Modifier.size(32.dp), Color.Black)
            }
            ControlIcon(Icons.Default.SkipNext, onClick = { onMediaAction(MediaAction.Next) }, size = 36.dp)
            ControlIcon(Icons.Rounded.Forward10, onClick = {})
        }
    }
}

@Composable
private fun ControlIcon(icon: ImageVector, onClick: () -> Unit, size: androidx.compose.ui.unit.Dp = 28.dp) {
    IconButton(onClick = onClick, modifier = Modifier.size(size)) {
        Icon(icon, null, Modifier.size(size), Color.White)
    }
}
