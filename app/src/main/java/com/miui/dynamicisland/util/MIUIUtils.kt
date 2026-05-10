// File: app/src/main/java/com/miui/dynamicisland/util/MIUIUtils.kt
// Purpose: MIUI specific utilities for AutoStart and Battery Optimization
// Hinglish: Is file mein MIUI phones ke liye specific settings open karne ke shortcuts hain.

package com.miui.dynamicisland.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

object MIUIUtils {

    fun isMIUI(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco")
    }

    fun getAutoStartIntent(): Intent {
        return Intent().apply {
            component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
        }
    }

    fun getBatteryOptimizationIntent(context: Context): Intent {
        return Intent().apply {
            component = ComponentName("com.miui.securitycenter", "com.miui.powercenter.detail.PowerUsageDetailActivity")
            putExtra("package_name", context.packageName)
            putExtra("package_label", "Dynamic Island")
        }
    }
}
