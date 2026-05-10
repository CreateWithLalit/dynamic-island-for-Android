package com.miui.dynamicisland.data.model

data class WeatherInfo(
    val temperature: Int,
    val condition: String,
    val iconCode: String,
    val cityName: String,
    val lastUpdated: Long
)