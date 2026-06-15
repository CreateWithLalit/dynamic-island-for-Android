// File: app/src/main/java/com/miui/dynamicisland/ui/components/ProgressWidget.kt
package com.miui.dynamicisland.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.ui.states.IslandState

private val ProgressBlue = Color(0xFF0A84FF)
private val ProgressGreen = Color(0xFF30D158)

@Composable
fun ProgressWidget(
    state: IslandState.Progress,
    slot: ProgressSlot,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isExpanded) {
        ProgressExpandedWidget(state, modifier)
    } else {
        when (slot) {
            ProgressSlot.LEFT -> ProgressLeftSlot(state, modifier)
            ProgressSlot.RIGHT -> ProgressRightSlot(state, modifier)
        }
    }
}

@Composable
private fun ProgressLeftSlot(state: IslandState.Progress, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(start = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Icon(
            imageVector = if (state.isDownload) Icons.Default.Download else Icons.Default.Upload,
            contentDescription = null,
            tint = if (state.isDownload) ProgressBlue else ProgressGreen,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ProgressRightSlot(state: IslandState.Progress, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(end = 12.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = "${(state.progress * 100).toInt()}%",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProgressExpandedWidget(state: IslandState.Progress, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.appName,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
            
            if (state.remainingTime.isNotBlank()) {
                Text(
                    text = state.remainingTime,
                    color = ProgressBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (state.isDownload) ProgressBlue else ProgressGreen,
                trackColor = Color.White.copy(alpha = 0.1f),
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "${(state.progress * 100).toInt()}% complete",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}
