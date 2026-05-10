package com.miui.dynamicisland.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.drawable.Drawable
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.miui.dynamicisland.data.repository.MediaRepositoryBridge
import com.miui.dynamicisland.data.repository.NotificationData
import com.miui.dynamicisland.data.repository.NotificationRepository
import com.miui.dynamicisland.manager.IslandStateManager
import com.miui.dynamicisland.ui.states.IslandState
import com.miui.dynamicisland.util.IslandLogger

class IslandAccessibilityService : AccessibilityService() {

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

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        IslandLogger.d(TAG, "Accessibility service connected", null)

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
            }
        }
    }

    private fun handleNotificationEvent(event: AccessibilityEvent) {
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
        return super.onUnbind(intent)
    }
}
