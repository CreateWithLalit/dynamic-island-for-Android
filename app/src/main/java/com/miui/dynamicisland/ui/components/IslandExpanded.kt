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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
                is IslandState.Volume       -> Unit
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
                    color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (state.isOngoing) formatDuration(state.duration) else "Incoming call",
                    color = if (state.isOngoing) AccentGreen else TextSecondary,
                    fontSize = 13.sp
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

    ExpandedCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            IosAppIcon(
                packageName = state.packageName,
                appName = state.appName.ifBlank { "App" },
                size = 36.dp
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(state.appName, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(timeText, color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    state.title.ifBlank { "Notification" },
                    color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (state.content.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        state.content,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(ExpandedDivider)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("MARK AS READ", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFFF3B30))
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("DELETE", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF0A84FF))
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("REPLY", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
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
                    Text("Battery", fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AccentOrange.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(methodLabel, color = AccentOrange, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (state.isCharging) "Charging" else "Unplugged",
                    color = if (state.isCharging) AccentGreen else TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text("${state.batteryLevel.coerceIn(0, 100)}%", color = TextSecondary, fontSize = 13.sp)
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
                    color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
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
                        fontSize = 13.sp
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
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
                    Text(state.cityName, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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