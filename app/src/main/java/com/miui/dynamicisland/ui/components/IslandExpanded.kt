// File: app/src/main/java/com/miui/dynamicisland/ui/components/IslandExpanded.kt
// Purpose: Expanded card view with Apple HIG text sizes (14-15sp primary)
// Hinglish: Expanded state ke liye neeche ek card dikhta hai.
//
// FIXES:
//  - Icons.Default.WaterDrop → Icons.Default.WaterDrop (extended – already in build.gradle ✓)
//  - Icons.Default.FlashOn  → Icons.Outlined.Bolt (extended, safe)

package com.miui.dynamicisland.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.automirrored.filled.*
import com.miui.dynamicisland.ui.states.IslandState
import java.util.Locale
import java.text.DateFormat
import java.util.Date
import androidx.compose.ui.platform.LocalContext
import com.miui.dynamicisland.data.repository.NotificationRepository
import android.content.Intent
import android.content.Context
import android.app.Notification
import android.app.RemoteInput
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.miui.dynamicisland.service.IslandNotificationListener
import com.miui.dynamicisland.manager.IslandStateManager
import com.miui.dynamicisland.ui.timer.ExpandedTimerView

private val ExpandedSurface  = Color(0xFF0A0A0A)
private val ExpandedDivider  = Color(0xFF2C2C2E)
private val TextPrimary      = Color.White
private val TextSecondary    = Color(0xFF8E8E93)
private val AccentGreen      = Color(0xFF30D158)
private val AccentOrange     = Color(0xFFFF9F0A)

@Composable
fun IslandExpanded(
    state: IslandState,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
        )
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessLow
            ),
            expandFrom = Alignment.Top
        ) + fadeIn(animationSpec = tween(200)),
        exit = shrinkVertically(
            animationSpec = tween(180),
            shrinkTowards = Alignment.Top
        ) + fadeOut(animationSpec = tween(180))
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp)
        ) {
            when (state) {
                is IslandState.Media        -> ExpandedMedia(state)
                is IslandState.Call         -> ExpandedCall(state)
                is IslandState.Notification -> ExpandedNotification(state, onDismiss)
                is IslandState.Charging     -> ExpandedCharging(state)
                is IslandState.Bluetooth    -> ExpandedBluetooth(state)
                is IslandState.Weather      -> ExpandedWeather(state)
                is IslandState.Timer        -> ExpandedTimer(state)
                is IslandState.Idle,
                is IslandState.Silent,
                is IslandState.Volume,
                is IslandState.LockScreen,
                is IslandState.Progress     -> Unit
                is IslandState.Navigation   -> ExpandedNavigation(state)
                is IslandState.Clipboard    -> ExpandedClipboard(state, onDismiss)
            }
        }
    }
}

// ── Individual expanded cards ─────────────────────────────────────────────────

@Composable
private fun ExpandedClipboard(state: IslandState.Clipboard, onDismiss: () -> Unit) {
    val context = LocalContext.current
    
    ExpandedCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Recently Copied",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = state.text,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = ExpandedDivider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ClipboardActionButton(Icons.Default.Search, "Search") {
                    handleClipboardAction(context, state.text, "search")
                    IslandStateManager.getInstance().removeState(IslandState.Clipboard::class.java)
                    onDismiss()
                }
                ClipboardActionButton(Icons.Default.Translate, "Translate") {
                    handleClipboardAction(context, state.text, "translate")
                    IslandStateManager.getInstance().removeState(IslandState.Clipboard::class.java)
                    onDismiss()
                }
                ClipboardActionButton(Icons.Default.Share, "Share") {
                    handleClipboardAction(context, state.text, "share")
                    IslandStateManager.getInstance().removeState(IslandState.Clipboard::class.java)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun ClipboardActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 4.dp)) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ExpandedDivider)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = TextPrimary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

private fun handleClipboardAction(context: Context, text: String, action: String) {
    val intent = when (action) {
        "search" -> Intent(Intent.ACTION_WEB_SEARCH).apply { putExtra(android.app.SearchManager.QUERY, text) }
        "translate" -> Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://translate.google.com/?sl=auto&tl=en&text=${android.net.Uri.encode(text)}&op=translate"))
        "share" -> Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        else -> return
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        if (action == "share") {
            val chooser = Intent.createChooser(intent, "Share via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } else {
            context.startActivity(intent)
        }
    } catch (_: Exception) {}
}

@Composable
private fun ExpandedMedia(state: IslandState.Media) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation    = 24.dp,
                shape        = RoundedCornerShape(38.dp),
                ambientColor = Color.Black.copy(alpha = 0.6f),
                spotColor    = Color.Black.copy(alpha = 0.6f)
            )
            .clip(RoundedCornerShape(38.dp))
            .background(ExpandedSurface)
    ) {
        MediaWidget(
            state = state,
            slot = MediaSlot.LEFT,
            isExpanded = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ExpandedCall(state: IslandState.Call) {
    ExpandedCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ExpandedDivider),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Call, "Call", Modifier.size(24.dp), TextPrimary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.callerName.ifBlank { "Unknown" },
                    color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (state.isOngoing) formatDuration(state.duration) else "Incoming call",
                    color = if (state.isOngoing) AccentGreen else TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        if (state.isIncoming || state.isOngoing) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = ExpandedDivider, thickness = 0.5.dp)
            Spacer(Modifier.height(12.dp))
            CallWidget(state = state, slot = CallSlot.BOTTOM)
        }
    }
}

