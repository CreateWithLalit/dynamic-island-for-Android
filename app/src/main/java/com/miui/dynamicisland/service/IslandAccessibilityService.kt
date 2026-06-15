package com.miui.dynamicisland.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Build
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.miui.dynamicisland.data.repository.MediaRepositoryBridge
import com.miui.dynamicisland.data.repository.NotificationData
import com.miui.dynamicisland.data.repository.NotificationRepository
import com.miui.dynamicisland.manager.CalibrationManager
import com.miui.dynamicisland.manager.IslandCalibration
import com.miui.dynamicisland.manager.IslandStateManager
import com.miui.dynamicisland.ui.island.CallAction
import com.miui.dynamicisland.ui.island.DynamicIsland
import com.miui.dynamicisland.ui.island.MediaAction
import com.miui.dynamicisland.ui.states.IslandState
import com.miui.dynamicisland.util.IslandLogger
import com.miui.dynamicisland.util.OverlaySettings
import com.miui.dynamicisland.util.WindowUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IslandAccessibilityService : AccessibilityService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    companion object {
        private const val TAG = "IslandAccessibilityService"
        private const val ACTION_ANSWER_CALL = 10
        private const val ACTION_DISMISS_CALL = 11
        var instance: IslandAccessibilityService? = null
            private set

        fun acceptCall(): Boolean {
            val inst = instance ?: return false

            // Method 1: Standard Android 8+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val result = inst.performGlobalAction(ACTION_ANSWER_CALL)
                    if (result) return true
                } catch (e: Exception) {
                    IslandLogger.e(TAG, "Global accept failed", e)
                }
            }

            // Method 2: MIUI specific — simulate tap on answer button
            try {
                val root = inst.rootInActiveWindow
                if (root != null) {
                    val nodes = root.findAccessibilityNodeInfosByText("Answer")
                        ?: root.findAccessibilityNodeInfosByText("Accept")
                        ?: root.findAccessibilityNodeInfosByText("उत्तर दें")

                    nodes?.forEach { node ->
                        if (node.isClickable) {
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            return true
                        }
                    }
                }
            } catch (e: Exception) {
                IslandLogger.e(TAG, "Node search accept failed", e)
            }

            return false
        }

        fun declineCall(): Boolean {
            val inst = instance ?: return false

            // Method 1: Standard Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val result = inst.performGlobalAction(ACTION_DISMISS_CALL)
                    if (result) return true
                } catch (e: Exception) {
                    IslandLogger.e(TAG, "Global decline failed", e)
                }
            }

            // Method 2: Find decline/reject button
            try {
                val root = inst.rootInActiveWindow
                if (root != null) {
                    val nodes = root.findAccessibilityNodeInfosByText("Decline")
                        ?: root.findAccessibilityNodeInfosByText("Reject")
                        ?: root.findAccessibilityNodeInfosByText("अस्वीकार")

                    nodes?.forEach { node ->
                        if (node.isClickable) {
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            return true
                        }
                    }
                }
            } catch (e: Exception) {
                IslandLogger.e(TAG, "Node search decline failed", e)
            }

            // Method 3: Back button fallback
            try {
                inst.performGlobalAction(GLOBAL_ACTION_BACK)
            } catch (e: Exception) {
                IslandLogger.e(TAG, "Back button fallback failed", e)
            }
            return false
        }
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private val serviceViewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = serviceViewModelStore

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val stateManager = IslandStateManager.getInstance()
    private lateinit var calibrationManager: CalibrationManager
    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var screenReceiver: BroadcastReceiver? = null
    private var clipboardManager: android.content.ClipboardManager? = null
    private var clipboardListener: android.content.ClipboardManager.OnPrimaryClipChangedListener? = null
    private var overlayJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        IslandLogger.d(TAG, "Accessibility service connected", null)

        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        calibrationManager = CalibrationManager(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        setupClipboardListener()

        updateOverlayVisibility()
        registerScreenReceiver()

        // Bridge media controller via accessibility (backup method)
        try {
            val msm = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val component = android.content.ComponentName(this, IslandAccessibilityService::class.java)
            val controllers = msm?.getActiveSessions(component)
            val firstController = controllers?.firstOrNull()

            if (firstController != null) {
                MediaRepositoryBridge.updateFromController(firstController)
                IslandLogger.d(TAG, "Media bridged via accessibility", null)
            }
        } catch (e: SecurityException) {
            IslandLogger.w(TAG, "No media session permission", null)
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Media bridge error", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                handleNotificationEvent(event)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Detect app changes if needed
                IslandLogger.d(TAG, "Window changed: ${event.packageName}", null)
                updateOverlayVisibility()
            }
        }
    }

    private fun handleNotificationEvent(event: AccessibilityEvent) {
        // If the specialized NotificationListenerService is running, ignore accessibility notifications
        // to avoid duplicates and get better data quality from SBNs.
        if (IslandNotificationListener.isServiceConnected()) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        // Ignore system apps
        if (packageName in setOf("android", "com.android.systemui")) return

        val text = event.text?.joinToString(" ") ?: ""
        if (text.isBlank()) return

        // Real app name
        val realAppName: String = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName  // fallback to package name
        }

        // Real app icon Drawable
        val appIconDrawable: Drawable? = try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null  // fallback -> "C" letter will show
        }

        val notificationData = NotificationData(
            appName = realAppName,
            title = text,
            content = "",
            packageName = packageName,
            appIcon = appIconDrawable,
            timestamp = System.currentTimeMillis(),
            contentIntent = null,
            actions = null,
            notificationKey = "${packageName}_${System.currentTimeMillis()}",
            isNotEmpty = true
        )

        IslandLogger.d(TAG, "Notification via accessibility: $packageName - $text", null)
        NotificationRepository.postNotification(notificationData)
    }

    override fun onInterrupt() {
        IslandLogger.d(TAG, "Accessibility service interrupted", null)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        IslandLogger.d(TAG, "Accessibility service unbound", null)
        instance = null
        detachOverlay()
        unregisterScreenReceiver()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        detachOverlay()
        unregisterScreenReceiver()
        clipboardListener?.let { clipboardManager?.removePrimaryClipChangedListener(it) }
        serviceViewModelStore.clear()
        super.onDestroy()
    }

    private fun updateOverlayVisibility() {
        if (shouldShowAccessibilityOverlay()) {
            ensureOverlayAttached()
        } else {
            detachOverlay()
        }
    }

    private fun shouldShowAccessibilityOverlay(): Boolean {
        return OverlaySettings.isAccessibilityOverlayEnabled(this)
    }

    private fun ensureOverlayAttached() {
        if (overlayView != null) return

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@IslandAccessibilityService)
            setViewTreeViewModelStoreOwner(this@IslandAccessibilityService)
            setViewTreeSavedStateRegistryOwner(this@IslandAccessibilityService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val currentState by stateManager.currentState.collectAsState()
                val calibration by calibrationManager.calibration.collectAsState(
                    initial = IslandCalibration.default()
                )

                DynamicIsland(
                    state = currentState,
                    calibration = calibration,
                    onMediaAction = { handleMediaAction(it) },
                    onCallAction = { handleCallAction(it) },
                    onClipboardAction = { handleClipboardAction(it) }
                )
            }
        }

        val params = buildAccessibilityParams()
        try {
            windowManager.addView(view, params)
            overlayView = view
            overlayParams = params
            startOverlayObservers()
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Accessibility overlay init error: ${e.message ?: "unknown"}", e)
        }
    }

    private fun detachOverlay() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
                // no-op
            }
        }
        overlayView = null
        overlayParams = null
        overlayJob?.cancel()
        overlayJob = null

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    private fun buildAccessibilityParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (OverlaySettings.isLockScreenOverlayEnabled(this@IslandAccessibilityService)) {
                flags = flags or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            }
        }
    }

    private fun startOverlayObservers() {
        overlayJob?.cancel()
        overlayJob = serviceScope.launch {
            launch {
                calibrationManager.calibration.collectLatest { cal ->
                    val params = overlayParams ?: return@collectLatest
                    val density = resources.displayMetrics.density
                    val baseSafeY = WindowUtils.getStatusBarHeight(this@IslandAccessibilityService)
                    params.x = (cal.offsetX * density).toInt()
                    params.y = baseSafeY + (cal.offsetY * density).toInt()
                    overlayView?.let { view ->
                        try {
                            windowManager.updateViewLayout(view, params)
                        } catch (_: Exception) {
                            // no-op
                        }
                    }
                }
            }
            launch {
                stateManager.currentState.collectLatest { state ->
                    val params = overlayParams ?: return@collectLatest
                    params.width = if (state.isExpanded) {
                        WindowManager.LayoutParams.MATCH_PARENT
                    } else {
                        WindowManager.LayoutParams.WRAP_CONTENT
                    }
                    overlayView?.let { view ->
                        try {
                            windowManager.updateViewLayout(view, params)
                        } catch (_: Exception) {
                            // no-op
                        }
                    }
                }
            }
        }
    }

    private fun handleMediaAction(action: MediaAction) {
        when (action) {
            MediaAction.PlayPause -> MediaRepositoryBridge.togglePlayPause()
            MediaAction.Next -> MediaRepositoryBridge.next()
            MediaAction.Previous -> MediaRepositoryBridge.previous()
            is MediaAction.Seek -> MediaRepositoryBridge.seekTo(action.position)
            MediaAction.LaunchApp -> MediaRepositoryBridge.launchApp()
        }
    }

    private fun handleCallAction(action: CallAction) {
        val callRepo = (application as? com.miui.dynamicisland.DynamicIslandApplication)?.callRepository
            ?: return
        when (action) {
            CallAction.Accept  -> callRepo.acceptCall()
            CallAction.Decline -> callRepo.declineCall()
            CallAction.End     -> callRepo.endCall()
            CallAction.Mute    -> callRepo.toggleMute()
            CallAction.ToggleSpeaker -> callRepo.toggleSpeaker()
            CallAction.LaunchApp -> callRepo.launchDialerApp()
        }
    }

    private fun handleClipboardAction(clipboardAction: com.miui.dynamicisland.ui.island.ClipboardAction) {
        val state = stateManager.currentState.value as? IslandState.Clipboard ?: return
        val text = state.text
        
        stateManager.collapseCurrentState()
        stateManager.removeState(IslandState.Clipboard::class.java)

        when (clipboardAction) {
            com.miui.dynamicisland.ui.island.ClipboardAction.Search -> {
                val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(android.app.SearchManager.QUERY, text)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/search?q=$text")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(browserIntent)
                }
            }
            com.miui.dynamicisland.ui.island.ClipboardAction.Translate -> {
                val intent = Intent().apply {
                    action = Intent.ACTION_VIEW
                    data = android.net.Uri.parse("https://translate.google.com/?sl=auto&tl=en&text=${android.net.Uri.encode(text)}&op=translate")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
            com.miui.dynamicisland.ui.island.ClipboardAction.Share -> {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val shareIntent = Intent.createChooser(intent, "Share via").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(shareIntent)
            }
        }
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

    private fun registerScreenReceiver() {
        if (screenReceiver != null) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                updateOverlayVisibility()
            }
        }
        registerReceiver(screenReceiver, filter)
    }

    private fun unregisterScreenReceiver() {
        screenReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {
                // no-op
            }
        }
        screenReceiver = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        overlayView?.let { view ->
            val isPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
            view.visibility = if (isPortrait) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
}
