package com.miui.dynamicisland.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miui.dynamicisland.ui.island.WeatherSlot
import com.miui.dynamicisland.ui.states.IslandState
import com.miui.dynamicisland.ui.weather.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
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
    Box(modifier = modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
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
                    modifier = Modifier.size(20.dp).padding(end = 8.dp)
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
            delay(60000L)
        }
    }

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEEE, d MMM", Locale.getDefault())
    val currentTimeStr = timeFormat.format(Date(currentTimeMillis))
    val currentDateStr = dateFormat.format(Date(currentTimeMillis))
    
    val textShadow = remember {
        Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(0f, 2f), blurRadius = 8f)
    }

    val zoneId = ZoneId.systemDefault()
    val currentLocalTime = Instant.ofEpochMilli(currentTimeMillis).atZone(zoneId).toLocalTime()
    val sunriseLocalTime = Instant.ofEpochSecond(state.sunrise).atZone(zoneId).toLocalTime()
    val sunsetLocalTime = Instant.ofEpochSecond(state.sunset).atZone(zoneId).toLocalTime()

    val sceneData = WeatherSceneData(
        condition = getWeatherConditionFromIconCode(state.iconCode),
        sunrise = sunriseLocalTime,
        sunset = sunsetLocalTime,
        precipitationIntensity = 0.5f
    )

    Box(modifier = modifier.fillMaxSize()) {
        ImmersiveWeatherScene(
            weatherData = sceneData,
            currentTime = currentLocalTime,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.fillMaxWidth().align(Alignment.Start)) {
                Text(
                    text = currentDateStr.replaceFirstChar { it.uppercase() },
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    style = TextStyle(shadow = textShadow)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(state.cityName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, style = TextStyle(shadow = textShadow))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(modifier = Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.TopCenter) {
                Text(
                    text = currentTimeStr,
                    color = Color.White,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Light,
                    style = TextStyle(shadow = textShadow),
                    modifier = Modifier.padding(top = 35.dp)
                )
                
                Text(
                    text = "${state.temperature}°",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    style = TextStyle(shadow = textShadow),
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 45.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Glassy Hourly Card
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(vertical = 12.dp)
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    val forecast = state.hourlyForecast.take(8)
                    items(forecast) { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HourlyItem(item, textShadow)
                            if (forecast.indexOf(item) < forecast.size - 1) {
                                Box(modifier = Modifier.padding(horizontal = 12.dp).width(0.5.dp).height(35.dp).background(Color.White.copy(alpha = 0.2f)))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getWeatherConditionFromIconCode(iconCode: String): WeatherCondition = when (iconCode.trim()) {
    "01d", "01n" -> WeatherCondition.Clear
    "02d", "02n", "03d", "03n", "04d", "04n" -> WeatherCondition.Cloudy
    "09d", "09n", "10d", "10n" -> WeatherCondition.Rain
    "11d", "11n" -> WeatherCondition.Storm
    "13d", "13n" -> WeatherCondition.Snow
    "50d", "50n" -> WeatherCondition.Fog
    else -> WeatherCondition.Cloudy
}

@Composable
private fun HourlyItem(item: com.miui.dynamicisland.data.model.HourlyWeather, textShadow: Shadow) {
    val hour = SimpleDateFormat("h a", Locale.getDefault()).format(Date(item.time))
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.width(42.dp)) {
        Text(hour.lowercase(), color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, style = TextStyle(shadow = textShadow))
        Icon(getWeatherIconFromCode(item.iconCode), null, tint = Color.White, modifier = Modifier.size(24.dp))
        Text("${item.temperature}°", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, style = TextStyle(shadow = textShadow))
    }
}

private fun getWeatherIconFromCode(iconCode: String): ImageVector = when (iconCode.trim()) {
    "01d" -> Icons.Default.WbSunny
    "01n" -> Icons.Default.NightsStay
    "02d", "02n", "03d", "03n", "04d", "04n" -> Icons.Default.Cloud
    "09d", "09n", "10d", "10n" -> Icons.Default.Grain // Grain looks more like rain/showers
    "11d", "11n" -> Icons.Default.Bolt
    "13d", "13n" -> Icons.Default.AcUnit
    else -> Icons.Default.WbCloudy
}
