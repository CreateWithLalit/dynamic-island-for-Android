package com.miui.dynamicisland.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.miui.dynamicisland.data.repository.WeatherRepository
import com.miui.dynamicisland.util.IslandLogger

class WeatherWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo = WeatherRepository(applicationContext)
            repo.refreshWeather()
            IslandLogger.d("WeatherWorker", "Weather updated successfully", null)
            Result.success()
        } catch (e: Exception) {
            IslandLogger.e("WeatherWorker", "Failed to update weather", e)
            Result.retry()
        }
    }
}