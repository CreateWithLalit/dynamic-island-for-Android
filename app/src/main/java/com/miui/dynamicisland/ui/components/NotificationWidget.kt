// File: app/src/main/java/com/miui/dynamicisland/ui/components/NotificationWidget.kt
package com.miui.dynamicisland.ui.components

import android.app.Notification
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.data.repository.NotificationRepository
import com.miui.dynamicisland.manager.IslandStateManager
import com.miui.dynamicisland.service.IslandNotificationListener
import com.miui.dynamicisland.ui.states.IslandState
import java.util.Locale

private val NotifTextPrimary = Color.White
private val NotifTextSecondary = Color.White.copy(alpha = 0.6f)
private val NotifChipBg = Color(0xFF1F2937)

@Composable
fun NotificationWidget(
    state: IslandState.Notification,
    slot: NotificationSlot? = null,
    isExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isExpanded) {
        NotificationExpandedContent(state, modifier)
        return
    }

    when (slot) {
        NotificationSlot.LEFT -> NotificationLeftSlot(state, modifier)
        NotificationSlot.RIGHT -> Unit
        null -> NotificationExpandedContent(state, modifier)
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
    val queueState by NotificationRepository.notifications.collectAsState()
    val maxVisibleIcons = 3

    Box(
        modifier = modifier
            .scale(popScale)
            .padding(start = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            queueState.items.take(maxVisibleIcons).forEachIndexed { index, item ->
                Box(
                    modifier = Modifier.offset(x = if (index == 0) 0.dp else (-8).dp)
                ) {
                    IosAppIcon(
                        packageName = item.packageName,
                        appName = item.appName,
                        size = 26.dp,
                        fallbackDrawable = item.appIcon,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationExpandedContent(
    state: IslandState.Notification,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appLabel = remember(state.appName) {
        if (state.appName.contains(".") || state.appName.all { it.isUpperCase() }) {
            state.appName.substringAfterLast('.').lowercase().replaceFirstChar { it.uppercase() }
        } else {
            state.appName
        }
    }
    val displayLabel = appLabel.uppercase(Locale.getDefault())
    val canNavigate = state.queueCount > 1
    
    val replyAction: Notification.Action? = remember(state.actions) {
        state.actions?.firstOrNull { action ->
            val title = action.title?.toString()?.lowercase(Locale.getDefault()) ?: ""
            action.remoteInputs?.isNotEmpty() == true && 
                (title.contains("reply") || title.contains("answer") || title.contains("write") || title.contains("message"))
        } ?: state.actions?.firstOrNull { it.remoteInputs?.isNotEmpty() == true }
    }
    
    val isReplying = state.isReplying
    var replyText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val swipeThresholdPx = remember(density) { with(density) { 24.dp.toPx() } }
    var dragTotal by remember { mutableStateOf(0f) }

    val swipeModifier = if (canNavigate && !isReplying) {
        Modifier.pointerInput(state.notificationKey) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    dragTotal += dragAmount
                },
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
    } else Modifier

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(swipeModifier)
            .clickable(
                enabled = !isReplying,
                onClick = {
                    IslandStateManager.getInstance().collapseCurrentState()
                    try {
                        state.contentIntent?.send()
                    } catch (e: Exception) {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(state.packageName)
                        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        if (launchIntent != null) context.startActivity(launchIntent)
                    }
                }
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Row: App info, Clear All, Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IosAppIcon(
                    packageName = state.packageName,
                    appName = state.appName,
                    size = 18.dp,
                    fallbackDrawable = state.appIcon,
                    contentPadding = 0.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$displayLabel • Just now",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isReplying) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF374151), RoundedCornerShape(50))
                            .clickable {
                                IslandNotificationListener.cancelAllPosted()
                                NotificationRepository.clearAll()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("CLEAR ALL", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    if (canNavigate) {
                        ArrowButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft) { NotificationRepository.navigatePrevious() }
                        ArrowButton(Icons.AutoMirrored.Filled.KeyboardArrowRight) { NotificationRepository.navigateNext() }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp).clickable { 
                            IslandStateManager.getInstance().pushState(state.copy(isReplying = false))
                            replyText = ""
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }

        // Animated Content for Title and Content OR Reply Field
        AnimatedContent(
            targetState = isReplying,
            transitionSpec = {
                (fadeIn() + expandVertically()).togetherWith(fadeOut() + shrinkVertically())
            },
            label = "reply_field_anim"
        ) { replying ->
            if (replying && replyAction != null) {
                // IN-LINE REPLY FIELD
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Type a reply...", color = Color.Gray, fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2C2C2E),
                            unfocusedContainerColor = Color(0xFF2C2C2E),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(22.dp),
                        textStyle = TextStyle(fontSize = 15.sp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(onSend = {
                            if (replyText.isNotBlank()) {
                                executeDirectReply(context, replyAction, replyText)
                                // We keep it in the island until manual delete
                                replyText = ""
                                IslandStateManager.getInstance().pushState(state.copy(isReplying = false))
                                focusManager.clearFocus()
                            }
                        })
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                executeDirectReply(context, replyAction, replyText)
                                // We keep it in the island until manual delete
                                replyText = ""
                                IslandStateManager.getInstance().pushState(state.copy(isReplying = false))
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier.size(44.dp).background(Color(0xFF30D158), CircleShape)
                    ) {
                        Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                // NORMAL NOTIFICATION CONTENT
                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        if (targetState.queueIndex > initialState.queueIndex) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut())
                        }.using(
                            SizeTransform(clip = false)
                        )
                    },
                    label = "notif_transition"
                ) { target ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IosAppIcon(
                            packageName = target.packageName,
                            appName = target.appName,
                            size = 52.dp,
                            fallbackDrawable = target.appIcon
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = target.title.ifBlank { "Notification" },
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (target.content.isNotBlank()) {
                                Text(
                                    text = target.content,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Row: Actions + Bell
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.isMessage && !isReplying) {
                    ActionChip("MARK AS READ") {
                        val readAction = state.actions?.firstOrNull {
                            val title = it.title?.toString()?.uppercase(Locale.getDefault()) ?: ""
                            title.contains("READ") || title.contains("DONE") || title.contains("SEEN")
                        }
                        try {
                            readAction?.actionIntent?.send()
                        } catch (_: Exception) {
                        }
                        // We keep it in the island until manual delete
                    }
                    ActionChip("DELETE") {
                        val deleteAction = state.actions?.firstOrNull {
                            val title = it.title?.toString()?.uppercase(Locale.getDefault()) ?: ""
                            title.contains("DELETE") || title.contains("DISMISS") || title.contains("CLEAR")
                        }
                        try {
                            deleteAction?.actionIntent?.send()
                        } catch (_: Exception) {
                        }
                        IslandNotificationListener.cancelByKey(state.notificationKey)
                        NotificationRepository.deleteCurrent()
                    }
                    ActionChip("REPLY") {
                        if (replyAction != null) {
                            IslandStateManager.getInstance().pushState(state.copy(isReplying = true))
                        } else {
                            val launchIntent =
                                context.packageManager.getLaunchIntentForPackage(state.packageName)
                            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            if (launchIntent != null) context.startActivity(launchIntent)
                        }
                    }
                } else if (!isReplying) {
                    // Non-messaging app actions (General actions if any)
                    state.actions?.take(2)?.forEach { action ->
                        ActionChip(action.title.toString().uppercase(Locale.getDefault())) {
                            try {
                                action.actionIntent?.send()
                            } catch (_: Exception) {}
                            if (action.title.toString().contains("DELETE", ignoreCase = true) || 
                                action.title.toString().contains("DISMISS", ignoreCase = true)) {
                                IslandNotificationListener.cancelByKey(state.notificationKey)
                                NotificationRepository.deleteCurrent()
                            }
                        }
                    }
                }
            }
            if (!isReplying) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Mute",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun ArrowButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(Color(0xFF374151), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ActionChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(NotifChipBg, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun executeDirectReply(context: android.content.Context, action: Notification.Action, messageText: String) {
    val remoteInputs = action.remoteInputs ?: return
    val resultsBundle = Bundle().apply {
        remoteInputs.forEach { input -> putCharSequence(input.resultKey, messageText) }
    }
    val fillInIntent = Intent().apply {
        RemoteInput.addResultsToIntent(remoteInputs, this, resultsBundle)
    }
    try {
        action.actionIntent.send(context, 0, fillInIntent)
    } catch (_: Exception) {}
}
