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
}

data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>,
    val name: String
) {
    data class Main(val temp: Double)
    data class Weather(val icon: String, val description: String)
}