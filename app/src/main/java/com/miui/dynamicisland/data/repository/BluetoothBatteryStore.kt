package com.miui.dynamicisland.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


data class BluetoothBatterySnapshot(
    val deviceName: String?,
    val batteryOverall: Int? = null,
    val batteryLeft: Int? = null,
    val batteryRight: Int? = null,
    val batteryCase: Int? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isFresh(maxAgeMs: Long = 30_000L): Boolean {
        return System.currentTimeMillis() - updatedAt <= maxAgeMs
    }

    fun matchesDeviceName(name: String?): Boolean {
        if (name.isNullOrBlank() || deviceName.isNullOrBlank()) return false
        val a = name.trim().lowercase()
        val b = deviceName.trim().lowercase()
        return a == b || a.contains(b) || b.contains(a)
    }

    fun overallOrNull(): Int? {
        return batteryOverall ?: listOf(batteryLeft, batteryRight, batteryCase)
            .filterNotNull()
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toInt()
    }
}

object BluetoothBatteryStore {
    private val _snapshot = MutableStateFlow<BluetoothBatterySnapshot?>(null)
    val snapshot: StateFlow<BluetoothBatterySnapshot?> = _snapshot.asStateFlow()

    fun update(snapshot: BluetoothBatterySnapshot) {
        _snapshot.value = snapshot
    }
}

