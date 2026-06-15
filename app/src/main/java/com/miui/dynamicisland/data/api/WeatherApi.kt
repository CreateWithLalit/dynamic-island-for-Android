package com.miui.dynamicisland.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("data/2.5/weather")
    suspend fun getWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse

    @GET("data/2.5/weather")
    suspend fun getWeatherByCoords(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse

    @GET("data/2.5/forecast")
    suspend fun getForecastByCoords(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("cnt") count: Int = 40 
    ): ForecastResponse

    @GET("data/2.5/forecast")
    suspend fun getForecast(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("cnt") count: Int = 40
    ): ForecastResponse
}

data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>,
    val name: String,
    val sys: Sys,
    val wind: Wind,
    val visibility: Int,
    val rain: Rain? = null
) {
    data class Main(
        val temp: Double,
        val humidity: Int
    )
    data class Weather(val icon: String, val description: String)
    data class Sys(
        val sunrise: Long,
        val sunset: Long
    )
    data class Wind(val speed: Double)
    data class Rain(
        @com.google.gson.annotations.SerializedName("1h") val oneHour: Double? = 0.0
    )
}

data class ForecastResponse(
    val list: List<ForecastItem>
) {
    data class ForecastItem(
        val dt: Long,
        val main: Main,
        val weather: List<Weather>
    ) {
        data class Main(
            val temp: Double,
            val temp_min: Double,
            val temp_max: Double
        )
        data class Weather(val icon: String, val description: String)
    }
}
