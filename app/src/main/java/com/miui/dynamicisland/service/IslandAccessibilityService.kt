package com.miui.dynamicisland.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.view.accessibility.AccessibilityEvent
import com.miui.dynamicisland.data.repository.MediaRepositoryBridge
import com.miui.dynamicisland.data.repository.NotificationData
import com.miui.dynamicisland.data.repository.NotificationRepository
import com.miui.dynamicisland.manager.IslandStateManager
import com.miui.dynamicisland.ui.states.IslandState
import com.miui.dynamicisland.util.IslandLogger

class IslandAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "IslandAccessibilityService"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
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

        val notificationData = NotificationData(
            appName = packageName,
            title = text,
            content = "",
            packageName = packageName,
            icon = null,
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
        return super.onUnbind(intent)
    }
}
