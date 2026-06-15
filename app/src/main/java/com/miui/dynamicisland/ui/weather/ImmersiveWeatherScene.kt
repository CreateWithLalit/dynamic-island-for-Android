package com.miui.dynamicisland.ui.weather

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.sin

private val SAFE_HORIZONTAL_PADDING = 50.dp
private val SAFE_VERTICAL_PADDING = 30.dp

@Composable
fun ImmersiveWeatherScene(
    weatherData: WeatherSceneData,
    currentTime: LocalTime,
    modifier: Modifier = Modifier
) {
    val timeOfDay = TimeOfDay.fromHour(currentTime.hour)
    val isNight = timeOfDay == TimeOfDay.Night
    val density = LocalDensity.current
    
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        
        val horizontalPaddingPx = with(density) { SAFE_HORIZONTAL_PADDING.toPx() }
        val verticalPaddingPx = with(density) { SAFE_VERTICAL_PADDING.toPx() }
        
        // Layer 0: Sky (Always fills 100%)
        AnimatedSkyBackground(
            currentTime = currentTime,
            weatherCondition = weatherData.condition,
            modifier = Modifier.fillMaxSize()
        )
        
        // Everything else drawn inside safe content area to avoid edge clipping
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SAFE_HORIZONTAL_PADDING, vertical = SAFE_VERTICAL_PADDING)
        ) {
            // Stars (night only)
            if (isNight) {
                StarryNight(modifier = Modifier.fillMaxSize())
            }
            
            // Layer 1: Celestial Body
            val sunMoonPosition = calculateCelestialPosition(
                currentTime = currentTime,
                sunrise = weatherData.sunrise,
                sunset = weatherData.sunset,
                containerSize = Size(
                    widthPx - 2 * horizontalPaddingPx,
                    heightPx - 2 * verticalPaddingPx
                )
            )
            
            if (isNight) {
                AnimatedMoon(
                    position = sunMoonPosition,
                    phase = 0.5f,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AnimatedSun(
                    sunPosition = sunMoonPosition,
                    intensity = if (timeOfDay == TimeOfDay.Noon) 1f else 0.7f,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Layer 2-5: Clouds
            if (weatherData.condition != WeatherCondition.Clear) {
                AnimatedClouds(
                    weatherCondition = weatherData.condition,
                    timeOfDay = timeOfDay,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Layer 4: Weather Effects
            when (weatherData.condition) {
                WeatherCondition.Rain -> RainEffect(
                    intensity = weatherData.precipitationIntensity,
                    modifier = Modifier.fillMaxSize()
                )
                WeatherCondition.Storm -> {
                    RainEffect(intensity = 1f, modifier = Modifier.fillMaxSize())
                    LightningEffect(active = true, modifier = Modifier.fillMaxSize())
                }
                WeatherCondition.Snow -> SnowEffect(
                    intensity = weatherData.precipitationIntensity,
                    modifier = Modifier.fillMaxSize()
                )
                else -> {}
            }
        }
        
        // Layer 6: Atmospheric Overlay
        AtmosphericOverlay(timeOfDay = timeOfDay)
    }
}

@Composable
fun AtmosphericOverlay(timeOfDay: TimeOfDay) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Haze/Vignette effect
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Soft bottom haze
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.1f)
                    ),
                    startY = size.height * 0.6f,
                    endY = size.height
                )
            )
            
            // Corner Vignette
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.25f)
                    ),
                    center = center,
                    radius = size.maxDimension * 0.8f
                )
            )
        }
        
        // Color grading based on time
        val tintColor = when(timeOfDay) {
            TimeOfDay.Dawn -> Color(0xFFFFA07A).copy(alpha = 0.15f)
            TimeOfDay.GoldenHour -> Color(0xFFFFD700).copy(alpha = 0.1f)
            TimeOfDay.Dusk -> Color(0xFF8B4513).copy(alpha = 0.2f)
            TimeOfDay.Night -> Color(0xFF191970).copy(alpha = 0.3f)
            else -> Color.Transparent
        }
        
        Box(modifier = Modifier
            .fillMaxSize()
            .background(tintColor)
        )
    }
}

// Calculate sun/moon position along the arc
fun calculateCelestialPosition(
    currentTime: LocalTime,
    sunrise: LocalTime,
    sunset: LocalTime,
    containerSize: Size
): Offset {
    val isDay = !currentTime.isBefore(sunrise) && currentTime.isBefore(sunset)
    
    val start = if (isDay) sunrise else sunset
    val end = if (isDay) sunset else sunrise.plusHours(24) // Simplified night end
    
    val totalMinutes = ChronoUnit.MINUTES.between(start, end).toFloat()
    val currentMinutes = ChronoUnit.MINUTES.between(start, currentTime).toFloat()
    val progress = (currentMinutes / totalMinutes).coerceIn(0f, 1f)
    
    // Arc path: centered and higher (above the time)
    val x = containerSize.width * (0.35f + 0.3f * progress)
    val y = containerSize.height * 0.08f * (1f - sin(progress * PI).toFloat()) + containerSize.height * 0.02f
    
    return Offset(x, y)
}
