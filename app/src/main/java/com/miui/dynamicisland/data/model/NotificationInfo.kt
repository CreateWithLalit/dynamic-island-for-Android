// File: app/src/main/java/com/miui/dynamicisland/data/model/NotificationInfo.kt
// Purpose: Incoming notification data (title, content, app, icon, etc.)

package com.miui.dynamicisland.data.model

import android.graphics.drawable.Drawable
import androidx.annotation.Keep

@Keep
data class NotificationInfo(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val icon: Drawable? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isClearable: Boolean = true,
    val notificationType: NotificationType = NotificationType.OTHER,
    val priority: Int = 0,
    val subText: String? = null,
    val bigContent: String? = null,
    val summaryText: String? = null,
    val actions: List<NotificationAction> = emptyList(),
    val isOngoing: Boolean = false,
    val isGroupSummary: Boolean = false,
    val groupKey: String? = null,
    val channelId: String? = null,
    val badgeIcon: Drawable? = null,
    val largeIcon: Drawable? = null
) {

    enum class NotificationType {
        MESSAGE, EMAIL, SOCIAL, CALL, MEDIA, ALERT, SYSTEM, PAYMENT, OTHER
    }

    data class NotificationAction(
        val label: String,
        val icon: Drawable? = null,
        val actionId: Int
    )

    val isNotEmpty: Boolean = title.isNotBlank() || content.isNotBlank() || appName.isNotBlank()

    val isHighPriority: Boolean =
        notificationType == NotificationType.MESSAGE ||
                notificationType == NotificationType.CALL ||
                priority >= 4

    val formattedTimestamp: String
        get() {
            val diff = System.currentTimeMillis() - timestamp
            return when {
                diff < 60_000L -> "Just now"
                diff < 3_600_000L -> "${diff / 60_000L} min ago"
                diff < 86_400_000L -> "${diff / 3_600_000L} hr ago"
                else -> "Yesterday"
            }
        }

    // Apple HIG ke hisaab se notification type ke color
    fun getAccentColor(): Int {
        return when (notificationType) {
            NotificationType.MESSAGE -> 0xFF30D158.toInt()   // Green
            NotificationType.EMAIL -> 0xFF0A84FF.toInt()     // Blue
            NotificationType.SOCIAL -> 0xFFBF5AF2.toInt()    // Purple
            NotificationType.CALL -> 0xFF30D158.toInt()      // Green
            NotificationType.MEDIA -> 0xFFFF2D55.toInt()     // Red/Pink
            NotificationType.ALERT -> 0xFFFF9F0A.toInt()     // Orange
            NotificationType.SYSTEM -> 0xFF8E8E93.toInt()    // Gray
            NotificationType.PAYMENT -> 0xFF30D158.toInt()   // Green
            else -> 0xFF8E8E93.toInt()
        }
    }

    fun getShortTitle(maxLength: Int = 20): String = title.truncate(maxLength)
    fun getShortContent(maxLength: Int = 60): String = (bigContent ?: content).truncate(maxLength)

    companion object {
        fun create(
            id: String,
            packageName: String,
            appName: String,
            title: String,
            content: String,
            icon: Drawable? = null,
            timestamp: Long = System.currentTimeMillis()
        ): NotificationInfo {
            return NotificationInfo(
                id = id,
                packageName = packageName,
                appName = appName,
                title = title,
                content = content,
                icon = icon,
                timestamp = timestamp,
                notificationType = inferTypeFromPackage(packageName)
            )
        }

        private fun inferTypeFromPackage(packageName: String): NotificationType {
            val normalized = packageName.lowercase()
            return when {
                normalized.contains("whatsapp") ||
                        normalized.contains("messenger") ||
                        normalized.contains("sms") ||
                        normalized.contains("telegram") -> NotificationType.MESSAGE

                normalized.contains("gmail") ||
                        normalized.contains("mail") ||
                        normalized.contains("outlook") -> NotificationType.EMAIL

                normalized.contains("instagram") ||
                        normalized.contains("twitter") ||
                        normalized.contains("facebook") ||
                        normalized.contains("x.") -> NotificationType.SOCIAL

                normalized.contains("phone") ||
                        normalized.contains("dialer") ||
                        normalized.contains("call") -> NotificationType.CALL

                normalized.contains("music") ||
                        normalized.contains("spotify") ||
                        normalized.contains("youtube") -> NotificationType.MEDIA

                normalized.contains("alarm") ||
                        normalized.contains("clock") ||
                        normalized.contains("calendar") -> NotificationType.ALERT

                normalized.contains("android") ||
                        normalized.contains("systemui") ||
                        normalized.contains("settings") -> NotificationType.SYSTEM

                normalized.contains("paytm") ||
                        normalized.contains("phonepe") ||
                        normalized.contains("gpay") ||
                        normalized.contains("upi") -> NotificationType.PAYMENT

                else -> NotificationType.OTHER
            }
        }
    }
}

private fun String.truncate(maxLength: Int): String {
    if (maxLength <= 0) return ""
    if (length <= maxLength) return this
    if (maxLength <= 3) return take(maxLength)
    return take(maxLength - 3) + "..."
}