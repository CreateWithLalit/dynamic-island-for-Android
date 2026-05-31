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
import com.miui.dynamicisland.ui.states.IslandState
import java.util.Locale
import java.text.DateFormat
import java.util.Date
import androidx.compose.ui.platform.LocalContext
import com.miui.dynamicisland.data.repository.NotificationRepository
import android.content.Intent
import android.app.Notification
import android.app.RemoteInput
import android.os.Bundle
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.miui.dynamicisland.service.IslandNotificationListener
import com.miui.dynamicisland.manager.IslandStateManager

private val ExpandedSurface  = Color(0xFF1C1C1E)
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
                is IslandState.Notification -> ExpandedNotification(state)
                is IslandState.Charging     -> ExpandedCharging(state)
                is IslandState.Bluetooth    -> ExpandedBluetooth(state)
                is IslandState.Weather      -> ExpandedWeather(state)
                is IslandState.Idle,
                is IslandState.Silent,
                is IslandState.Volume,
                is IslandState.LockScreen   -> Unit
            }
        }
    }
}

// ── Individual expanded cards ─────────────────────────────────────────────────

@Composable
private fun ExpandedMedia(state: IslandState.Media) {
    ExpandedCard {
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
private fun ExpandedNotification(state: IslandState.Notification) {
    val timeText = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(state.postTime))
    val canNavigate = state.queueCount > 1
    val context = LocalContext.current
    val appLabel = state.appName.ifBlank { "App" }.uppercase(Locale.getDefault())
    val interactionSource = remember { MutableInteractionSource() }
    val replyAction: android.app.Notification.Action? = remember(state.actions) {
        state.actions?.firstOrNull { it.remoteInputs?.isNotEmpty() == true }
    }
    var replyText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Reply to ${state.title}...", color = Color.Gray, fontSize = 14.sp) },
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedContainerColor = Color(0xFF2C2C2E),
                            unfocusedContainerColor = Color(0xFF1C1C1E)
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send,
                            keyboardType = KeyboardType.Text
                        ),
                        keyboardActions = KeyboardActions(onSend = {
                            if (replyText.isNotBlank()) {
                                executeDirectReply(context, replyAction, replyText)
                                IslandStateManager.getInstance().pushState(state.copy(isReplying = false))
                                replyText = ""
                                focusManager.clearFocus()
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                executeDirectReply(context, replyAction, replyText)
                                IslandStateManager.getInstance().pushState(state.copy(isReplying = false))
                                replyText = ""
                                focusManager.clearFocus()
                            } else {
                                IslandStateManager.getInstance().pushState(state.copy(isReplying = false))
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (replyText.isBlank()) Icons.Default.Close else Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (replyText.isBlank()) Color.Gray else AccentGreen
                        )
                    }
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
                                        IslandStateManager.getInstance().pushState(state.copy(isReplying = true))
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
        IslandState.Charging.ChargeMethod.WIRELESS -> "Wireless"
        IslandState.Charging.ChargeMethod.WIRED -> "Wired"
        IslandState.Charging.ChargeMethod.NONE -> "Battery"
        IslandState.Charging.ChargeMethod.UNKNOWN -> "Battery"
    }

    ExpandedCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Battery", fontSize = 18.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AccentOrange.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(methodLabel, color = AccentOrange, fontSize = 12.sp, fontWeight = FontWeight.Normal)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (state.isCharging) "Charging" else "Unplugged",
                    color = if (state.isCharging) AccentGreen else TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(Modifier.height(2.dp))
                Text("${state.batteryLevel.coerceIn(0, 100)}%", color = TextSecondary, fontSize = 12.sp)
                if (state.estimatedTimeMinutes > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text("Full in ${state.estimatedTimeMinutes} min", color = TextSecondary, fontSize = 12.sp)
                }
            }
            IosBatteryIcon(
                level = state.batteryLevel.coerceIn(0, 100),
                isCharging = state.isCharging,
                modifier = Modifier.size(width = 48.dp, height = 24.dp)
            )
        }
    }
}

@Composable
private fun ExpandedBluetooth(state: IslandState.Bluetooth) {
    val deviceName = state.deviceName.ifBlank { "Bluetooth" }
    val isEarbuds = deviceName.lowercase(Locale.US).let { name ->
        name.contains("airpod") || name.contains("earbud") || name.contains("buds") || name.contains("pods")
    }
    val statusColor = if (state.isConnected) AccentGreen else TextSecondary

    ExpandedCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (isEarbuds) {
                AirPodsIcon(modifier = Modifier.size(24.dp), tint = TextPrimary)
            } else {
                Icon(
                    if (state.isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                    "Bluetooth",
                    Modifier.size(24.dp),
                    TextPrimary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    deviceName,
                    color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (state.isConnected) "Connected" else "Disconnected",
                        color = statusColor,
                        fontSize = 12.sp
                    )
                }
            }
            state.batteryLevel?.let { raw ->
                val level = raw.coerceIn(0, 100)
                val batteryColor = if (level > 20) AccentGreen else Color(0xFFFF3B30)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "$level%",
                        color = batteryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                    IosBatteryIcon(
                        level = level,
                        isCharging = false,
                        modifier = Modifier.size(width = 24.dp, height = 12.dp),
                        color = batteryColor
                    )
                }
            }
        }
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
                shape        = RoundedCornerShape(26.dp),
                ambientColor = Color.Black.copy(alpha = 0.6f),
                spotColor    = Color.Black.copy(alpha = 0.6f)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(ExpandedSurface)
            .padding(16.dp),
        content = content
    )
}

// ── Icon helpers ──────────────────────────────────────────────────────────────

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