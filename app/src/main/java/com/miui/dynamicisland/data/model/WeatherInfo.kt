package com.miui.dynamicisland.data.model

data class WeatherInfo(
    val temperature: Int,
    val condition: String,
    val iconCode: String,
    val cityName: String,
    val lastUpdated: Long,
    val sunrise: Long = 0,
    val sunset: Long = 0,
    val windSpeed: Double = 0.0,
    val humidity: Int = 0,
    val visibility: Int = 0,
    val hourlyForecast: List<HourlyWeather> = emptyList(),
    val dailyForecast: List<DailyWeather> = emptyList()
)

data class HourlyWeather(
    val time: Long,
    val temperature: Int,
    val iconCode: String,
    val condition: String
)

data class DailyWeather(
    val time: Long,
    val minTemp: Int,
    val maxTemp: Int,
    val iconCode: String,
    val condition: String
)