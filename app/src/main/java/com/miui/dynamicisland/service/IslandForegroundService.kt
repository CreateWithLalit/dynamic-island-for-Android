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
import android.view.OrientationEventListener
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
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
import com.miui.dynamicisland.data.repository.*
import com.miui.dynamicisland.manager.CalibrationManager
import com.miui.dynamicisland.manager.IslandCalibration
import com.miui.dynamicisland.manager.IslandStateManager
import com.miui.dynamicisland.receiver.AudioModeReceiver
import com.miui.dynamicisland.receiver.BatteryReceiver
import com.miui.dynamicisland.service.IslandCallService
import com.miui.dynamicisland.ui.island.*
import com.miui.dynamicisland.ui.states.IslandState
import com.miui.dynamicisland.ui.states.IslandInputState
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
        const val ACTION_UPDATE_NOTCH_MODE = "com.miui.dynamicisland.UPDATE_NOTCH_MODE"
    }

    private lateinit var windowManager: WindowManager
    private var islandView: ComposeView? = null
    private var islandParams: WindowManager.LayoutParams? = null
    private var wasReplying: Boolean = false
    
    private var isFixNotchMode: Boolean = true
    private var isLandscapeEnabled: Boolean = true
    private var currentOrientation: Int = Configuration.ORIENTATION_PORTRAIT

    private val stateManager = IslandStateManager.getInstance()
    private lateinit var calibrationManager: CalibrationManager
    private lateinit var batteryRepository: BatteryRepository
    private lateinit var bluetoothRepository: BluetoothRepository
    private lateinit var timerRepository: TimerRepository
    lateinit var mediaRepository: MediaRepository
        private set
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var batteryReceiver: BatteryReceiver
    private var audioUpdateReceiver: BroadcastReceiver? = null
    private var clipboardManager: android.content.ClipboardManager? = null
    private var clipboardListener: android.content.ClipboardManager.OnPrimaryClipChangedListener? = null

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

    private var lastBluetoothDeviceName: String? = null

    private val settingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_UPDATE_NOTCH_MODE) {
                isFixNotchMode = OverlaySettings.isFixNotchMode(context)
                stateManager.setFixNotchMode(isFixNotchMode)
                isLandscapeEnabled = OverlaySettings.isLandscapeEnabled(context)
                // Force re-layout
                handleOrientationUpdate(resources.configuration.orientation)
            }
        }
    }

    private val orientationListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                
                val newOrientation = if (orientation in 45..134 || orientation in 225..314) {
                    Configuration.ORIENTATION_LANDSCAPE
                } else {
                    Configuration.ORIENTATION_PORTRAIT
                }
                
                if (newOrientation != currentOrientation) {
                    currentOrientation = newOrientation
                    handleOrientationUpdate(newOrientation)
                }
            }
        }
    }

    private fun handleOrientationUpdate(orientation: Int) {
        val params = islandParams ?: return
        val view = islandView ?: return
        val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
        
        // Sync actual landscape state to manager regardless of mode
        stateManager.setLandscapeMode(isLandscape)

        if (!isLandscapeEnabled && isLandscape) {
            view.visibility = android.view.View.GONE
            return
        }
        
        view.visibility = android.view.View.VISIBLE

        if (isLandscape && isFixNotchMode) {
            val side = detectNotchSide()
            when (side) {
                NotchSide.LEFT -> {
                    params.gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                    params.x = (8 * resources.displayMetrics.density).toInt()
                    params.y = 0
                }
                NotchSide.RIGHT -> {
                    params.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                    params.x = (8 * resources.displayMetrics.density).toInt()
                    params.y = 0
                }
                else -> {
                    params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    params.x = 0
                    params.y = 0
                }
            }
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            // PORTRAIT OR FLEXIBLE MODE
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.x = 0
            // In landscape, we usually want it at the very top (0), in portrait it follows status bar
            params.y = if (isLandscape) 0 else WindowUtils.getStatusBarHeight(this)
            params.width = if (stateManager.currentState.value.isExpanded) {
                WindowManager.LayoutParams.MATCH_PARENT
            } else {
                WindowManager.LayoutParams.WRAP_CONTENT
            }
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
        }

        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Orientation update error", e)
        }
    }

    private fun detectNotchSide(): NotchSide {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val windowMetrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowManager.currentWindowMetrics
            } else {
                null
            }
            val cutout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowMetrics?.windowInsets?.displayCutout
            } else {
                // Fallback for P, Q
                null
            } ?: return NotchSide.UNKNOWN

            val rects = cutout.boundingRects
            val displayMetrics = resources.displayMetrics
            
            // Using real display metrics to avoid issues with immersive mode reported sizes
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            
            return when {
                rects.any { it.left <= 40 && it.centerY().toDouble() in (screenHeight * 0.2..screenHeight * 0.8) } 
                    -> NotchSide.LEFT
                rects.any { it.right >= (screenWidth - 40) && it.centerY().toDouble() in (screenHeight * 0.2..screenHeight * 0.8) }
                    -> NotchSide.RIGHT
                else -> NotchSide.UNKNOWN
            }
        }
        return NotchSide.UNKNOWN
    }

    enum class NotchSide { LEFT, RIGHT, UNKNOWN }

    override fun onCreate() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        super.onCreate()

        windowManager     = getSystemService(WINDOW_SERVICE) as WindowManager
        calibrationManager = CalibrationManager(this)
        batteryRepository  = BatteryRepository(this)
        bluetoothRepository = BluetoothRepository(this)
        timerRepository    = TimerRepository(this)
        mediaRepository    = MediaRepository(this)
        MediaRepositoryBridge.register(mediaRepository)
        weatherRepository  = WeatherRepository(this)
        batteryReceiver    = BatteryReceiver()
        
        isFixNotchMode = OverlaySettings.isFixNotchMode(this)
        stateManager.setFixNotchMode(isFixNotchMode)
        isLandscapeEnabled = OverlaySettings.isLandscapeEnabled(this)
        
        val filter = IntentFilter(ACTION_UPDATE_NOTCH_MODE)
        ContextCompat.registerReceiver(this, settingsReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        
        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable()
        }
        
        setupClipboardListener()

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
                    onCallAction  = { handleCallAction(it) },
                    onWeatherAction = { handleWeatherAction(it) },
                    onClipboardAction = { handleClipboardAction(it) },
                    onTimerAction = { handleTimerAction(it) }
                )
            }

            setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
                    stateManager.collapseCurrentState()
                    true
                } else {
                    false
                }
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

        // 3. Input Mode Observer
        lifecycleScope.launch {
            stateManager.inputState.collectLatest { inputMode ->
                when (inputMode) {
                    is com.miui.dynamicisland.ui.states.IslandInputState.Normal -> exitReplyMode()
                    is com.miui.dynamicisland.ui.states.IslandInputState.ReplyMode -> enterReplyMode()
                }
            }
        }
    }

    private fun enterReplyMode() {
        IslandLogger.d(TAG, "Enter Reply Mode", null)
        val params = islandParams ?: return
        val view = islandView ?: return

        // Remove FLAG_NOT_FOCUSABLE to allow keyboard
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE

        try {
            windowManager.updateViewLayout(view, params)
            IslandLogger.d(TAG, "Window flags updated for Reply Mode (Focusable)", null)
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Error entering reply mode", e)
        }
    }

    private fun exitReplyMode() {
        IslandLogger.d(TAG, "Exit Reply Mode", null)
        val params = islandParams ?: return
        val view = islandView ?: return

        // Restore FLAG_NOT_FOCUSABLE
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN

        try {
            windowManager.updateViewLayout(view, params)
            hideIme(view)
            IslandLogger.d(TAG, "Window flags restored to Normal (Non-focusable)", null)
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Error exiting reply mode", e)
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

        val isReplyingNow = (state as? IslandState.Notification)?.isReplying == true
        wasReplying = isReplyingNow
    }

    private fun hideIme(targetView: android.view.View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager ?: return
        imm.hideSoftInputFromWindow(targetView.windowToken, 0)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Manual listener (OrientationEventListener) handles the refined logic
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

    private fun setupClipboardListener() {
        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboardListener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
            val clip = clipboardManager?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString()
                if (!text.isNullOrBlank()) {
                    stateManager.pushState(IslandState.Clipboard(text))
                }
            }
        }
        clipboardManager?.addPrimaryClipChangedListener(clipboardListener)
    }

    private fun handleClipboardAction(action: com.miui.dynamicisland.ui.island.ClipboardAction) {
        val current = stateManager.currentState.value as? IslandState.Clipboard ?: return
        val text = current.text
        
        stateManager.collapseCurrentState()
        stateManager.removeState(IslandState.Clipboard::class.java)

        val intent = when (action) {
            com.miui.dynamicisland.ui.island.ClipboardAction.Search -> {
                Intent(Intent.ACTION_WEB_SEARCH).apply { putExtra(android.app.SearchManager.QUERY, text) }
            }
            com.miui.dynamicisland.ui.island.ClipboardAction.Translate -> {
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://translate.google.com/?sl=auto&tl=en&text=${android.net.Uri.encode(text)}&op=translate"))
            }
            com.miui.dynamicisland.ui.island.ClipboardAction.Share -> {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            if (action == com.miui.dynamicisland.ui.island.ClipboardAction.Share) {
                startActivity(Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else {
                startActivity(intent)
            }
        } catch (_: Exception) {}
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
                            sunrise     = weatherInfo.sunrise,
                            sunset      = weatherInfo.sunset,
                            windSpeed   = weatherInfo.windSpeed,
                            humidity    = weatherInfo.humidity,
                            visibility  = weatherInfo.visibility,
                            precipitation = weatherInfo.precipitation,
                            hourlyForecast = weatherInfo.hourlyForecast,
                            dailyForecast = weatherInfo.dailyForecast,
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
                
                val currentInfo = weatherRepository.cachedWeather.value
                val isRaining = (currentInfo?.precipitation ?: 0.0) > 0.0
                
                // If raining, refresh every 1 minute. Otherwise, every 10 minutes.
                val refreshInterval = if (isRaining) 1 * 60 * 1000L else 10 * 60 * 1000L
                delay(refreshInterval)
            }
        }
    }

    private fun handleWeatherAction(action: com.miui.dynamicisland.ui.island.WeatherAction) {
        when (action) {
            com.miui.dynamicisland.ui.island.WeatherAction.Refresh -> {
                lifecycleScope.launch {
                    try {
                        weatherRepository.refreshWeather()
                        IslandLogger.d(TAG, "Weather manually refreshed", null)
                    } catch (e: Exception) {
                        IslandLogger.e(TAG, "Manual weather refresh failed", e)
                    }
                }
            }
        }
    }

    private fun handleTimerAction(action: com.miui.dynamicisland.ui.island.TimerAction) {
        when (action) {
            com.miui.dynamicisland.ui.island.TimerAction.PauseResume -> timerRepository.pauseResume()
            com.miui.dynamicisland.ui.island.TimerAction.Reset -> timerRepository.reset()
            com.miui.dynamicisland.ui.island.TimerAction.Secondary -> {
                val current = stateManager.currentState.value as? IslandState.Timer ?: return
                if (current.mode is IslandState.Timer.TimerMode.Countdown) {
                    timerRepository.addOneMinute()
                } else if (current.mode is IslandState.Timer.TimerMode.Stopwatch) {
                    timerRepository.lap()
                }
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
                            wattage      = 33, // Default to 33W as requested
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
            combine(
                bluetoothRepository.connectedDevice,
                BluetoothBatteryStore.snapshot
            ) { info, snapshot ->
                info to snapshot
            }.collectLatest { (info, snapshot) ->
                val snapshotFresh = snapshot?.isFresh(10_000L) == true
                val snapshotMatches = snapshotFresh && snapshot?.matchesDeviceName(info.deviceName) == true
                // If it's fresh and from GMS, we trust it even if the system hasn't fully connected yet
                val snapshotUsable = snapshotFresh && (snapshotMatches || info.deviceName.isNullOrBlank() || info.isConnected)

                val resolvedName = if (snapshotFresh && !snapshot?.deviceName.isNullOrBlank()) snapshot?.deviceName else info.deviceName
                val overallFromSnapshot = if (snapshotUsable) snapshot?.overallOrNull() else null
                val overallBattery = overallFromSnapshot ?: info.batteryLevel

                val left = if (snapshotUsable) snapshot?.batteryLeft else null
                val right = if (snapshotUsable) snapshot?.batteryRight else null
                val caseLevel = if (snapshotUsable) snapshot?.batteryCase else null

                // Logic to expand for detailed battery updates (e.g. from GMS notification)
                val hasDetailedBattery = left != null || right != null || caseLevel != null
                val isFreshNotificationUpdate = snapshot?.isFresh(10_000L) == true && hasDetailedBattery

                val shouldShow = (info.isConnected || isFreshNotificationUpdate) && !resolvedName.isNullOrBlank()
                val isNewConnection = shouldShow && (resolvedName != lastBluetoothDeviceName)
                lastBluetoothDeviceName = if (shouldShow) resolvedName else null

                if (shouldShow) {
                    stateManager.pushState(
                        IslandState.Bluetooth(
                            isConnected = info.isConnected || isFreshNotificationUpdate,
                            deviceName = resolvedName!!,
                            batteryLevel = overallBattery,
                            batteryLeft = left,
                            batteryRight = right,
                            batteryCase = caseLevel
                        )
                    )
                    
                    if (isNewConnection || isFreshNotificationUpdate) {
                        // For TWS connections, keep it in compact pill mode as requested.
                        // stateManager.expandCurrentState(ms = 4000L, forceAutoCollapse = true)
                    }
                    
                    IslandLogger.d(TAG, "Bluetooth state pushed: $resolvedName, battery: $overallBattery, new: $isNewConnection, detailPop: $isFreshNotificationUpdate", null)
                } else {
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
                    val wasReplying = (currentState as? IslandState.Notification)?.let {
                        if (it.notificationKey == current.notificationKey) it.isReplying else false
                    } ?: false

                    stateManager.pushState(
                        IslandState.Notification(
                            appName     = current.appName,
                            title       = current.title,
                            content     = current.content,
                            packageName = current.packageName,
                            appIcon     = current.appIcon,
                            largeIcon   = current.largeIcon,
                            postTime    = current.timestamp,
                            queueCount  = queueState.items.size,
                            queueIndex  = queueState.safeIndex,
                            contentIntent = current.contentIntent,
                            actions = current.actions,
                            notificationKey = current.notificationKey,
                            isMessage = current.isMessage,
                            isExpanded = wasExpanded,
                            isReplying = wasReplying
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
                                isSpeakerOn = callRepo.isSpeakerOn(),
                                isMuted = callRepo.isMuted(),
                                isExpanded = true // Auto-expand on ringing
                            )
                        )
                    }
                    com.miui.dynamicisland.data.model.CallState.OffHook -> {
                        val prevCall = current as? IslandState.Call
                        stateManager.pushState(
                            IslandState.Call(
                                callerName = prevCall?.callerName ?: "Ongoing Call",
                                callerSubtext = prevCall?.callerSubtext ?: "",
                                callerPhoto = prevCall?.callerPhoto,
                                isIncoming = false,
                                isOngoing = true,
                                isSpeakerOn = callRepo.isSpeakerOn(),
                                isMuted = callRepo.isMuted(),
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

        // Common logic for buttons in expanded state
        val isExpanded = current?.isExpanded == true

        when (action) {
            MediaAction.PlayPause -> {
                if (current?.isPlaying == true) mediaRepository.pause()
                else mediaRepository.play()
                if (isExpanded) {
                    stateManager.expandCurrentState(4000L, forceAutoCollapse = true)
                    mediaRepository.launchMusicApp()
                }
            }
            MediaAction.Next -> {
                mediaRepository.next()
                if (isExpanded) {
                    stateManager.expandCurrentState(4000L, forceAutoCollapse = true)
                    mediaRepository.launchMusicApp()
                }
            }
            MediaAction.Previous -> {
                mediaRepository.previous()
                if (isExpanded) {
                    stateManager.expandCurrentState(4000L, forceAutoCollapse = true)
                    mediaRepository.launchMusicApp()
                }
            }
            is MediaAction.Seek -> {
                val durationMs = current?.duration ?: 0L
                if (durationMs > 0L) {
                    mediaRepository.seekTo((action.position * durationMs).toLong())
                }
                if (isExpanded) {
                    stateManager.expandCurrentState(4000L, forceAutoCollapse = true)
                }
            }
            MediaAction.LaunchApp -> {
                mediaRepository.launchMusicApp()
            }
        }
    }

    private fun handleCallAction(action: CallAction) {
        val callRepo = (application as? DynamicIslandApplication)?.callRepository ?: return
        val current = stateManager.currentState.value as? IslandState.Call
        val isExpanded = current?.isExpanded == true

        when (action) {
            CallAction.Accept -> {
                callRepo.acceptCall()
                if (isExpanded) stateManager.expandCurrentState(4000L, forceAutoCollapse = true)
            }
            CallAction.Decline -> {
                callRepo.declineCall()
                // Usually declines close the pill anyway via state removal, but safety first:
                if (isExpanded) stateManager.collapseCurrentState()
            }
            CallAction.End -> {
                callRepo.endCall()
                if (isExpanded) stateManager.collapseCurrentState()
            }
            CallAction.Mute -> {
                callRepo.toggleMute()
                if (current != null) {
                    // Immediate UI feedback
                    stateManager.pushState(current.copy(isMuted = !current.isMuted))
                    if (isExpanded) stateManager.expandCurrentState(4000L, forceAutoCollapse = true)
                }
            }
            CallAction.ToggleSpeaker -> {
                callRepo.toggleSpeaker()
                if (current != null) {
                    // Immediate UI feedback
                    stateManager.pushState(current.copy(isSpeakerOn = !current.isSpeakerOn))
                    if (isExpanded) stateManager.expandCurrentState(4000L, forceAutoCollapse = true)
                }
            }
            CallAction.LaunchApp -> {
                callRepo.launchDialerApp()
            }
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

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        val screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        stateManager.pushState(IslandState.LockScreen(countAllNotifications()))
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        stateManager.removeState(IslandState.LockScreen::class.java)
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
        ContextCompat.registerReceiver(
            this, screenReceiver,
            screenFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun countAllNotifications(): Int {
        var count = 0
        val allStates = stateManager.allStates.value
        allStates.forEach { state ->
            when (state) {
                is IslandState.Notification -> count += state.queueCount
                is IslandState.Call -> count++
                // Media, Weather, Bluetooth, etc. are persistent states, not "unread" notifications
                else -> {}
            }
        }
        return count
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
        orientationListener.disable()
        try {
            unregisterReceiver(settingsReceiver)
        } catch (_: Exception) {}

        islandView?.let {
            try { windowManager.removeView(it) }
            catch (e: Exception) {
                IslandLogger.e(TAG, "Remove view error: ${e.message ?: "unknown"}", e)
            }
        }
        try {
            unregisterReceiver(batteryReceiver)
            audioUpdateReceiver?.let { unregisterReceiver(it) }
            clipboardListener?.let { clipboardManager?.removePrimaryClipChangedListener(it) }
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
