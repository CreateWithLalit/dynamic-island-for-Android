// File: app/src/main/java/com/miui/dynamicisland/data/model/BatteryInfo.kt
// Purpose: Battery level, charging status, method, and helper utilities.

package com.miui.dynamicisland.data.model

import android.os.BatteryManager
import androidx.annotation.Keep
import java.util.Locale

@Keep
data class BatteryInfo(
    val level: Int,
    val isCharging: Boolean,
    val chargeMethod: ChargeMethod,
    val temperature: Int = 0,
    val voltage: Int = 0,
    val health: Int = BatteryManager.BATTERY_HEALTH_GOOD,
    val plugged: Int = 0,
    val status: Int = BatteryManager.BATTERY_STATUS_DISCHARGING,
    val chargeCounter: Int = 0,
    val currentNow: Int = 0,
    val isPowerSaveEnabled: Boolean = false,
    val estimatedTimeToFullMs: Long = -1L
) {

    enum class ChargeMethod {
        WIRED,      // USB / AC charger
        WIRELESS,   // Wireless charging
        NONE        // Discharging
    }

    val isFull: Boolean
        get() = level >= 100 && (status == BatteryManager.BATTERY_STATUS_FULL || isCharging)

    val isLow: Boolean get() = level <= 15
    val isCritical: Boolean get() = level <= 5

    val formattedEstimatedTime: String
        get() {
            if (estimatedTimeToFullMs <= 0L) return "Unknown"
            val totalMinutes = (estimatedTimeToFullMs / 60_000L).toInt()
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }

    val formattedTemperature: String
        get() = if (temperature > 0) "${temperature}°C" else ""

    val formattedVoltage: String
        get() = if (voltage > 0) String.format(Locale.US, "%.1fV", voltage / 1000.0) else ""

    // Apple HIG ke hisaab se colors: low = red, medium = orange, good = green
    fun getLevelColor(): Int {
        return when {
            level <= 10 -> 0xFFFF3B30.toInt()   // Red
            level <= 19 -> 0xFFFF9F0A.toInt()   // Orange
            else -> 0xFF30D158.toInt()          // Green
        }
    }

    companion object {
        val EMPTY = BatteryInfo(
            level = 100,
            isCharging = false,
            chargeMethod = ChargeMethod.NONE
        )

        fun fromSystemValues(
            level: Int,
            isCharging: Boolean,
            plugged: Int,
            temperature: Int,
            voltage: Int,
            health: Int
        ): BatteryInfo {
            val chargeMethod = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargeMethod.WIRELESS
                BatteryManager.BATTERY_PLUGGED_AC, BatteryManager.BATTERY_PLUGGED_USB -> ChargeMethod.WIRED
                else -> ChargeMethod.NONE
            }
            val status = if (isCharging) BatteryManager.BATTERY_STATUS_CHARGING else BatteryManager.BATTERY_STATUS_DISCHARGING
            return BatteryInfo(
                level = level.coerceIn(0, 100),
                isCharging = isCharging,
                chargeMethod = chargeMethod,
                temperature = temperature,
                voltage = voltage,
                health = health,
                plugged = plugged,
                status = status
            )
        }
    }
}