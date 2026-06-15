package com.miui.dynamicisland.service

import android.app.Notification
import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.miui.dynamicisland.data.repository.BluetoothBatteryNotificationParser
import com.miui.dynamicisland.data.repository.MediaRepositoryBridge
import com.miui.dynamicisland.data.repository.NotificationData
import com.miui.dynamicisland.data.repository.NotificationRepository
import com.miui.dynamicisland.util.IconUtils
import com.miui.dynamicisland.util.IslandLogger

class IslandNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "IslandNotificationListener"
        @Volatile internal var instance: IslandNotificationListener? = null

        fun isServiceConnected(): Boolean = instance != null

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
        "com.android.settings"
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
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString(" | ") { it.toString() }
            .orEmpty()

        IslandLogger.d(TAG, """
            [DEBUG_BT] Received Notification:
            - Package: $packageName
            - Title: $title
            - Text: $text
            - BigText: $bigText
            - Lines: $lines
        """.trimIndent(), null)

        val parserResult = BluetoothBatteryNotificationParser.tryUpdateFromNotification(sbn)
        IslandLogger.d(TAG, "[DEBUG_BT] Parser executed for $packageName. Result: $parserResult", null)

        if (parserResult) {
            IslandLogger.d(TAG, "[FLOW] Bluetooth battery update handled by parser, skipping normal notification", null)
            return
        }

        val isBluetoothSpecial = title.contains("Buds", ignoreCase = true) || 
                                 text.contains("connecting", ignoreCase = true) ||
                                 text.contains("pair", ignoreCase = true) ||
                                 text.contains("connected", ignoreCase = true) ||
                                 title.contains("Realme", ignoreCase = true) ||
                                 packageName.contains("bluetooth", ignoreCase = true) ||
                                 packageName == "com.google.android.gms"

        val isNavigationSpecial = packageName == "com.google.android.apps.maps" || 
                                 sbn.notification.category == Notification.CATEGORY_NAVIGATION

        // 1. Refined Blacklist & System Noise Filter
        if (packageName in ignoredPackages && !isBluetoothSpecial && !isNavigationSpecial) {
            IslandLogger.d(TAG, "[FLOW] Notification from $packageName ignored (blacklisted)", null)
            return
        }
        
        // Filter out summary notifications and ongoing ones
        val isSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        if (isSummary) {
            IslandLogger.d(TAG, "Ignoring summary notification from $packageName", null)
            return
        }
        
        if (!isBluetoothSpecial && !isNavigationSpecial) {
            if (sbn.isOngoing || (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT != 0)) return
            if (sbn.notification.extras.containsKey("android.mediaSession")) return
        }

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
                ?: extras.getParcelable(Notification.EXTRA_PICTURE, android.graphics.Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            (extras.getParcelable(Notification.EXTRA_LARGE_ICON) as? android.graphics.Bitmap)
                ?: (extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG) as? android.graphics.Bitmap)
                ?: (extras.getParcelable(Notification.EXTRA_PICTURE) as? android.graphics.Bitmap)
        } ?: sbn.notification.largeIcon

        val largeIconDrawable: Drawable? = try {
            when {
                profileIcon != null -> profileIcon.loadDrawable(this)
                largeBitmap != null -> android.graphics.drawable.BitmapDrawable(resources, largeBitmap)
                else -> null
            }
        } catch (e: Exception) {
            null
        }

        val appIconDrawable: Drawable? = try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            IslandLogger.d(TAG, "App icon fetch failed for $packageName", e)
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
            largeIcon = largeIconDrawable,
            timestamp = sbn.postTime,
            contentIntent = sbn.notification.contentIntent,
            actions = sbn.notification.actions,
            notificationKey = notificationKey,
            isMessage = isMessage,
            isNotEmpty = true
        )

        IslandLogger.d(TAG, "Posting high-res notification from: $packageName", null)
        
        // 4. Special Case: Google Maps Navigation
        if (isNavigationSpecial) {
            val direction = parseDirection(title, text)
            if (direction != com.miui.dynamicisland.ui.states.IslandState.Navigation.Direction.UNKNOWN) {
                val distance = extractDistance(title) ?: extractDistance(text) ?: ""
                val street = extractStreet(title) ?: extractStreet(text) ?: text
                
                // Try to parse "Then" instruction
                val nextDir = parseNextDirection(text) ?: parseNextDirection(bigText)
                
                val mapBitmap = (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    extras.getParcelable(Notification.EXTRA_PICTURE, android.graphics.Bitmap::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    extras.getParcelable(Notification.EXTRA_PICTURE) as? android.graphics.Bitmap
                }) ?: largeBitmap ?: largeIconDrawable?.let {
                    IconUtils.drawableToBitmap(it, 320, 320)
                }

                com.miui.dynamicisland.manager.IslandStateManager.getInstance().pushState(
                    com.miui.dynamicisland.ui.states.IslandState.Navigation(
                        direction = direction,
                        distance = distance,
                        instruction = title,
                        street = street,
                        toward = extractToward(text) ?: "",
                        nextDirection = nextDir,
                        appIcon = appIconDrawable,
                        isUrgent = distance.contains("m") && (distance.filter { it.isDigit() }.toIntOrNull() ?: 500) < 150,
                        mapSnippet = mapBitmap,
                        packageName = packageName
                    )
                )
            }
        }

        NotificationRepository.postNotification(notificationData)

        // 5. Special Case: Progress (Downloads/Uploads)
        val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val progressCurrent = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val isIndeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)

        if ((progressMax > 0 || packageName == "com.android.vending") && !isIndeterminate) {
            val progressFraction = if (progressMax > 0) progressCurrent.toFloat() / progressMax else {
                parseProgressFromText(text) ?: parseProgressFromText(title) ?: 0f
            }
            
            if (progressFraction > 0f && progressFraction < 1f) {
                val remainingTime = extractRemainingTime(text) ?: extractRemainingTime(title) ?: ""
                com.miui.dynamicisland.manager.IslandStateManager.getInstance().pushState(
                    com.miui.dynamicisland.ui.states.IslandState.Progress(
                        appName = realAppName,
                        title = if (title.isNotBlank()) title else realAppName,
                        progress = progressFraction,
                        remainingTime = remainingTime,
                        isDownload = !packageName.contains("drive") && !packageName.contains("upload"),
                        packageName = packageName
                    )
                )
            } else if (progressFraction >= 1f || text.lowercase().contains("complete") || text.lowercase().contains("downloaded")) {
                com.miui.dynamicisland.manager.IslandStateManager.getInstance().removeState(com.miui.dynamicisland.ui.states.IslandState.Progress::class.java)
            }
        }
    }

    private fun parseProgressFromText(input: String): Float? {
        val regex = Regex("(\\d+)%")
        return regex.find(input)?.groupValues?.get(1)?.toFloatOrNull()?.div(100f)
    }

    private fun extractRemainingTime(input: String): String? {
        val regex = Regex("(\\d+\\s*(min|sec|hr|h|m|s)\\s*(remaining|left))", RegexOption.IGNORE_CASE)
        return regex.find(input)?.value ?: Regex("(\\d+:\\d+\\s*(left|remaining))").find(input)?.value
    }

    private fun parseDirection(title: String, text: String): com.miui.dynamicisland.ui.states.IslandState.Navigation.Direction {
        val combined = "$title $text".lowercase()
        return when {
            combined.contains("slight left") -> com.miui.dynamicisland.ui.states.IslandState.Navigation.Direction.SLIGHT_LEFT
            combined.contains("slight right") -> com.miui.dynamicisland.ui.states.IslandState.Navigation.Direction.SLIGHT_RIGHT
            combined.contains("left") -> com.miui.dynamicisland.ui.states.IslandState.Navigation.Direction.LEFT
            combined.contains("right") -> com.miui.dynamicisland.ui.states.IslandState.Navigation.Direction.RIGHT
            combined.contains("u-turn") -> com.miui.dynamicisland.ui.states.IslandState.Navigation.Direction.U_TURN
            combined.contains("merge") -> com.miui.dynamicisland.ui.states.IslandState.Navigation.Direction.MERGE
            combined.contains("exit") -> com.miui.dynamicisland.ui.states.IslandState.Navigation.Direction.EXIT
            combined.contains("arrive") || combined.contains("destination") -> com.miui.dynamicisland.ui.states.IslandState.Navigation.Direction.ARRIVE
            combined.contains("straight") || combined.contains("keep") || combined.contains("ahead") || combined.contains("onto") -> com.miui.dynamicisland.ui.states.IslandState.Navigation.Direction.STRAIGHT
            else -> com.miui.dynamicisland.ui.states.IslandState.Navigation.Direction.UNKNOWN
        }
    }

    private fun parseNextDirection(text: String): com.miui.dynamicisland.ui.states.IslandState.Navigation.Direction? {
        val lower = text.lowercase()
        val thenIndex = lower.indexOf("then")
        if (thenIndex == -1) return null
        val thenText = lower.substring(thenIndex)
        return when {
            thenText.contains("slight left") -> com.miui.dynamicisland.ui.states.IslandState.Navigation.Direction.SLIGHT_LEFT
            thenText.contains("slight right") -> com.miui.dynamicisland.ui.states.IslandState.Navigation.Direction.SLIGHT_RIGHT
            thenText.contains("left") -> com.miui.dynamicisland.ui.states.IslandState.Navigation.Direction.LEFT
            thenText.contains("right") -> com.miui.dynamicisland.ui.states.IslandState.Navigation.Direction.RIGHT
            thenText.contains("u-turn") -> com.miui.dynamicisland.ui.states.IslandState.Navigation.Direction.U_TURN
            thenText.contains("straight") -> com.miui.dynamicisland.ui.states.IslandState.Navigation.Direction.STRAIGHT
            else -> null
        }
    }

    private fun extractDistance(input: String): String? {
        val regex = Regex("(\\d+[.,]?\\d*\\s*(m|km|ft|mi))", RegexOption.IGNORE_CASE)
        return regex.find(input)?.value
    }

    private fun extractStreet(input: String): String? {
        if (input.isBlank()) return null
        
        // Case: "Street Name • toward ..."
        if (input.contains("•")) {
            return input.split("•")[0].trim()
        }
        
        // Case: "Street Name toward ..."
        if (input.contains(" toward ", ignoreCase = true)) {
            val parts = input.split(Regex(" toward ", RegexOption.IGNORE_CASE))
            if (parts[0].isNotBlank()) return parts[0].trim()
        }

        // Regex: "onto (.+)" or "toward (.+)"
        val ontoRegex = Regex("onto\\s+(.+)", RegexOption.IGNORE_CASE)
        ontoRegex.find(input)?.groupValues?.get(1)?.let { return it.trim() }
        
        return null
    }

    private fun extractToward(input: String): String? {
        val towardRegex = Regex("toward\\s+(.+)", RegexOption.IGNORE_CASE)
        return towardRegex.find(input)?.groupValues?.get(1)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == "com.google.android.apps.maps" || sbn.notification.category == Notification.CATEGORY_NAVIGATION) {
            com.miui.dynamicisland.manager.IslandStateManager.getInstance().removeState(com.miui.dynamicisland.ui.states.IslandState.Navigation::class.java)
        }

        // We no longer remove notifications from the island when the system removes them.
        // This allows the island to act as a persistent tray until the user manually clears it.
        IslandLogger.d(TAG, "System notification removed, keeping in Island: ${sbn.packageName}", null)
    }
}
