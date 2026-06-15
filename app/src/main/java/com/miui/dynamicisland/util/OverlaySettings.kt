package com.miui.dynamicisland.util

import android.content.Context

object OverlaySettings {
    private const val PREFS_NAME = "dynamic_island_overlay_settings"
    private const val KEY_ACCESSIBILITY_OVERLAY = "use_accessibility_overlay"
    private const val KEY_LOCK_SCREEN_OVERLAY = "lock_screen_overlay"
    private const val KEY_LANDSCAPE_ENABLED = "landscape_enabled"
    private const val KEY_FIX_NOTCH_MODE = "fix_notch_mode"

    fun isAccessibilityOverlayEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ACCESSIBILITY_OVERLAY, false)
    }

    fun setAccessibilityOverlayEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ACCESSIBILITY_OVERLAY, enabled).apply()
    }

    fun isLockScreenOverlayEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_LOCK_SCREEN_OVERLAY, false)
    }

    fun setLockScreenOverlayEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_LOCK_SCREEN_OVERLAY, enabled).apply()
    }

    fun isLandscapeEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_LANDSCAPE_ENABLED, true)
    }

    fun setLandscapeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_LANDSCAPE_ENABLED, enabled).apply()
    }

    fun isFixNotchMode(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_FIX_NOTCH_MODE, true)
    }

    fun setFixNotchMode(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_FIX_NOTCH_MODE, enabled).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

