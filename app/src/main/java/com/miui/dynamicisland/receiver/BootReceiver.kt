// File: app/src/main/java/com/miui/dynamicisland/receiver/BootReceiver.kt
package com.miui.dynamicisland.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.miui.dynamicisland.service.IslandForegroundService
import com.miui.dynamicisland.util.IslandLogger
import com.miui.dynamicisland.util.PermissionUtils

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        var action = intent.action ?: return

        // Standard boot and MIUI quick boot actions
        if (action != Intent.ACTION_BOOT_COMPLETED && action != "android.intent.action.QUICKBOOT_POWERON") {
            return
        }

        IslandLogger.d("BootReceiver", "Boot completed, attempting to start service", null)

        if (!PermissionUtils.canDrawOverlays(context)) {
            IslandLogger.d("BootReceiver", "Overlay permission missing – service not started", null)
            return
        }

        val serviceIntent = Intent(context, IslandForegroundService::class.java).apply {
            action = IslandForegroundService.ACTION_START
        }

        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            IslandLogger.e("BootReceiver", "Failed to start service after boot", e)
        }
    }
}