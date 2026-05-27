package com.miui.dynamicisland.util

import android.content.Context

object OverlaySettings {
    private const val PREFS_NAME = "dynamic_island_overlay_settings"
    private const val KEY_ACCESSIBILITY_OVERLAY = "use_accessibility_overlay"
    private const val KEY_LOCK_SCREEN_OVERLAY = "lock_screen_overlay"

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

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