@Composable
private fun ExpandedNotification(state: IslandState.Notification, onDismiss: () -> Unit) {
    val timeText = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(state.postTime))
    val canNavigate = state.queueCount > 1
    val context = LocalContext.current
    val appLabel = state.appName.ifBlank { "App" }.uppercase(Locale.getDefault())
    val interactionSource = remember { MutableInteractionSource() }
    val replyAction: android.app.Notification.Action? = remember(state.actions) {
        state.actions?.firstOrNull { it.remoteInputs?.isNotEmpty() == true }
    }
    val focusManager = LocalFocusManager.current
    val view = LocalView.current

    ExpandedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    try {
                        state.contentIntent?.send()
                    } catch (_: Exception) {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(state.packageName)
                        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        }
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                    ) {
                        IosAppIcon(
                            packageName = state.packageName,
                            appName = state.appName.ifBlank { "App" },
                            size = 16.dp,
                            contentPadding = 0.dp,
                            fallbackDrawable = state.appIcon
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$appLabel • $timeText",
                        color = Color(0xFFD1D5DB),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CLEAR ALL",
                        color = Color(0xFF6B7280),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            IslandNotificationListener.cancelAllPosted()
                            NotificationRepository.clearAll()
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    if (canNavigate) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFF374151), CircleShape)
                                .clickable { NotificationRepository.navigatePrevious() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous",
                                tint = Color.LightGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFF374151), CircleShape)
                                .clickable { NotificationRepository.navigateNext() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next",
                                tint = Color.LightGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = state.title.ifBlank { "Notification" },
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (state.content.isNotBlank()) {
                    Text(
                        text = state.content,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = timeText,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            if (state.isReplying && replyAction != null) {
                // Placeholder that launches ReplyActivity
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(48.dp)
                        .background(Color(0xFF2C2C2E), RoundedCornerShape(24.dp))
                        .clickable {
                            onDismiss() // Dismiss the expanded overlay
                            com.miui.dynamicisland.ui.ReplyActivity.launch(
                                context,
                                state.appName,
                                state.notificationKey
                            )
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reply to ${state.title}...",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color(0xFF30D158),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(Color(0xFF1F2937), RoundedCornerShape(50))
                                .clickable {
                                    val readAction = state.actions?.firstOrNull {
                                        it.title?.toString()?.contains("READ", ignoreCase = true) == true
                                    }
                                    try {
                                        readAction?.actionIntent?.send()
                                    } catch (_: Exception) {
                                        // no-op
                                    }
                                    IslandNotificationListener.cancelByKey(state.notificationKey)
                                    NotificationRepository.markCurrentRead()
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("MARK AS READ", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(Color(0xFF1F2937), RoundedCornerShape(50))
                                .clickable {
                                    val deleteAction = state.actions?.firstOrNull {
                                        val title = it.title?.toString() ?: ""
                                        title.contains("DELETE", ignoreCase = true) ||
                                                title.contains("DISMISS", ignoreCase = true) ||
                                                title.contains("CLEAR", ignoreCase = true)
                                    }
                                    try {
                                        deleteAction?.actionIntent?.send()
                                    } catch (_: Exception) {
                                        // no-op
                                    }
                                    IslandNotificationListener.cancelByKey(state.notificationKey)
                                    NotificationRepository.deleteCurrent()
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("DELETE", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1F2937), RoundedCornerShape(50))
                                .clickable {
                                    if (replyAction != null) {
                                        onDismiss() // Dismiss the expanded overlay
                                        com.miui.dynamicisland.ui.ReplyActivity.launch(
                                            context,
                                            state.appName,
                                            state.notificationKey
                                        )
                                    } else {
                                        val launchIntent = context.packageManager.getLaunchIntentForPackage(state.packageName)
                                        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        if (launchIntent != null) {
                                            context.startActivity(launchIntent)
                                        }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            val btnText = if (replyAction != null) "REPLY" else "OPEN"
                            Text(btnText, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Mute",
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { }
                    )
                }
            }
        }
    }
}

private fun executeDirectReply(context: android.content.Context, action: Notification.Action, messageText: String) {
    val remoteInputs = action.remoteInputs ?: return
    val resultsBundle = Bundle().apply {
        remoteInputs.forEach { input ->
            putCharSequence(input.resultKey, messageText)
        }
    }
    val fillInIntent = Intent().apply {
        RemoteInput.addResultsToIntent(remoteInputs, this, resultsBundle)
    }
    try {
        action.actionIntent.send(context, 0, fillInIntent)
    } catch (_: Exception) {
        // no-op
    }
}

@Composable
private fun ExpandedCharging(state: IslandState.Charging) {
    val methodLabel = when (state.chargeMethod) {
        IslandState.Charging.ChargeMethod.WIRELESS -> "Wireless Charging"
        IslandState.Charging.ChargeMethod.WIRED -> {
            if (state.wattage > 0) "${state.wattage}W Turbo Charging" else "Wired Charging"
        }
        else -> "Battery"
    }

    ExpandedCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = methodLabel,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${state.batteryLevel}% Charged",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            LiquidBatteryIcon(
                level = state.batteryLevel.coerceIn(0, 100),
                isCharging = state.isCharging,
                modifier = Modifier.size(width = 60.dp, height = 30.dp)
            )
        }
    }
}

@Composable
private fun ExpandedBluetooth(state: IslandState.Bluetooth) {
    ExpandedCard {
        BluetoothExpandedWidget(state = state)
    }
}

@Composable
private fun ExpandedNavigation(state: IslandState.Navigation) {
    ExpandedCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.street.ifBlank { "Navigation" },
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (state.toward.isNotBlank()) {
                    Text(
                        text = "toward ${state.toward}",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Green Action Button
                    Row(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF34C759))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getDirectionIcon(state.direction),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = state.distance,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Speaker Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2C2C2E))
                            .clickable { /* Toggle Mute */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Exit Button
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFF3B30))
                            .clickable { /* Exit Nav */ }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Exit",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Map Snippet
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C1C1E)),
                contentAlignment = Alignment.Center
            ) {
                if (state.mapSnippet != null) {
                    Image(
                        bitmap = state.mapSnippet.asImageBitmap(),
                        contentDescription = "Map",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedTimer(state: IslandState.Timer) {
    ExpandedCard {
        ExpandedTimerView(state)
    }
}

@Composable
private fun ExpandedWeather(state: IslandState.Weather) {
    ExpandedCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (state.cityName.isNotBlank()) {
                    // cityName (not location) – WeatherInfo data class ke mutabik
                    Text(state.cityName, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Normal)
                }
                Text("${state.temperature}°", color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(
                    state.condition.replaceFirstChar { it.uppercase() },
                    color = TextSecondary, fontSize = 14.sp
                )
            }
            Icon(
                imageVector = getWeatherIconFromCode(state.iconCode),
                contentDescription = state.condition,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFFFFD60A)
            )
        }
    }
}

