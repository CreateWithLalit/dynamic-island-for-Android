package com.miui.dynamicisland.data.repository

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.miui.dynamicisland.BuildConfig
import java.util.*
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
        private const val DEFAULT_CITY = "Noida"
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
                    lastUpdated = System.currentTimeMillis(),
                    sunrise = System.currentTimeMillis() / 1000 - 3600 * 5,
                    sunset = System.currentTimeMillis() / 1000 + 3600 * 7,
                    windSpeed = 15.0,
                    humidity = 65,
                    visibility = 10000,
                    hourlyForecast = List(8) { i ->
                        com.miui.dynamicisland.data.model.HourlyWeather(
                            time = System.currentTimeMillis() + i * 3600 * 1000,
                            temperature = 25 - i,
                            iconCode = "01d",
                            condition = "Clear"
                        )
                    },
                    dailyForecast = List(5) { i ->
                        com.miui.dynamicisland.data.model.DailyWeather(
                            time = System.currentTimeMillis() + i * 86400 * 1000,
                            minTemp = 18 + i,
                            maxTemp = 30 + i,
                            iconCode = if (i % 2 == 0) "01d" else "02d",
                            condition = "Clear"
                        )
                    }
                )
                return
            }

            val location = getLastKnownNetworkLocation()
            val weatherResponse = if (location != null) {
                api.getWeatherByCoords(location.latitude, location.longitude, apiKey)
            } else {
                api.getWeather(DEFAULT_CITY, apiKey)
            }

            val forecastResponse = if (location != null) {
                api.getForecastByCoords(location.latitude, location.longitude, apiKey)
            } else {
                api.getForecast(DEFAULT_CITY, apiKey)
            }

            val weatherInfo = mapResponseToWeatherInfo(weatherResponse, forecastResponse)
            _cachedWeather.value = weatherInfo

            IslandLogger.d(TAG, "Weather refreshed: ${weatherInfo.temperature}°C in ${weatherInfo.cityName}", null)
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Failed to refresh weather: ${e.message}", e)
        }
    }

    private fun getLastKnownNetworkLocation(): Location? {
        val hasCoarsePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasCoarsePermission) return null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return runCatching {
            manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        }.getOrNull()
    }

    private fun mapResponseToWeatherInfo(
        weatherResponse: com.miui.dynamicisland.data.api.WeatherResponse,
        forecastResponse: com.miui.dynamicisland.data.api.ForecastResponse
    ): WeatherInfo {
        val weather = weatherResponse.weather.firstOrNull()
        return WeatherInfo(
            temperature = weatherResponse.main.temp.toInt(),
            condition = weather?.description?.replaceFirstChar { it.uppercase() } ?: "Unknown",
            iconCode = weather?.icon ?: "01d",
            cityName = weatherResponse.name,
            lastUpdated = System.currentTimeMillis(),
            sunrise = weatherResponse.sys.sunrise,
            sunset = weatherResponse.sys.sunset,
            windSpeed = weatherResponse.wind.speed,
            humidity = weatherResponse.main.humidity,
            visibility = weatherResponse.visibility,
            hourlyForecast = forecastResponse.list.take(8).map { item ->
                com.miui.dynamicisland.data.model.HourlyWeather(
                    time = item.dt * 1000,
                    temperature = item.main.temp.toInt(),
                    iconCode = item.weather.firstOrNull()?.icon ?: "01d",
                    condition = item.weather.firstOrNull()?.description ?: "Unknown"
                )
            },
            dailyForecast = forecastResponse.list
                .groupBy { 
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.dt * 1000 }
                    cal.get(java.util.Calendar.DAY_OF_YEAR)
                }
                .values
                .take(5)
                .map { dayItems ->
                    val first = dayItems.first()
                    com.miui.dynamicisland.data.model.DailyWeather(
                        time = first.dt * 1000,
                        minTemp = dayItems.minOf { it.main.temp_min }.toInt(),
                        maxTemp = dayItems.maxOf { it.main.temp_max }.toInt(),
                        iconCode = dayItems[dayItems.size / 2].weather.firstOrNull()?.icon ?: "01d",
                        condition = dayItems[dayItems.size / 2].weather.firstOrNull()?.description ?: "Unknown"
                    )
                }
        )
    }
}
