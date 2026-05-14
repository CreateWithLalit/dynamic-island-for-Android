package com.miui.dynamicisland.service

import android.app.Notification
import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.miui.dynamicisland.data.repository.MediaRepositoryBridge
import com.miui.dynamicisland.data.repository.NotificationData
import com.miui.dynamicisland.data.repository.NotificationRepository
import com.miui.dynamicisland.util.IslandLogger

class IslandNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "IslandNotificationListener"
    }

    private val ignoredPackages = setOf(
        "android",
        "com.android.systemui",
        "com.miui.dynamicisland",
        "com.android.settings",
        "com.google.android.gms" // Ignore Google Play Services background alerts
    )

    override fun onListenerConnected() {
        super.onListenerConnected()
        IslandLogger.d(TAG, "Connected", null)

        try {
            val msm = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val component = ComponentName(this, IslandNotificationListener::class.java)
            val controllers = msm?.getActiveSessions(component)
            val firstController = controllers?.firstOrNull()

            if (firstController != null) {
                MediaRepositoryBridge.updateFromController(firstController)
                IslandLogger.d(TAG, "Bridged media controller: ${firstController.packageName}", null)
            }
        } catch (e: SecurityException) {
            IslandLogger.w(TAG, "No permission to get active sessions yet", null)
        } catch (e: Exception) {
            IslandLogger.e(TAG, "Error bridging media", e)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        // 1. Refined Blacklist & System Noise Filter
        if (packageName in ignoredPackages) return
        if (sbn.isOngoing || (sbn.notification.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0)) return
        if (sbn.notification.extras.containsKey("android.mediaSession")) return

        val title = sbn.notification.extras
            .getCharSequence(android.app.Notification.EXTRA_TITLE)
            ?.toString()
            .orEmpty()

        val content = sbn.notification.extras
            .getCharSequence(android.app.Notification.EXTRA_TEXT)
            ?.toString()
            .orEmpty()

        if (title.isBlank() && content.isBlank()) return

        // 2. Real App Metadata (Universal Launcher Icon)
        val realAppName: String = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            IslandLogger.d(TAG, "Label fetch failed for $packageName", e)
            packageName
        }

        val appIconDrawable: Drawable? = try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            IslandLogger.d(TAG, "Icon fetch failed for $packageName", e)
            null
        }

        val notificationData = NotificationData(
            appName = realAppName,
            title = title,
            content = content,
            packageName = packageName,
            appIcon = appIconDrawable,
            timestamp = sbn.postTime,
            isNotEmpty = true
        )

        IslandLogger.d(TAG, "Posting high-res notification from: $packageName", null)
        NotificationRepository.postNotification(notificationData)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val notificationId = sbn.key ?: "${sbn.packageName}_${sbn.id}_${sbn.postTime}"
        IslandLogger.d(TAG, "Notification removed: $notificationId", null)
        
        // Clear the notification state
        NotificationRepository.postNotification(NotificationData.EMPTY)
    }
}