// ── Shared card shell ──────────────────────────────────────────────────────────

@Composable
private fun ExpandedCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation    = 24.dp,
                shape        = RoundedCornerShape(38.dp),
                ambientColor = Color.Black.copy(alpha = 0.6f),
                spotColor    = Color.Black.copy(alpha = 0.6f)
            )
            .clip(RoundedCornerShape(38.dp))
            .background(ExpandedSurface)
            .padding(16.dp),
        content = content
    )
}

// ── Icon helpers ──────────────────────────────────────────────────────────────

/** Map OpenWeatherMap icon codes → safe Material Icons (extended dependency present) */
private fun getDirectionIcon(direction: IslandState.Navigation.Direction): ImageVector = when (direction) {
    IslandState.Navigation.Direction.LEFT         -> Icons.AutoMirrored.Filled.ArrowBack
    IslandState.Navigation.Direction.RIGHT        -> Icons.AutoMirrored.Filled.ArrowForward
    IslandState.Navigation.Direction.STRAIGHT     -> Icons.Default.ArrowUpward
    IslandState.Navigation.Direction.SLIGHT_LEFT  -> Icons.AutoMirrored.Filled.Reply // Placeholder
    IslandState.Navigation.Direction.SLIGHT_RIGHT -> Icons.AutoMirrored.Filled.Reply // Placeholder
    IslandState.Navigation.Direction.U_TURN       -> Icons.AutoMirrored.Filled.Reply
    IslandState.Navigation.Direction.MERGE        -> Icons.Default.Merge
    IslandState.Navigation.Direction.EXIT         -> Icons.Default.ExitToApp
    IslandState.Navigation.Direction.ARRIVE       -> Icons.Default.CheckCircle
    else                                          -> Icons.Default.Navigation
}

/** Map OpenWeatherMap icon codes → safe Material Icons (extended dependency present) */
private fun getWeatherIconFromCode(iconCode: String): ImageVector = when (iconCode) {
    "01d", "01n"                   -> Icons.Default.WbSunny
    "02d", "02n",
    "03d", "03n",
    "04d", "04n"                   -> Icons.Default.Cloud
    "09d", "09n",
    "10d", "10n"                   -> Icons.Outlined.WaterDrop   // extended – safe
    "11d", "11n"                   -> Icons.Outlined.Bolt        // extended – replaces FlashOn
    "13d", "13n"                   -> Icons.Default.AcUnit
    else                           -> Icons.Default.WbSunny
}

private fun formatDuration(ms: Long): String {
    val safeMs       = ms.coerceAtLeast(0L)
    val totalSeconds = safeMs / 1000L
    val minutes      = totalSeconds / 60L
    val seconds      = totalSeconds % 60L
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}