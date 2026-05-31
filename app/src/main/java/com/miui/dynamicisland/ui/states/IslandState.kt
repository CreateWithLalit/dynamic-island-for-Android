// File: app/src/main/java/com/miui/dynamicisland/ui/states/IslandState.kt
package com.miui.dynamicisland.ui.states

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.app.Notification
import android.app.PendingIntent

sealed class IslandState(
    val priority: Int,
    val durationMs: Long,
    val allowInteraction: Boolean = false,
    open val isExpanded: Boolean = false
) {

    object Idle : IslandState(0, Long.MAX_VALUE, false, false)

    data class Charging(
        val batteryLevel: Int,
        val isCharging: Boolean = true,
        val chargeMethod: ChargeMethod = ChargeMethod.WIRED,
        val estimatedTimeMinutes: Int = -1,
        override val isExpanded: Boolean = false
    ) : IslandState(30, 3000L, true, isExpanded) {
        enum class ChargeMethod { WIRED, WIRELESS, NONE, UNKNOWN }
    }

    data class Media(
        val title: String,
        val artist: String,
        val isPlaying: Boolean = true,
        val packageName: String = "",
        val albumArt: Bitmap? = null,
        val albumArtUri: String? = null,
        val duration: Long = 0L,
        val position: Long = 0L,
        val showFullPlayer: Boolean = false,
        override val isExpanded: Boolean = false
    ) : IslandState(10, Long.MAX_VALUE, true, isExpanded) {
        val formattedPosition: String get() = formatTime(position)
        val formattedRemainingTime: String
            get() = if (isPlaying && duration > 0L) "-${formatTime((duration - position).coerceAtLeast(0L))}" else formatTime(duration)
        val formattedDuration: String get() = formatTime(duration)

        private fun formatTime(ms: Long): String {
            if (ms <= 0L) return "0:00"
            val totalSeconds = (ms / 1000.0).toInt()
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    data class Notification(
        val appName: String,
        val title: String,
        val content: String,
        val packageName: String,
        val appIcon: Drawable? = null,
        val postTime: Long = System.currentTimeMillis(),
        val queueCount: Int = 1,
        val queueIndex: Int = 0,
        val contentIntent: PendingIntent? = null,
        val actions: Array<android.app.Notification.Action>? = null,
        val notificationKey: String = "",
        val isMessage: Boolean = false,
        val isReplying: Boolean = false,
        override val isExpanded: Boolean = false
    ) : IslandState(20, Long.MAX_VALUE, true, isExpanded)

    data class Bluetooth(
        val isConnected: Boolean = false,
        val deviceName: String = "",
        val batteryLevel: Int? = null,
        override val isExpanded: Boolean = false
    ) : IslandState(35, 3000L, false, isExpanded)

    data class Silent(
        val isSilent: Boolean = true,
        val ringerMode: Int = AudioManager.RINGER_MODE_SILENT,
        override val isExpanded: Boolean = false
    ) : IslandState(15, 2000L, false, isExpanded)

    data class Volume(
        val volumeLevel: Int = 0,
        val maxVolume: Int = 100,
        val volumeType: VolumeType = VolumeType.MEDIA,
        override val isExpanded: Boolean = false
    ) : IslandState(15, 2000L, false, isExpanded) {
        enum class VolumeType { MEDIA, RING, ALARM, CALL }
    }

    data class Call(
        val callerName: String = "Unknown",
        val callerSubtext: String = "iPhone",
        val callerPhoto: Bitmap? = null,
        val isIncoming: Boolean = false,
        val isOngoing: Boolean = true,
        val isSpeakerOn: Boolean = false,
        val isMuted: Boolean = false,
        val duration: Long = 0L,
        override val isExpanded: Boolean = false
    ) : IslandState(40, Long.MAX_VALUE, true, isExpanded)

    data class Weather(
        val temperature: Int,
        val condition: String,
        val iconCode: String,
        val cityName: String,
        val sunrise: Long = 0,
        val sunset: Long = 0,
        val windSpeed: Double = 0.0,
        val humidity: Int = 0,
        val visibility: Int = 0,
        val hourlyForecast: List<com.miui.dynamicisland.data.model.HourlyWeather> = emptyList(),
        val dailyForecast: List<com.miui.dynamicisland.data.model.DailyWeather> = emptyList(),
        override val isExpanded: Boolean = false
    ) : IslandState(5, Long.MAX_VALUE, true, isExpanded)

    data class LockScreen(
        val notificationCount: Int,
        override val isExpanded: Boolean = false
    ) : IslandState(100, Long.MAX_VALUE, false, isExpanded)
} // ✅ Added missing closing brace

// Extension function (now outside sealed class)
fun IslandState.withExpanded(expanded: Boolean): IslandState {
    return when (this) {
        is IslandState.Media -> copy(isExpanded = expanded)
        is IslandState.Notification -> copy(isExpanded = expanded)
        is IslandState.Call -> copy(isExpanded = expanded)
        is IslandState.Charging -> copy(isExpanded = expanded)
        is IslandState.Bluetooth -> copy(isExpanded = expanded)
        is IslandState.Silent -> copy(isExpanded = expanded)
        is IslandState.Volume -> copy(isExpanded = expanded)
        is IslandState.Weather -> copy(isExpanded = expanded)
        is IslandState.LockScreen -> copy(isExpanded = expanded)
        else -> this
    }
}