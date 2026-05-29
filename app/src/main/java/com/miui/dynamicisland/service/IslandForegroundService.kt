// File: app/src/main/java/com/miui/dynamicisland/service/IslandForegroundService.kt

package com.miui.dynamicisland.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.miui.dynamicisland.DynamicIslandApplication
import com.miui.dynamicisland.data.model.BatteryInfo
import com.miui.dynamicisland.data.repository.BatteryRepository
import com.miui.dynamicisland.data.repository.BluetoothRepository
import com.miui.dynamicisland.data.repository.MediaRepository
import com.miui.dynamicisland.data.repository.MediaRepositoryBridge
import com.miui.dynamicisland.data.repository.NotificationRepository
import com.miui.dynamicisland.data.repository.WeatherRepository
import com.miui.dynamicisland.manager.CalibrationManager
import com.miui.dynamicisland.manager.IslandCalibration
import com.miui.dynamicisland.manager.IslandStateManager
import com.miui.dynamicisland.receiver.AudioModeReceiver
import com.miui.dynamicisland.receiver.BatteryReceiver
import com.miui.dynamicisland.ui.island.CallAction
import com.miui.dynamicisland.ui.island.DynamicIsland
import com.miui.dynamicisland.ui.island.MediaAction
import com.miui.dynamicisland.ui.states.IslandState
import com.miui.dynamicisland.util.IslandLogger
import com.miui.dynamicisland.util.WindowUtils
import com.miui.dynamicisland.util.OverlaySettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class IslandForegroundService : LifecycleService(), ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "IslandForegroundService"
        const val ACTION_START = "com.miui.dynamicisland.START"
        const val ACTION_STOP  = "com.miui.dynamicisland.STOP"
    }

    private lateinit var windowManager: WindowManager
    private var islandView: ComposeView? = null
    private var islandParams: WindowManager.LayoutParams? = null

    private val stateManager = IslandStateManager.getInstance()
    private lateinit var calibrationManager: CalibrationManager
    private lateinit var batteryRepository: BatteryRepository
    private lateinit var bluetoothRepository: BluetoothRepository
    lateinit var mediaRepository: MediaRepository
        private set
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var batteryReceiver: BatteryReceiver
    private var audioUpdateReceiver: BroadcastReceiver? = null

    private var mediaSessionManager: MediaSessionManager? = null
    private val mediaSessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        val first = controllers?.firstOrNull()
        mediaRepository.updateFromController(first)
        IslandLogger.d(TAG, "Active media sessions changed: ${controllers?.size ?: 0}", null)
    }

    private val serviceViewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = serviceViewModelStore

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        super.onCreate()

        windowManager     = getSystemService(WINDOW_SERVICE) as WindowManager
        calibrationManager = CalibrationManager(this)
        batteryRepository  = BatteryRepository(this)
        bluetoothRepository = BluetoothRepository(this)
        mediaRepository    = MediaRepository(this)
        MediaRepositoryBridge.register(mediaRepository)
        weatherRepository  = WeatherRepository(this)
        batteryReceiver    = BatteryReceiver()

        startForeground(1001, createNotification())
        initializeOverlay()
        registerReceivers()
        attachMediaSessionListener()
        observeWeather()
        observeMedia()
        observeBattery()
        observeBluetooth()
        observeNotifications()
        observeCalls()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                IslandLogger.d(TAG, "Stop action received", null)
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                // Ensure foreground notification exists (service may be restarted by system)
                try {
                    startForeground(1001, createNotification())
                } catch (_: Exception) {
                    // no-op
                }
                // Ensure overlay is attached
                initializeOverlay()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun initializeOverlay() {
        if (OverlaySettings.isAccessibilityOverlayEnabled(this)) {
            removeAppOverlay()
            return
        }
        if (islandView != null) return

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@IslandForegroundService)
            setViewTreeViewModelStoreOwner(this@IslandForegroundService)
            setViewTreeSavedStateRegistryOwner(this@IslandForegroundService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)

            setContent {
                val currentState by stateManager.currentState.collectAsState()
                val calibration  by calibrationManager.calibration.collectAsState(
                    initial = IslandCalibration.default()
                )
                
                DynamicIsland(
                    state         = currentState,
                    calibration   = calibration,
                    onMediaAction = { handleMediaAction(it) },
                    onCallAction  = { handleCallAction(it) }
                )
            }
        }

        val params = getNonTouchableParams()

        try {
            windowManager.addView(view, params)
            islandView   = view
            islandParams = params
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Overlay init error: ${e.message ?: "unknown"}", e)
            return
        }

        // Calibration observer
        lifecycleScope.launch {
            calibrationManager.calibration.collectLatest { cal ->
                updateOverlayParams(cal = cal)
            }
        }

        // State/flags observer  
        lifecycleScope.launch {
            stateManager.currentState.collectLatest { state ->
                updateOverlayParams(state = state)
            }
        }
    }

    private fun updateOverlayParams(
        cal: IslandCalibration? = null,
        state: IslandState? = null
    ) {
        if (OverlaySettings.isAccessibilityOverlayEnabled(this)) {
            removeAppOverlay()
            return
        }
        val params = islandParams ?: return
        val view = islandView ?: return

        // 1. Position Update
        cal?.let {
            val density = resources.displayMetrics.density
            val baseSafeY = WindowUtils.getStatusBarHeight(this@IslandForegroundService)
            params.x = (it.offsetX * density).toInt()
            params.y = baseSafeY + (it.offsetY * density).toInt()
        }

        // 2. Flags Update (Touch handling)
        state?.let {
            val isReplying = (it as? IslandState.Notification)?.isReplying == true
            
            // If replying, we MUST remove FLAG_NOT_FOCUSABLE to allow keyboard input.
            // Otherwise, we keep it so gestures (like back) work normally.
            val baseFlags = if (isReplying) {
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            } else {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            }

            params.flags = baseFlags or
                           WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                           WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

            params.width = if (it.isExpanded) {
                WindowManager.LayoutParams.MATCH_PARENT
            } else {
                WindowManager.LayoutParams.WRAP_CONTENT
            }
            applyLockScreenFlags(params)
        }

        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Params update error: ${e.message ?: "unknown"}", e)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        
        // Full visibility toggle based on orientation
        islandView?.let { view ->
            val isPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
            view.visibility = if (isPortrait) android.view.View.VISIBLE else android.view.View.GONE
            
            // Log for debugging
            IslandLogger.d(TAG, "Orientation changed. Visible: $isPortrait", null)
        }
    }

    private fun baseParams(): WindowManager.LayoutParams {
        val statusBarHeight = WindowUtils.getStatusBarHeight(this)
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            0,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            // Remove screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT 
            // as it locks the entire system. 
            // Instead, we will hide the island in landscape mode.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            x = 0
            y = 0
        }
    }

    private fun getTouchableParams(): WindowManager.LayoutParams {
        return baseParams().apply {
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            applyLockScreenFlags(this)
        }
    }

    private fun getNonTouchableParams(): WindowManager.LayoutParams {
        return baseParams().apply {
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            applyLockScreenFlags(this)
        }
    }

    private fun applyLockScreenFlags(params: WindowManager.LayoutParams) {
        if (OverlaySettings.isLockScreenOverlayEnabled(this) &&
            !OverlaySettings.isAccessibilityOverlayEnabled(this)
        ) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        }
    }

    private fun removeAppOverlay() {
        islandView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
                // no-op
            }
        }
        islandView = null
        islandParams = null
    }

    private fun observeWeather() {
        lifecycleScope.launch {
            weatherRepository.cachedWeather.collectLatest { weatherInfo ->
                if (weatherInfo != null) {
                    stateManager.pushState(
                        IslandState.Weather(
                            temperature = weatherInfo.temperature,
                            condition   = weatherInfo.condition,
                            iconCode    = weatherInfo.iconCode,
                            cityName    = weatherInfo.cityName,
                            isExpanded  = false
                        )
                    )
                    IslandLogger.d(TAG, "Weather state pushed: ${weatherInfo.temperature}° ${weatherInfo.condition}", null)
                } else {
                    stateManager.removeState(IslandState.Weather::class.java)
                }
            }
        }

        lifecycleScope.launch {
            while (true) {
                try {
                    weatherRepository.refreshWeather()
                } catch (e: Exception) {
                    IslandLogger.e(TAG, "Weather refresh error: ${e.message ?: "unknown"}", e)
                }
                delay(15 * 60 * 1000L)
            }
        }
    }

    private fun observeMedia() {
        lifecycleScope.launch {
            mediaRepository.realTimeMediaInfo.collectLatest { media ->
                if (media != null && media.isActive) {
                    val current = stateManager.currentState.value
                    val wasExpanded = (current as? IslandState.Media)?.isExpanded ?: false
                    stateManager.pushState(
                        IslandState.Media(
                            title       = media.title,
                            artist      = media.artist,
                            packageName = media.packageName,
                            isPlaying   = media.isPlaying,
                            albumArt    = media.albumArt,
                            albumArtUri = media.albumArtUri,
                            duration    = media.duration,
                            position    = media.position,
                            isExpanded  = wasExpanded
                        )
                    )
                } else {
                    stateManager.removeState(IslandState.Media::class.java)
                }
            }
        }
    }

    private fun observeBattery() {
        lifecycleScope.launch {
            var wasCharging = false
            batteryRepository.batteryInfo.collectLatest { battery ->
                if (battery.isCharging) {
                    stateManager.pushState(
                        IslandState.Charging(
                            batteryLevel = battery.level,
                            isCharging   = true,
                            chargeMethod = battery.chargeMethod.toIslandChargeMethod(),
                            isExpanded   = false // Always stay in compact pill mode
                        )
                    )
                    wasCharging = true
                } else {
                    if (wasCharging) {
                        delay(2000L)
                        stateManager.removeState(IslandState.Charging::class.java)
                    }
                    wasCharging = false
                }
            }
        }
    }

    private fun observeBluetooth() {
        lifecycleScope.launch {
            bluetoothRepository.connectedDevice.collectLatest { info ->
                if (info.deviceName != null) {
                    // Force a slightly longer duration (5s) to ensure visibility
                    stateManager.pushState(
                        IslandState.Bluetooth(
                            isConnected = true,
                            deviceName = info.deviceName,
                            batteryLevel = info.batteryLevel
                        )
                    )
                    IslandLogger.d(TAG, "Bluetooth state pushed: ${info.deviceName}, battery: ${info.batteryLevel}", null)
                } else {
                    // Only remove if it was actually connected before
                    if (stateManager.currentState.value is IslandState.Bluetooth) {
                        stateManager.removeState(IslandState.Bluetooth::class.java)
                    }
                }
            }
        }
    }

    private fun observeNotifications() {
        lifecycleScope.launch {
            NotificationRepository.notifications.collectLatest { queueState ->
                val current = queueState.current
                if (current != null && queueState.isNotEmpty) {
                    val currentState = stateManager.currentState.value
                    val wasExpanded = (currentState as? IslandState.Notification)?.isExpanded ?: false
                    stateManager.pushState(
                        IslandState.Notification(
                            appName     = current.appName,
                            title       = current.title,
                            content     = current.content,
                            packageName = current.packageName,
                            appIcon     = current.appIcon,
                            postTime    = current.timestamp,
                            queueCount  = queueState.items.size,
                            queueIndex  = queueState.safeIndex,
                            contentIntent = current.contentIntent,
                            actions = current.actions,
                            notificationKey = current.notificationKey,
                            isMessage = current.isMessage,
                            isExpanded = wasExpanded
                        )
                    )
                } else {
                    stateManager.removeState(IslandState.Notification::class.java)
                }
            }
        }
    }

    private fun observeCalls() {
        // We now rely on ExternalCallReceiver for calls from your new Dialer app.
        // This local observer is only a fallback and should be disabled if using 
        // the external dialer to avoid double pop-ups.
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
        if (telecomManager?.defaultDialerPackage != packageName) {
             IslandLogger.d(TAG, "External dialer detected, skipping local call observation", null)
             return
        }

        val callRepo = (application as? DynamicIslandApplication)?.callRepository ?: return
        lifecycleScope.launch {
            combine(callRepo.callState, callRepo.ongoingDuration) { state, duration ->
                state to duration
            }.collectLatest { (callState, duration) ->
                val current = stateManager.currentState.value
                val wasExpanded = (current as? IslandState.Call)?.isExpanded ?: false
                when (callState) {
                    is com.miui.dynamicisland.data.model.CallState.Ringing -> {
                        val number = callState.phoneNumber
                        val name = callRepo.getContactName(number)
                        val photo = callRepo.getContactPhoto(number)
                        stateManager.pushState(
                            IslandState.Call(
                                callerName = name ?: number ?: "Unknown",
                                callerSubtext = if (name != null) number ?: "" else "Incoming Call",
                                callerPhoto = photo,
                                isIncoming = true,
                                isOngoing = false,
                                isExpanded = true // Auto-expand on ringing
                            )
                        )
                    }
                    com.miui.dynamicisland.data.model.CallState.OffHook -> {
                        val wasExpanded = (current as? IslandState.Call)?.isExpanded ?: true
                        val prevCall = current as? IslandState.Call
                        stateManager.pushState(
                            IslandState.Call(
                                callerName = prevCall?.callerName ?: "Ongoing Call",
                                callerSubtext = prevCall?.callerSubtext ?: "",
                                callerPhoto = prevCall?.callerPhoto,
                                isIncoming = false,
                                isOngoing = true,
                                duration = duration,
                                isExpanded = wasExpanded
                            )
                        )
                    }
                    else -> {
                        stateManager.removeState(IslandState.Call::class.java)
                    }
                }
            }
        }
    }

    private fun handleMediaAction(action: MediaAction) {
        val current = stateManager.currentState.value as? IslandState.Media

        when (action) {
            MediaAction.PlayPause -> {
                if (current?.isPlaying == true) mediaRepository.pause()
                else mediaRepository.play()
            }
            MediaAction.Next     -> mediaRepository.next()
            MediaAction.Previous -> mediaRepository.previous()
            is MediaAction.Seek  -> {
                val durationMs = current?.duration ?: 0L
                if (durationMs > 0L) {
                    mediaRepository.seekTo((action.position * durationMs).toLong())
                }
            }
        }
    }

    private fun handleCallAction(action: CallAction) {
        val callRepo = (application as? DynamicIslandApplication)?.callRepository ?: return
        when (action) {
            CallAction.Accept  -> callRepo.acceptCall()
            CallAction.Decline -> callRepo.declineCall()
            CallAction.End     -> callRepo.endCall()
            CallAction.Mute    -> callRepo.toggleMute()
        }
    }

    private fun attachMediaSessionListener() {
        try {
            val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            mediaSessionManager = msm

            val notifListenerComponent = android.content.ComponentName(
                this, "com.miui.dynamicisland.service.IslandNotificationListener"
            )

            val initialSessions: List<MediaController>? = try {
                msm?.getActiveSessions(notifListenerComponent)
            } catch (e: SecurityException) {
                IslandLogger.w(TAG, "Notification listener not granted yet – media controls disabled", null)
                null
            }

            mediaRepository.updateFromController(initialSessions?.firstOrNull())

            msm?.addOnActiveSessionsChangedListener(
                mediaSessionListener, notifListenerComponent
            )
        } catch (e: Exception) {
            IslandLogger.e(TAG, "MediaSession listener error: ${e.message ?: "unknown"}", e)
        }
    }

    private fun registerReceivers() {
        val audioFilter = IntentFilter().apply {
            addAction(AudioModeReceiver.ACTION_RINGER_MODE_CHANGED)
            addAction(AudioModeReceiver.ACTION_VOLUME_CHANGED)
            addAction("android.media.RINGER_MODE_CHANGED")
        }
        audioUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    "android.media.RINGER_MODE_CHANGED" -> {
                        val mode = intent.getIntExtra("android.media.EXTRA_RINGER_MODE", -1)
                        if (mode != -1) {
                            stateManager.pushState(
                                IslandState.Silent(
                                    isSilent = mode != android.media.AudioManager.RINGER_MODE_NORMAL,
                                    ringerMode = mode
                                )
                            )
                        }
                    }
                    AudioModeReceiver.ACTION_RINGER_MODE_CHANGED -> {
                        val mode = intent.getIntExtra(AudioModeReceiver.EXTRA_RINGER_MODE, -1)
                        if (mode != -1) {
                            stateManager.pushState(
                                IslandState.Silent(
                                    isSilent = mode != android.media.AudioManager.RINGER_MODE_NORMAL,
                                    ringerMode = mode
                                )
                            )
                        }
                    }
                }
            }
        }

        ContextCompat.registerReceiver(
            this, batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            this, audioUpdateReceiver,
            audioFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun createNotification() = NotificationCompat
        .Builder(this, DynamicIslandApplication.CHANNEL_ID)
        .setContentTitle("Dynamic Island Active")
        .setContentText("Overlay service running")
        .setSmallIcon(android.R.drawable.ic_menu_info_details)
        .setOngoing(true)
        .build()

    private fun BatteryInfo.ChargeMethod.toIslandChargeMethod(): IslandState.Charging.ChargeMethod =
        when (this) {
            BatteryInfo.ChargeMethod.WIRED    -> IslandState.Charging.ChargeMethod.WIRED
            BatteryInfo.ChargeMethod.WIRELESS -> IslandState.Charging.ChargeMethod.WIRELESS
            else                              -> IslandState.Charging.ChargeMethod.UNKNOWN
        }

    override fun onDestroy() {
        islandView?.let {
            try { windowManager.removeView(it) }
            catch (e: Exception) {
                IslandLogger.e(TAG, "Remove view error: ${e.message ?: "unknown"}", e)
            }
        }
        try {
            unregisterReceiver(batteryReceiver)
            audioUpdateReceiver?.let { unregisterReceiver(it) }
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Unregister error: ${e.message ?: "unknown"}", e)
        }
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(mediaSessionListener)
        } catch (_: Exception) {}

        MediaRepositoryBridge.unregister()
        stateManager.clearAll()
        super.onDestroy()
    }
}
