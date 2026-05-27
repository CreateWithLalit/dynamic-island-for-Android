package com.miui.dynamicisland.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.miui.dynamicisland.DynamicIslandApplication
import com.miui.dynamicisland.R
import com.miui.dynamicisland.ui.bluetooth.DynamicBluetoothIslandOverlay
import com.miui.dynamicisland.util.IslandLogger

class BluetoothIslandService : LifecycleService(), ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "BluetoothIslandService"
        private const val NOTIFICATION_ID = 1002
        const val ACTION_START = "com.miui.dynamicisland.bluetooth.START"
        const val ACTION_STOP = "com.miui.dynamicisland.bluetooth.STOP"
        const val BLUETOOTH_DEVICE_ACTION_BATTERY_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        const val BLUETOOTH_BATTERY_EXTRA = "android.bluetooth.device.extra.BATTERY_LEVEL"
    }

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var overlayParams: WindowManager.LayoutParams? = null

    private var batteryLevel by mutableIntStateOf(-100)
    private var deviceName by mutableStateOf<String?>(null)
    private var isExpanded by mutableStateOf(false)

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                val safeIntent = intent ?: return
                if (safeIntent.action != BLUETOOTH_DEVICE_ACTION_BATTERY_CHANGED) return

                if (!hasBluetoothPermission()) {
                    batteryLevel = -100
                    deviceName = null
                    return
                }

                val level = safeIntent.getIntExtra(BLUETOOTH_BATTERY_EXTRA, -100)
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    safeIntent.getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE, android.bluetooth.BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    safeIntent.getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE)
                }

                if (level != -100) {
                    batteryLevel = level
                }
                deviceName = device?.name
            } catch (e: SecurityException) {
                IslandLogger.e(TAG, "Bluetooth receiver security error", e)
            } catch (e: Exception) {
                IslandLogger.e(TAG, "Bluetooth receiver error", e)
            }
        }
    }

    private val serviceViewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = serviceViewModelStore

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIFICATION_ID, createNotification())
        registerBluetoothReceiver()
        attachOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val superResult = super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                try {
                    startForeground(NOTIFICATION_ID, createNotification())
                } catch (_: Exception) {
                    // no-op
                }
                attachOverlay()
            }
        }
        return superResult
    }

    private fun registerBluetoothReceiver() {
        try {
            val filter = IntentFilter(BLUETOOTH_DEVICE_ACTION_BATTERY_CHANGED)
            ContextCompat.registerReceiver(
                this,
                bluetoothReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Bluetooth receiver registration failed", e)
        }
    }

    private fun attachOverlay() {
        if (composeView != null) return

        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(this@BluetoothIslandService)
            setViewTreeSavedStateRegistryOwner(this@BluetoothIslandService)
            setViewTreeViewModelStoreOwner(this@BluetoothIslandService)
            setContent {
                DynamicBluetoothIslandOverlay(
                    batteryLevel = batteryLevel,
                    deviceName = deviceName,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        isExpanded = !isExpanded
                        updateWindowLayout(isExpanded)
                    }
                )
            }
        }

        val params = baseParams()
        try {
            windowManager.addView(view, params)
            composeView = view
            overlayParams = params
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Overlay init error: ${e.message ?: "unknown"}", e)
        }
    }

    private fun updateWindowLayout(expanded: Boolean) {
        val params = overlayParams ?: return
        val view = composeView ?: return

        if (expanded) {
            params.width = dpToPx(320)
            params.height = dpToPx(85)
        } else {
            params.width = dpToPx(160)
            params.height = dpToPx(40)
        }

        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Overlay update error: ${e.message ?: "unknown"}", e)
        }
    }

    private fun baseParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dpToPx(20)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, DynamicIslandApplication.CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("Bluetooth Island")
        .setContentText("Bluetooth battery overlay is running")
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (_: Exception) {
            // no-op
        }
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
                // no-op
            }
        }
        composeView = null
        overlayParams = null
        serviceViewModelStore.clear()
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)
}
