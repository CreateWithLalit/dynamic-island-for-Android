// File: app/src/main/java/com/miui/dynamicisland/data/repository/BluetoothRepository.kt

package com.miui.dynamicisland.data.repository

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.miui.dynamicisland.util.IslandLogger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

data class BluetoothInfo(
    val deviceName: String?,
    val batteryLevel: Int? = null,
    val isConnected: Boolean = false,
    val deviceAddress: String? = null
)

class BluetoothRepository(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothRepository"
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    val isBluetoothEnabled: Flow<Boolean> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    val enabled = state == BluetoothAdapter.STATE_ON
                    // FIXED: Corrected string quotes and signature
                    IslandLogger.d(TAG, "Bluetooth enabled: $enabled", null)
                    trySend(enabled)
                }
            }
        }
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        ContextCompat.registerReceiver(
            context.applicationContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        trySend(bluetoothAdapter?.isEnabled == true)
        awaitClose { context.applicationContext.unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    val connectedDevice: Flow<BluetoothInfo> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action ?: return
                when (action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED,
                    BluetoothDevice.ACTION_ACL_DISCONNECTED,
                    BluetoothAdapter.ACTION_STATE_CHANGED,
                    "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED",
                    "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED",
                    "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED",
                    "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED" -> {

                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }

                        val battery = intent.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL", -1)
                            .takeIf { it != -1 }
                            ?: runCatching {
                                val method = device?.javaClass?.getMethod("getBatteryLevel") ?: return@runCatching null
                                val level = method.invoke(device) as Int
                                if (level >= 0) level else null
                            }.getOrNull()

                        val isConnected = resolveConnectedState(action, intent)
                        val deviceName = safeDeviceName(device) ?: safeDeviceName(getConnectedDevice())

                        IslandLogger.d(TAG, "Bluetooth event: $action, connected: $isConnected, battery: $battery", null)
                        trySend(
                            BluetoothInfo(
                                deviceName = deviceName,
                                batteryLevel = battery,
                                isConnected = isConnected,
                                deviceAddress = device?.address
                            )
                        )
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
            addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
        }
        ContextCompat.registerReceiver(
            context.applicationContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val initialDevice = getConnectedDevice()
        trySend(
            BluetoothInfo(
                deviceName = safeDeviceName(initialDevice),
                batteryLevel = null,
                isConnected = initialDevice != null,
                deviceAddress = initialDevice?.address
            )
        )

        awaitClose { context.applicationContext.unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    private fun resolveConnectedState(action: String, intent: Intent): Boolean {
        return when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> true
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> false
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
                if (state == BluetoothAdapter.STATE_ON) getConnectedDevice() != null else false
            }
            "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED" -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.ERROR)
                state == BluetoothAdapter.STATE_CONNECTED
            }
            "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED",
            "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED" -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                state == BluetoothProfile.STATE_CONNECTED
            }
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED" -> {
                getConnectedDevice() != null
            }
            else -> getConnectedDevice() != null
        }
    }

    private fun getConnectedDevice(): BluetoothDevice? {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            IslandLogger.w(TAG, "Missing BLUETOOTH_CONNECT permission", null)
            return null
        }

        return try {
            val a2dp = bluetoothManager.getConnectedDevices(BluetoothProfileIds.A2DP)
            val headset = bluetoothManager.getConnectedDevices(BluetoothProfileIds.HEADSET)
            (a2dp + headset).firstOrNull()
        } catch (e: SecurityException) {
            IslandLogger.e(TAG, "Security exception getting connected devices", e)
            null
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Error getting connected device", e)
            null
        }
    }


    private fun safeDeviceName(device: BluetoothDevice?): String? {
        if (device == null) return null
        return try {
            device.name
        } catch (e: SecurityException) {
            IslandLogger.w(TAG, "Missing BLUETOOTH_CONNECT permission for device name", null)
            null
        }
    }

    private fun getBatteryViaReflection(device: BluetoothDevice?): Int? {
        if (device == null) return null
        return try {
            val method = device.javaClass.getMethod("getBatteryLevel")
            val level = method.invoke(device) as Int
            if (level >= 0) level else null
        } catch (_: Exception) {
            null
        }
    }

}

private object BluetoothProfileIds {
    const val A2DP = 2
    const val HEADSET = 1
}