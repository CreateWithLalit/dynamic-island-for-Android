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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
    val progress = if (state.duration > 0L)
        (state.position.toFloat() / state.duration.toFloat()).coerceIn(0f, 1f)
    else 0f

    ExpandedCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ExpandedDivider),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MusicNote, "Music", Modifier.size(24.dp), TextSecondary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.title.ifBlank { "Unknown Track" },
                    color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    state.artist.ifBlank { "Unknown Artist" },
                    color = TextSecondary, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ExpandedDivider),
                contentAlignment = Alignment.Center
            ) {
                Text(if (state.isPlaying) "⏸" else "▶", color = TextPrimary, fontSize = 14.sp)
            }
        }
        if (state.duration > 0L) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress     = { progress },
                modifier     = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color        = TextPrimary,
                trackColor   = ExpandedDivider
            )
        }
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
        if (state.isIncoming && !state.isOngoing) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = ExpandedDivider, thickness = 0.5.dp)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFFFF3B30)),
                    contentAlignment = Alignment.Center
                ) { Text("✕", color = TextPrimary, fontSize = 20.sp) }
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(26.dp))
                        .background(AccentGreen),
                    contentAlignment = Alignment.Center
                ) { Text("✓", color = TextPrimary, fontSize = 20.sp) }
            }
        }
    }
}

@Composable
private fun ExpandedNotification(state: IslandState.Notification) {
    ExpandedCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(ExpandedDivider),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Notifications, "Notification", Modifier.size(20.dp), TextSecondary)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(state.appName, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("now", color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    state.title.ifBlank { "Notification" },
                    color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (state.content.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(state.content, color = TextSecondary, fontSize = 13.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ExpandedCharging(state: IslandState.Charging) {
    ExpandedCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Battery", fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (state.isCharging) "Charging" else "Unplugged",
                    color = if (state.isCharging) AccentGreen else TextSecondary,
                    fontSize = 14.sp, fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text("${state.batteryLevel.coerceIn(0, 100)}%", color = TextSecondary, fontSize = 13.sp)
            }
            Text(
                when (state.chargeMethod) {
                    IslandState.Charging.ChargeMethod.WIRELESS -> "Wireless"
                    IslandState.Charging.ChargeMethod.WIRED    -> "Wired"
                    else                                       -> "Battery"
                },
                color = AccentOrange, fontSize = 12.sp, fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress   = { state.batteryLevel.coerceIn(0, 100) / 100f },
            modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color      = if (state.batteryLevel > 20) AccentGreen else Color(0xFFFF3B30),
            trackColor = ExpandedDivider
        )
    }
}

@Composable
private fun ExpandedBluetooth(state: IslandState.Bluetooth) {
    ExpandedCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Bluetooth, "Bluetooth", Modifier.size(24.dp), TextPrimary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.deviceName.ifBlank { "Bluetooth" },
                    color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (state.isConnected) "Connected" else "Disconnected",
                    color = if (state.isConnected) AccentGreen else TextSecondary,
                    fontSize = 13.sp
                )
            }
            state.batteryLevel?.let { raw ->
                val level = raw.coerceIn(0, 100)
                Text(
                    "$level%",
                    color = if (level > 20) AccentGreen else Color(0xFFFF3B30),
                    fontSize = 13.sp, fontWeight = FontWeight.Medium
                )
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
                imageVector    = getWeatherIconFromCode(state.iconCode),
                contentDescription = state.condition,
                modifier       = Modifier.size(48.dp),
                tint           = Color(0xFFFFD60A)
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