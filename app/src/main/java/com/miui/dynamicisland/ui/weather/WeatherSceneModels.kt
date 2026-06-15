package com.miui.dynamicisland.ui.weather

import androidx.compose.ui.graphics.Color
import java.time.LocalTime

enum class WeatherCondition {
    Clear, Cloudy, Rain, Storm, Snow, Fog
}

sealed class TimeOfDay(val startHour: Int, val endHour: Int) {
    object Dawn : TimeOfDay(5, 7)
    object Morning : TimeOfDay(7, 11)
    object Noon : TimeOfDay(11, 16)
    object GoldenHour : TimeOfDay(16, 19)
    object Dusk : TimeOfDay(19, 21)
    object Night : TimeOfDay(21, 5) // wraps around
    
    companion object {
        fun fromHour(hour: Int): TimeOfDay = when(hour) {
            in 5..6 -> Dawn
            in 7..10 -> Morning
            in 11..15 -> Noon
            in 16..18 -> GoldenHour
            in 19..20 -> Dusk
            else -> Night
        }
    }
}

val skyGradients = mapOf(
    TimeOfDay.Dawn to listOf(Color(0xFF2E1A47), Color(0xFF8B4A6B), Color(0xFFFFB347)),
    TimeOfDay.Morning to listOf(Color(0xFF87CEEB), Color(0xFFB0E0E6), Color(0xFFF0F8FF)),
    TimeOfDay.Noon to listOf(Color(0xFF1E90FF), Color(0xFF87CEEB), Color(0xFFB0E0E6)),
    TimeOfDay.GoldenHour to listOf(Color(0xFF4A6741), Color(0xFFFF8C00), Color(0xFFFFD700)),
    TimeOfDay.Dusk to listOf(Color(0xFF2C1810), Color(0xFF8B4513), Color(0xFFFF6347)),
    TimeOfDay.Night to listOf(Color(0xFF0B1026), Color(0xFF1B1B3A), Color(0xFF2E2E5E))
)

data class WeatherSceneData(
    val condition: WeatherCondition,
    val sunrise: LocalTime,
    val sunset: LocalTime,
    val precipitationIntensity: Float = 0.5f,
    val currentTemp: Int = 24,
    val location: String = "Noida",
    val moonPhase: Float = 0.5f,
    val hourlyForecast: List<HourlyForecast> = emptyList()
)

data class HourlyForecast(
    val time: String,
    val temp: Int,
    val iconRes: Int
)
