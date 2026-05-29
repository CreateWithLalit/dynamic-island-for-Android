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
        @Volatile private var instance: IslandNotificationListener? = null

        fun cancelByKey(notificationKey: String?) {
            if (notificationKey.isNullOrBlank()) return
            try {
                instance?.cancelNotification(notificationKey)
            } catch (e: Exception) {
                IslandLogger.w(TAG, "Failed to cancel notification $notificationKey", e)
            }
        }

        fun cancelAllPosted() {
            try {
                instance?.cancelAllNotifications()
            } catch (e: Exception) {
                IslandLogger.w(TAG, "Failed to cancel all notifications", e)
            }
        }
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
        instance = this
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

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        // 1. Refined Blacklist & System Noise Filter
        if (packageName in ignoredPackages) return
        if (sbn.isOngoing || (sbn.notification.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0)) return
        if (sbn.notification.extras.containsKey("android.mediaSession")) return

        val extras = sbn.notification.extras
        var senderTitle = extras.getString(Notification.EXTRA_TITLE)
            ?: extras.getString(Notification.EXTRA_TITLE_BIG)
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: ""
            
        var messageBody = when {
            extras.containsKey("android.messagingStyleUser") || extras.containsKey(Notification.EXTRA_MESSAGES) -> {
                val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
                if (!messages.isNullOrEmpty()) {
                    val lastMessage = messages.last() as? android.os.Bundle
                    val text = lastMessage?.getCharSequence("text")?.toString()
                    if (text.isNullOrBlank()) extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() else text
                } else extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            }
            extras.containsKey(Notification.EXTRA_TEXT) -> extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            extras.containsKey(Notification.EXTRA_BIG_TEXT) -> extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            else -> null
        } ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n") { it.toString() } ?: ""

        // If one is empty, try to balance it
        if (senderTitle.isNotBlank() && messageBody.isBlank()) {
            // Instagram sometimes puts the whole thing in Title
            if (senderTitle.contains(":") || senderTitle.length > 15) {
                messageBody = senderTitle
                senderTitle = "" // Will fall back to App Name later
            }
        }

        // 2. Resolve true app label for user-facing name
        val realAppName: String = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            val substituteName = extras.getCharSequence("android.substName")?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            if (!substituteName.isNullOrBlank()) substituteName else packageName
        }
        
        if (senderTitle.isBlank()) senderTitle = realAppName

        val profileIcon: android.graphics.drawable.Icon? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_LARGE_ICON, android.graphics.drawable.Icon::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(Notification.EXTRA_LARGE_ICON) as? android.graphics.drawable.Icon
        } ?: if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG, android.graphics.drawable.Icon::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG) as? android.graphics.drawable.Icon
        }

        val largeBitmap: android.graphics.Bitmap? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_LARGE_ICON, android.graphics.Bitmap::class.java)
                ?: extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG, android.graphics.Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            (extras.getParcelable(Notification.EXTRA_LARGE_ICON) as? android.graphics.Bitmap)
                ?: (extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG) as? android.graphics.Bitmap)
        } ?: sbn.notification.largeIcon

        val appIconDrawable: Drawable? = try {
            when {
                profileIcon != null -> profileIcon.loadDrawable(this)
                largeBitmap != null -> android.graphics.drawable.BitmapDrawable(resources, largeBitmap)
                else -> packageManager.getApplicationIcon(packageName)
            }
        } catch (e: Exception) {
            IslandLogger.d(TAG, "Icon fetch failed for $packageName", e)
            null
        }

        val notificationKey = sbn.key ?: "${sbn.packageName}_${sbn.id}_${sbn.postTime}"

        val isMessage = sbn.notification.category == Notification.CATEGORY_MESSAGE ||
                packageName in setOf(
                    "com.whatsapp",
                    "com.instagram.android",
                    "org.telegram.messenger",
                    "com.google.android.apps.messaging",
                    "com.android.mms",
                    "com.samsung.android.messaging"
                ) ||
                sbn.notification.extras.containsKey("android.messagingPerson")

        val notificationData = NotificationData(
            appName = realAppName,
            title = senderTitle,
            content = messageBody,
            packageName = packageName,
            appIcon = appIconDrawable,
            timestamp = sbn.postTime,
            contentIntent = sbn.notification.contentIntent,
            actions = sbn.notification.actions,
            notificationKey = notificationKey,
            isMessage = isMessage,
            isNotEmpty = true
        )

        IslandLogger.d(TAG, "Posting high-res notification from: $packageName", null)
        NotificationRepository.postNotification(notificationData)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // We no longer remove notifications from the island when the system removes them.
        // This allows the island to act as a persistent tray until the user manually clears it.
        IslandLogger.d(TAG, "System notification removed, keeping in Island: ${sbn.packageName}", null)
    }
}
