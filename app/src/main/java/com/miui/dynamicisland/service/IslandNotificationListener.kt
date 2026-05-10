package com.miui.dynamicisland.service

import android.app.Notification
import android.content.ComponentName
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
        "com.miui.home",
        "com.android.settings"
    )

    private val priorityPackages = setOf(
        "com.whatsapp",
        "com.telegram.messenger",
        "com.google.android.apps.messaging",
        "com.facebook.orca"
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

        if (packageName in ignoredPackages) return
        if (sbn.isOngoing && packageName !in priorityPackages) return
        if (sbn.notification.extras.containsKey("android.mediaSession")) return

        val title = sbn.notification.extras
            .getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()
            .orEmpty()

        val content = sbn.notification.extras
            .getCharSequence(Notification.EXTRA_TEXT)
            ?.toString()
            .orEmpty()

        if (title.isBlank() && content.isBlank()) return

        val notificationData = NotificationData(
            appName = packageName,
            title = title,
            content = content,
            packageName = packageName,
            icon = sbn.notification.smallIcon,
            timestamp = sbn.postTime,
            isNotEmpty = true
        )

        IslandLogger.d(TAG, "Posting notification from: $packageName", null)
        NotificationRepository.postNotification(notificationData)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val notificationId = sbn.key ?: "${sbn.packageName}_${sbn.id}_${sbn.postTime}"
        IslandLogger.d(TAG, "Notification removed: $notificationId", null)
        
        // Clear the notification state
        NotificationRepository.postNotification(NotificationData.EMPTY)
    }
}
