package com.miui.dynamicisland

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.work.*
import com.miui.dynamicisland.data.repository.CallRepository
import com.miui.dynamicisland.data.repository.WeatherRepository
import com.miui.dynamicisland.manager.IslandStateManager
import com.miui.dynamicisland.service.WeatherWorker
import com.miui.dynamicisland.ui.states.IslandState
import com.miui.dynamicisland.util.IslandLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class DynamicIslandApplication : Application() {

    val stateManager: IslandStateManager by lazy { IslandStateManager.getInstance() }
    val callRepository: CallRepository by lazy { CallRepository(this) }
    private val appScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        IslandLogger.initialize(this, enableFileLogging = true)
        createNotificationChannel()
        scheduleWeatherUpdates()

        appScope.launch {
            val repo = WeatherRepository(this@DynamicIslandApplication)
            repo.cachedWeather.collect { weather ->
                if (weather != null) {
                    stateManager.pushState(
                        IslandState.Weather(
                            temperature = weather.temperature,
                            condition = weather.condition,
                            iconCode = weather.iconCode,
                            cityName = weather.cityName,
                            sunrise = weather.sunrise,
                            sunset = weather.sunset,
                            windSpeed = weather.windSpeed,
                            humidity = weather.humidity,
                            visibility = weather.visibility,
                            hourlyForecast = weather.hourlyForecast,
                            dailyForecast = weather.dailyForecast
                        )
                    )
                }
            }
        }
    }

    private fun scheduleWeatherUpdates() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<WeatherWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WeatherUpdate",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Dynamic Island Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the overlay running"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "dynamic_island_service_channel"
    }
}