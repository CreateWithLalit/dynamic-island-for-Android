// File: app/src/main/java/com/miui/dynamicisland/data/repository/BluetoothRepository.kt

package com.miui.dynamicisland.data.repository

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
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

    val connectedDevice: Flow<String?> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        // FIXED: Corrected signature
                        IslandLogger.d(TAG, "Bluetooth device connected: ${device?.name}", null)
                        trySend(device?.name ?: "Connected")
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        // FIXED: Corrected signature
                        IslandLogger.d(TAG, "Bluetooth device disconnected", null)
                        trySend(null)
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        ContextCompat.registerReceiver(
            context.applicationContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        trySend(getConnectedDeviceName())
        awaitClose { context.applicationContext.unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    private fun getConnectedDeviceName(): String? {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            // FIXED: Corrected signature
            IslandLogger.w(TAG, "Missing BLUETOOTH_CONNECT permission", null)
            return null
        }

        return try {
            val a2dp = bluetoothManager.getConnectedDevices(BluetoothProfile.A2DP)
            val headset = bluetoothManager.getConnectedDevices(BluetoothProfile.HEADSET)
            val firstDevice = (a2dp + headset).firstOrNull()
            firstDevice?.name ?: bluetoothAdapter.bondedDevices.firstOrNull()?.name
        } catch (e: SecurityException) {
            // FIXED: Corrected signature and passing Exception as 3rd param
            IslandLogger.e(TAG, "Security exception getting connected devices", e)
            null
        } catch (e: Exception) {
            // FIXED: Corrected signature and passing Exception as 3rd param
            IslandLogger.e(TAG, "Error getting connected device", e)
            null
        }
    }
}

private object BluetoothProfile {
    const val A2DP = 2
    const val HEADSET = 1
}