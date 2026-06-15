// File: app/src/main/java/com/miui/dynamicisland/ui/components/ClipboardWidget.kt
package com.miui.dynamicisland.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.ui.island.ClipboardAction
import com.miui.dynamicisland.ui.states.IslandState

private val ClipboardBlue = Color(0xFF007AFF)

@Composable
fun ClipboardWidget(
    state: IslandState.Clipboard,
    slot: ClipboardSlot,
    isExpanded: Boolean = false,
    onAction: (ClipboardAction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (isExpanded) {
        ClipboardExpandedWidget(state, onAction, modifier)
    } else {
        when (slot) {
            ClipboardSlot.LEFT -> ClipboardLeftSlot(state, modifier)
            ClipboardSlot.RIGHT -> ClipboardRightSlot(state, modifier)
        }
    }
}

@Composable
private fun ClipboardLeftSlot(state: IslandState.Clipboard, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(start = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = null,
            tint = ClipboardBlue,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ClipboardRightSlot(state: IslandState.Clipboard, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(end = 12.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = "Copied",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ClipboardExpandedWidget(
    state: IslandState.Clipboard,
    onAction: (ClipboardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Recently Copied",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = state.text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClipboardActionButton(
                icon = Icons.Default.Search,
                label = "Search",
                onClick = { onAction(ClipboardAction.Search) }
            )
            ClipboardActionButton(
                icon = Icons.Default.Translate,
                label = "Translate",
                onClick = { onAction(ClipboardAction.Translate) }
            )
            ClipboardActionButton(
                icon = Icons.Default.Share,
                label = "Share",
                onClick = { onAction(ClipboardAction.Share) }
            )
        }
    }
}

@Composable
private fun ClipboardActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
    }
}
