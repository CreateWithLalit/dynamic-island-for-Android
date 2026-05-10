package com.miui.dynamicisland.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
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
import com.miui.dynamicisland.ui.states.IslandState

class IslandOverlayWindow(
    private val context: Context
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore = ViewModelStore()

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var currentParams: WindowManager.LayoutParams? = null
    private var isAttached = false
    private var canInteract = false

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 0
        }
    }

    fun updateStateConfiguration(state: IslandState) {
        val params = currentParams ?: return
        val view = composeView ?: return

        val shouldAllowTouch = state.allowInteraction || state.priority > 0

        if (canInteract != shouldAllowTouch) {
            canInteract = shouldAllowTouch

            params.flags = if (shouldAllowTouch) {
                params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            } else {
                params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            }

            windowManager.updateViewLayout(view, params)
        }
    }

    fun show(content: @Composable () -> Unit) {
        if (isAttached) return

        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        currentParams = buildLayoutParams()

        composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@IslandOverlayWindow)
            setViewTreeViewModelStoreOwner(this@IslandOverlayWindow)
            setViewTreeSavedStateRegistryOwner(this@IslandOverlayWindow)
            setContent {
                content()
            }

            setOnClickListener {
                performClick()
            }
        }

        try {
            windowManager.addView(composeView, currentParams)
            isAttached = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hide() {
        if (!isAttached) return

        composeView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        composeView = null
        currentParams = null
        isAttached = false
        canInteract = false

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    fun destroy() {
        hide()
        viewModelStore.clear()
    }

    val isShowing: Boolean
        get() = isAttached
}
