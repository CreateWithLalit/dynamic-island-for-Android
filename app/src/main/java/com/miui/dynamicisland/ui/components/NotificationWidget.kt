// File: app/src/main/java/com/miui/dynamicisland/ui/components/NotificationWidget.kt
package com.miui.dynamicisland.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.miui.dynamicisland.ui.states.IslandState

private val NotifTextPrimary = Color.White
private val NotifTextSecondary = Color.White.copy(alpha = 0.6f)
private val NotifIconBg = Color(0xFF2C2C2E)

@Composable
fun NotificationWidget(
    state: IslandState.Notification,
    slot: NotificationSlot? = null,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isExpanded) {
        NotificationBottomBanner(state, modifier)
        return
    }

    when (slot) {
        NotificationSlot.LEFT -> NotificationLeftSlot(state, modifier)
        NotificationSlot.RIGHT -> Unit
        null -> NotificationBottomBanner(state, modifier)
    }
}

@Composable
private fun NotificationLeftSlot(
    state: IslandState.Notification,
    modifier: Modifier = Modifier
) {
    var appeared by remember { mutableStateOf(false) }
    val popScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.7f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)
    )
    LaunchedEffect(state.packageName, state.postTime) { appeared = false; appeared = true }
    Box(modifier = modifier.scale(popScale).padding(start = 8.dp)) {
        NotificationAppIcon(state.appIcon, state.appName, size = 28)
    }
}

@Composable
private fun NotificationBottomBanner(
    state: IslandState.Notification,
    modifier: Modifier = Modifier
) {
    var appeared by remember { mutableStateOf(false) }
    val popScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.7f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)
    )
    LaunchedEffect(state.packageName, state.postTime) { appeared = false; appeared = true }
    Row(
        modifier = modifier.wrapContentWidth().scale(popScale),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NotificationAppIcon(state.appIcon, state.appName, size = 32)
        Column(modifier = Modifier.widthIn(max = 240.dp)) {
            Text(state.appName, color = NotifTextSecondary, fontSize = 12.sp,
                fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(state.title.ifBlank { "Notification" }, color = NotifTextPrimary, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (state.content.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(state.content, color = NotifTextSecondary, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun NotificationAppIcon(drawable: Drawable?, appName: String, size: Int = 28) {
    val bitmap = remember(drawable) {
        runCatching { drawable?.toBitmap(width = size, height = size) }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = appName,
            modifier = Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.25f).dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.25f).dp)).background(NotifIconBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appName.firstOrNull()?.uppercase() ?: "?",
                color = NotifTextSecondary,
                fontSize = (size * 0.45f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
