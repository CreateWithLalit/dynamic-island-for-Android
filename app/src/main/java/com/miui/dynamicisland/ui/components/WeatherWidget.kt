// File: app/src/main/java/com/miui/dynamicisland/ui/components/WeatherWidget.kt
// Purpose: Weather compact (temp + icon) and expanded (full card) views.
// Hinglish: Compact mein temp aur icon dikhta hai; expanded mein full weather card.
//
// FIXES:
//  - WeatherSlot import → ui.island.WeatherSlot (yahan define hota hai DynamicIsland.kt mein)
//  - Icons.Default.FlashOn → Icons.Default.BoltSharp nahi hai; use Icons.Default.Bolt (extended)
//    Fallback: use WbCloudy which is safe in both base + extended

package com.miui.dynamicisland.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.ui.island.WeatherSlot   // ← correct package
import com.miui.dynamicisland.ui.states.IslandState

@Composable
fun WeatherWidget(
    state: IslandState.Weather,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    slot: WeatherSlot = WeatherSlot.LEFT
) {
    if (isExpanded) {
        WeatherExpanded(state, modifier)
    } else {
        WeatherCompact(state, slot, modifier)
    }
}

@Composable
private fun WeatherCompact(
    state: IslandState.Weather,
    slot: WeatherSlot,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        when (slot) {
            WeatherSlot.LEFT -> {
                // Temperature on the left
                Text(
                    text = "${state.temperature}°",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            WeatherSlot.RIGHT -> {
                // Condition icon on the right
                Icon(
                    imageVector = getWeatherIconFromCode(state.iconCode),
                    contentDescription = state.condition,
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun WeatherExpanded(
    state: IslandState.Weather,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            if (state.cityName.isNotBlank()) {
                Text(
                    text = state.cityName,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }
            Text(
                text = "${state.temperature}°C",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = state.condition.replaceFirstChar { it.uppercase() },
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
        Icon(
            imageVector = getWeatherIconFromCode(state.iconCode),
            contentDescription = null,
            tint = Color(0xFFFFD60A),
            modifier = Modifier.size(48.dp)
        )
    }
}

/** Maps OpenWeatherMap icon codes → safe Material Icons (uses extended where available). */
private fun getWeatherIconFromCode(iconCode: String): ImageVector = when (iconCode.trim()) {
    "01d", "01n"                   -> Icons.Default.WbSunny
    "02d", "02n",
    "03d", "03n",
    "04d", "04n"                   -> Icons.Default.Cloud
    "09d", "09n",
    "10d", "10n"                   -> Icons.Default.WbCloudy
    "11d", "11n"                   -> Icons.Outlined.Bolt
    "13d", "13n"                   -> Icons.Default.AcUnit
    else                             -> Icons.Default.WbCloudy
}