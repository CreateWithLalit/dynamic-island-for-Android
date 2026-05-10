package com.miui.dynamicisland.data.repository

import android.content.Context
import com.miui.dynamicisland.BuildConfig
import com.miui.dynamicisland.data.api.WeatherApi
import com.miui.dynamicisland.data.api.WeatherResponse
import com.miui.dynamicisland.data.model.WeatherInfo
import com.miui.dynamicisland.util.IslandLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRepository(private val context: Context) {
    companion object {
        private const val TAG = "WeatherRepository"
        private const val BASE_URL = "https://api.openweathermap.org/"
        private const val DEFAULT_CITY = "Mumbai"
    }

    private val _cachedWeather = MutableStateFlow<WeatherInfo?>(null)
    val cachedWeather: StateFlow<WeatherInfo?> = _cachedWeather

    private val api: WeatherApi by lazy { createApi() }

    private fun createApi(): WeatherApi {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }

    suspend fun refreshWeather() {
        try {
            val apiKey = com.miui.dynamicisland.BuildConfig.WEATHER_API_KEY
            if (apiKey.isBlank() || apiKey == "YOUR_API_KEY_HERE") {
                IslandLogger.w(TAG, "Weather API key not set in BuildConfig. Using fallback mock data.", null)
                _cachedWeather.value = WeatherInfo(
                    temperature = 25,
                    condition = "Clear",
                    iconCode = "01d",
                    cityName = DEFAULT_CITY,
                    lastUpdated = System.currentTimeMillis()
                )
                return
            }

            val response = api.getWeather(city = DEFAULT_CITY, apiKey = apiKey)
            val weatherInfo = mapResponseToWeatherInfo(response)
            _cachedWeather.value = weatherInfo

            IslandLogger.d(TAG, "Weather refreshed: ${weatherInfo.temperature}°C in ${weatherInfo.cityName}", null)
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Failed to refresh weather: ${e.message}", e)
        }
    }

    private fun mapResponseToWeatherInfo(response: WeatherResponse): WeatherInfo {
        val weather = response.weather.firstOrNull()
        return WeatherInfo(
            temperature = response.main.temp.toInt(),
            condition = weather?.description?.replaceFirstChar { it.uppercase() } ?: "Unknown",
            iconCode = weather?.icon ?: "01d",
            cityName = response.name,
            lastUpdated = System.currentTimeMillis()
        )
    }
}