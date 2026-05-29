// File: app/src/main/java/com/miui/dynamicisland/ui/components/NotificationWidget.kt
package com.miui.dynamicisland.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.ui.states.IslandState
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import com.miui.dynamicisland.data.repository.NotificationRepository
import androidx.compose.ui.platform.LocalDensity
import android.content.Intent
import androidx.compose.ui.platform.LocalContext

private val NotifTextPrimary = Color.White
private val NotifTextSecondary = Color.White.copy(alpha = 0.6f)
private val NotifDivider = Color.White.copy(alpha = 0.12f)

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
    Box(modifier = modifier.scale(popScale).padding(start = 10.dp)) {
        IosAppIcon(
            packageName = state.packageName,
            appName = state.appName,
            size = 24.dp,
            fallbackDrawable = state.appIcon,
        )
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
    val density = LocalDensity.current
    val context = LocalContext.current
    val swipeThresholdPx = remember(density) { with(density) { 48.dp.toPx() } }
    var dragTotal by remember { mutableStateOf(0f) }
    val canNavigate = state.queueCount > 1

    val swipeModifier = if (canNavigate) {
        Modifier.pointerInput(state.queueIndex, state.queueCount) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { _, dragAmount -> dragTotal += dragAmount },
                onDragEnd = {
                    if (dragTotal > swipeThresholdPx) {
                        NotificationRepository.navigatePrevious()
                    } else if (dragTotal < -swipeThresholdPx) {
                        NotificationRepository.navigateNext()
                    }
                    dragTotal = 0f
                },
                onDragCancel = { dragTotal = 0f }
            )
        }
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(swipeModifier)
            .scale(popScale)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IosAppIcon(
                    packageName = state.packageName,
                    appName = state.appName,
                    size = 22.dp,
                    fallbackDrawable = state.appIcon
                )
                Column {
                    Text(
                        text = state.appName.ifBlank { "Notification" },
                        color = NotifTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Just now",
                        color = NotifTextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (canNavigate) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous",
                        tint = NotifTextSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { NotificationRepository.navigatePrevious() }
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next",
                        tint = NotifTextSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { NotificationRepository.navigateNext() }
                    )
                }
                Text(
                    text = "CLEAR ALL",
                    color = NotifTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { NotificationRepository.clearAll() }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = state.title.ifBlank { "Notification" },
                color = NotifTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (state.content.isNotBlank()) {
                Text(
                    text = state.content,
                    color = NotifTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionText("MARK AS READ") { NotificationRepository.markCurrentRead() }
                ActionDivider()
                ActionText("DELETE") { NotificationRepository.deleteCurrent() }
                ActionDivider()
                ActionText("REPLY") {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(state.packageName)
                    launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.NotificationsOff,
                contentDescription = "Mute",
                tint = NotifTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ActionText(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = NotifTextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 6.dp).clickable { onClick() }
    )
}

@Composable
private fun ActionDivider() {
    Box(
        modifier = Modifier
            .height(12.dp)
            .width(1.dp)
            .background(NotifDivider)
    )
}
