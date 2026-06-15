package com.miui.dynamicisland.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.miui.dynamicisland.manager.IslandStateManager
import com.miui.dynamicisland.ui.states.IslandState
import com.miui.dynamicisland.util.IslandLogger

class BatteryReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BatteryReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val stateManager = IslandStateManager.getInstance()
        IslandLogger.d(TAG, "Action received: $action", null)

        when (action) {
            "android.intent.action.POWER_CONNECTED",
            "android.intent.action.BATTERY_CHANGED" -> {
                val isCharging = isCharging(intent)
                if (isCharging) {
                    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val currentMicroAmps = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                    val voltageMilliVolts = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                    
                    // Rough wattage calculation or default to 33W as requested by user
                    val calculatedWattage = if (voltageMilliVolts > 0 && currentMicroAmps > 0) {
                        ((voltageMilliVolts.toLong() * currentMicroAmps) / 1_000_000_000L).toInt()
                    } else 0
                    
                    val finalWattage = if (calculatedWattage > 10) calculatedWattage else 33

                    stateManager.pushState(
                        IslandState.Charging(
                            batteryLevel = getBatteryLevel(intent),
                            isCharging = true,
                            chargeMethod = getChargeMethod(intent),
                            wattage = finalWattage
                        )
                    )
                } else if (action == "android.intent.action.BATTERY_CHANGED") {
                    stateManager.removeState(IslandState.Charging::class.java)
                }
            }
            "android.intent.action.POWER_DISCONNECTED" -> {
                stateManager.removeState(IslandState.Charging::class.java)
            }
        }
    }

    private fun isCharging(intent: Intent): Boolean {
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun getBatteryLevel(intent: Intent): Int {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else 0
    }

    private fun getChargeMethod(intent: Intent): IslandState.Charging.ChargeMethod {
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        return when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC, BatteryManager.BATTERY_PLUGGED_USB -> IslandState.Charging.ChargeMethod.WIRED
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> IslandState.Charging.ChargeMethod.WIRELESS
            else -> IslandState.Charging.ChargeMethod.NONE
        }
    }
}