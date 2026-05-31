package com.miui.dynamicisland.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.ui.island.WeatherSlot
import com.miui.dynamicisland.ui.states.IslandState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

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
                Text(
                    text = "${state.temperature}°",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            WeatherSlot.RIGHT -> {
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
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(60000L) // Update every minute
        }
    }

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEEE, d MMM", Locale.getDefault())
    val currentTimeStr = timeFormat.format(Date(currentTimeMillis))
    val currentDateStr = dateFormat.format(Date(currentTimeMillis))

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Header: Date and Location
        Column(modifier = Modifier.fillMaxWidth().align(Alignment.Start)) {
            Text(
                text = currentDateStr.replaceFirstChar { it.uppercase() },
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = state.cityName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Arc + Time (Hero Section)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            SunMoonTrajectory(
                sunrise = state.sunrise,
                sunset = state.sunset,
                currentTime = currentTimeMillis / 1000
            )

            // Large Time centered below the arc summit
            Text(
                text = currentTimeStr,
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(top = 42.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Hourly Forecast (Scrollable)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(state.hourlyForecast) { item ->
                HourlyItem(item)
            }
        }
    }
}

@Composable
private fun SunMoonTrajectory(sunrise: Long, sunset: Long, currentTime: Long) {
    val sunriseTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(sunrise * 1000))
    val sunsetTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(sunset * 1000))

    val isDay = currentTime in sunrise..sunset

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val arcHeight = height * 0.85f

            // Draw the arc background (thick yellow-ish)
            drawArc(
                color = if (isDay) Color(0xFFFFD60A).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(0f, height - arcHeight),
                size = Size(width, arcHeight * 2),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Calculate progress
            val dayProgress = if (sunset > sunrise) {
                ((currentTime - sunrise).toFloat() / (sunset - sunrise).toFloat()).coerceIn(0f, 1f)
            } else 0.5f

            val nightProgress = if (currentTime > sunset) {
                ((currentTime - sunset).toFloat() / (24 * 3600 - (sunset - sunrise))).coerceIn(0f, 1f)
            } else if (currentTime < sunrise) {
                ((currentTime + (24 * 3600 - sunset)).toFloat() / (24 * 3600 - (sunset - sunrise))).coerceIn(0f, 1f)
            } else 0f

            val displayProgress = if (isDay) dayProgress else nightProgress

            val angle = 180f + displayProgress * 180f
            val rad = Math.toRadians(angle.toDouble())
            val centerX = width / 2
            val centerY = height
            val radiusX = width / 2
            val radiusY = arcHeight

            val iconX = (centerX + radiusX * Math.cos(rad)).toFloat()
            val iconY = (centerY + radiusY * Math.sin(rad)).toFloat()

            val iconColor = if (isDay) Color(0xFFFFD60A) else Color(0xFFBDC3C7)
            val glowColor = if (isDay) Color(0xFFFFD60A).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f)

            // Draw Glow
            drawIntoCanvas { canvas ->
                val paint = Paint().asFrameworkPaint().apply {
                    setShadowLayer(20.dp.toPx(), 0f, 0f, glowColor.toArgb())
                }
                canvas.nativeCanvas.drawCircle(iconX, iconY, 10.dp.toPx(), paint)
            }

            // Draw Sun/Moon circle
            drawCircle(
                color = iconColor,
                radius = 10.dp.toPx(),
                center = Offset(iconX, iconY)
            )

            if (!isDay) {
                drawCircle(
                    color = Color.Black,
                    radius = 8.dp.toPx(),
                    center = Offset(iconX - 4.dp.toPx(), iconY - 3.dp.toPx())
                )
            }
        }

        // Labels
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text("Sunrise", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            Text(sunriseTime, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        Column(modifier = Modifier.align(Alignment.BottomEnd), horizontalAlignment = Alignment.End) {
            Text("Sunset", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            Text(sunsetTime, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun HourlyItem(item: com.miui.dynamicisland.data.model.HourlyWeather) {
    val hour = SimpleDateFormat("h a", Locale.getDefault()).format(Date(item.time))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = hour, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        Icon(
            imageVector = getWeatherIconFromCode(item.iconCode),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
        Text(
            text = "${item.temperature}°",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DailyItem(item: com.miui.dynamicisland.data.model.DailyWeather) {
    val day = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(item.time))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(55.dp)
    ) {
        Text(text = day, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Icon(
            imageVector = getWeatherIconFromCode(item.iconCode),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "${item.maxTemp}°", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = "/", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 2.dp))
            Text(text = "${item.minTemp}°", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun WeatherStatItem(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
        Text(text = value, color = Color.White, fontSize = 12.sp)
    }
}

private fun getWeatherIconFromCode(iconCode: String): ImageVector = when (iconCode.trim()) {
    "01d"                          -> Icons.Default.WbSunny
    "01n"                          -> Icons.Default.NightsStay
    "02d", "02n",
    "03d", "03n",
    "04d", "04n"                   -> Icons.Default.Cloud
    "09d", "09n",
    "10d", "10n"                   -> Icons.Default.WbCloudy
    "11d", "11n"                   -> Icons.Outlined.Bolt
    "13d", "13n"                   -> Icons.Default.AcUnit
    else                             -> Icons.Default.WbCloudy
}
