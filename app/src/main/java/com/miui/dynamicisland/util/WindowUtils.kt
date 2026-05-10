// File: app/src/main/java/com/miui/dynamicisland/util/WindowUtils.kt

package com.miui.dynamicisland.util

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.DisplayCutout
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowInsetsCompat

object WindowUtils {

    private const val TAG = "WindowUtils"
    private var cachedStatusBarHeight = -1
    private var cachedNavigationBarHeight = -1
    private var cachedDisplayCutoutRect: Rect? = null

    fun getStatusBarHeight(context: Context): Int {
        if (cachedStatusBarHeight > 0) return cachedStatusBarHeight
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        cachedStatusBarHeight = if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            (24 * context.resources.displayMetrics.density).toInt()
        }
        return cachedStatusBarHeight
    }

    fun getScreenWidth(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return wm.currentWindowMetrics.bounds.width()
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(metrics)
        return metrics.widthPixels
    }

    fun getDisplayCutoutRect(context: Context): Rect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        if (cachedDisplayCutoutRect != null) return cachedDisplayCutoutRect
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        var cutoutRect: Rect? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            cutoutRect = wm.currentWindowMetrics.windowInsets.displayCutout?.boundingRects?.firstOrNull()
        } else {
            try {
                val display = wm.defaultDisplay
                val method = display.javaClass.getMethod("getCutout")
                val cutout = method.invoke(display)
                if (cutout != null) {
                    val getRects = cutout.javaClass.getMethod("getBoundingRects")
                    @Suppress("UNCHECKED_CAST")
                    val rects = getRects.invoke(cutout) as List<Rect>
                    cutoutRect = rects.firstOrNull()
                }
            } catch (e: Exception) {
                IslandLogger.w(TAG, "Failed to get cutout rect: ${e.message}", e)
            }
        }
        cachedDisplayCutoutRect = cutoutRect
        return cutoutRect
    }

    /** * CALCULATES BASE Y POSITION
     * Logic: If cutout exists, center the pill on the cutout.
     * If not, place it just below the status bar height.
     */
    fun getSafeIslandTopPosition(context: Context): Int {
        val statusBarHeight = getStatusBarHeight(context)
        val cutoutRect = getDisplayCutoutRect(context)

        return if (cutoutRect != null && !cutoutRect.isEmpty) {
            // Center the island vertically within the cutout area
            // Most punch holes are slightly smaller than the status bar height
            val cutoutCenterY = cutoutRect.centerY()
            // We return the top 'y' coordinate for the WindowManager
            // Typically, we want the island to start near the top of the cutout
            cutoutRect.top
        } else {
            // No cutout? Place at the very top, but padding will be handled by calibration
            0
        }
    }

    fun dpToPx(context: Context, dp: Float): Int = (dp * context.resources.displayMetrics.density).toInt()
}