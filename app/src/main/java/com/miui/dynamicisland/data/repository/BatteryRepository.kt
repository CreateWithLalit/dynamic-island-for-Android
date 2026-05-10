// File: app/src/main/java/com/miui/dynamicisland/data/repository/BatteryRepository.kt

package com.miui.dynamicisland.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.miui.dynamicisland.data.model.BatteryInfo
import com.miui.dynamicisland.util.IslandLogger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class BatteryRepository(context: Context) {

    private val appContext = context.applicationContext
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    companion object {
        private const val TAG = "BatteryRepository"
    }

    val batteryInfo: Flow<BatteryInfo> = callbackFlow {
        IslandLogger.d(TAG, "Subscribing to battery updates", null)

        fun sendCurrentBatteryState() {
            val batteryIntent = appContext.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val info = batteryIntent?.let { extractBatteryInfo(it) } ?: BatteryInfo.EMPTY
            trySend(info).isSuccess
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val info = extractBatteryInfo(intent)
                        IslandLogger.d(TAG, "Battery update: level=${info.level}, charging=${info.isCharging}", null)
                        trySend(info).isSuccess
                    }
                    Intent.ACTION_POWER_CONNECTED,
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        IslandLogger.d(TAG, "Power state changed: ${intent.action}", null)
                        sendCurrentBatteryState()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }

        try {
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            IslandLogger.d(TAG, "Battery receiver registered", null)
            sendCurrentBatteryState()
        } catch (e: Exception) {
            // FIXED: Passing TAG and message separately, passing exception as 3rd param
            IslandLogger.e(TAG, "Error registering battery receiver", e)
            trySend(BatteryInfo.EMPTY).isSuccess
        }

        awaitClose {
            IslandLogger.d(TAG, "Unsubscribing from battery updates", null)
            try {
                appContext.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // FIXED: Passing TAG and message separately
                IslandLogger.e(TAG, "Error unregistering battery receiver", e)
            }
        }
    }.distinctUntilChanged { old, new ->
        old.level == new.level &&
                old.isCharging == new.isCharging &&
                old.chargeMethod == new.chargeMethod &&
                old.isPowerSaveEnabled == new.isPowerSaveEnabled &&
                old.status == new.status &&
                old.plugged == new.plugged
    }

    val batteryLevel: Flow<Int> = batteryInfo.map { it.level }
    val isCharging: Flow<Boolean> = batteryInfo.map { it.isCharging }

    private fun extractBatteryInfo(intent: Intent): BatteryInfo {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val levelPercent = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale).toInt().coerceIn(0, 100)
        } else 0

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

        val chargeMethod = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> BatteryInfo.ChargeMethod.WIRELESS
            BatteryManager.BATTERY_PLUGGED_AC, BatteryManager.BATTERY_PLUGGED_USB -> BatteryInfo.ChargeMethod.WIRED
            else -> BatteryInfo.ChargeMethod.NONE
        }

        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val isPowerSaveEnabled = powerManager.isPowerSaveMode

        val batteryManager = appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val chargeCounter = try {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        } catch (e: Exception) { -1 }
        val currentNow = try {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        } catch (e: Exception) { Int.MIN_VALUE }

        return BatteryInfo(
            level = levelPercent,
            isCharging = isCharging,
            chargeMethod = chargeMethod,
            temperature = temperature,
            voltage = voltage,
            health = health,
            plugged = plugged,
            status = status,
            chargeCounter = chargeCounter,
            currentNow = currentNow,
            isPowerSaveEnabled = isPowerSaveEnabled
        )
    }
}