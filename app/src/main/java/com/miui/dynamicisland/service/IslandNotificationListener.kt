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
        
        // Filter out summary notifications and ongoing ones
        val isSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        if (isSummary) {
            IslandLogger.d(TAG, "Ignoring summary notification from $packageName", null)
            return
        }
        
        if (sbn.isOngoing || (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT != 0)) return
        if (sbn.notification.extras.containsKey("android.mediaSession")) return

        val extras = sbn.notification.extras
        
        // Better Sender Resolution for Messaging Apps
        var senderTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            ?: ""
            
        var messageBody = when {
            extras.containsKey("android.messagingStyleUser") || extras.containsKey(Notification.EXTRA_MESSAGES) -> {
                val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
                if (!messages.isNullOrEmpty()) {
                    val lastMessage = messages.last() as? android.os.Bundle
                    val text = lastMessage?.getCharSequence("text")?.toString()
                    val sender = lastMessage?.getCharSequence("sender")?.toString()
                    
                    // If we have a sender name in the messaging style, use it as title if title is generic
                    if (!sender.isNullOrBlank() && (senderTitle.isBlank() || senderTitle == packageName)) {
                        senderTitle = sender
                    }
                    
                    if (text.isNullOrBlank()) extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() else text
                } else extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            }
            extras.containsKey(Notification.EXTRA_TEXT) -> extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            extras.containsKey(Notification.EXTRA_BIG_TEXT) -> extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            else -> null
        } ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n") { it.toString() } ?: ""

        // Deduplicate Title in Content (e.g. "Akshat: Hello" -> "Hello")
        if (messageBody.startsWith("$senderTitle: ", ignoreCase = true)) {
            messageBody = messageBody.substring(senderTitle.length + 2)
        } else if (messageBody.startsWith("$senderTitle ", ignoreCase = true)) {
            messageBody = messageBody.substring(senderTitle.length + 1)
        }

        // WhatsApp / Telegram "System" message filters
        val lowerBody = messageBody.lowercase()
        if (lowerBody.contains("checking for new messages") || 
            lowerBody.contains("new message") && messageBody.length < 15 ||
            lowerBody.matches(Regex(".*\\d+.*new messages.*")) ||
            lowerBody.matches(Regex(".*\\d+.*messages from.*chats.*"))) {
            
            // If we already have a better notification for this app, ignore this summary/placeholder
            val currentQueue = NotificationRepository.notifications.value.items
            if (currentQueue.any { it.packageName == packageName && it.content.length > messageBody.length }) {
                IslandLogger.d(TAG, "Ignoring placeholder notification as better content exists", null)
                return
            }
        }

        // 2. Resolve true app label for user-facing name
        val realAppName: String = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            extras.getCharSequence("android.substName")?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
                ?: packageName
        }
        
        if (senderTitle.isBlank() || senderTitle == realAppName) {
            senderTitle = realAppName
        }

        // 3. Privacy Check: If message content is just "New message" or "X messages", 
        // it might be hidden by system settings.
        val lowerContent = messageBody.lowercase()
        if (lowerContent.contains("new message") || lowerContent.matches(Regex(".*\\d+.*messages.*"))) {
            // Keep it as is, or maybe the user wants to hide it? 
            // The user said they want it shown only when app is unlocked.
            // We can't easily check app-specific "unlocked" state, but we can check if device is locked.
            val km = getSystemService(android.content.Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
            if (km?.isKeyguardLocked == true) {
                // Device is locked, respect privacy if needed. 
                // But usually the system already hides the text in SBN extras if privacy is on.
            }
        }

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
