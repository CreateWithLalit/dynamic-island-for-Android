package com.miui.dynamicisland.data.repository

import android.graphics.drawable.Icon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationData(
    val appName: String,
    val title: String,
    val content: String,
    val packageName: String,
    val icon: Icon?,
    val timestamp: Long,
    val isNotEmpty: Boolean = false
) {
    companion object {
        val EMPTY = NotificationData(
            appName = "",
            title = "",
            content = "",
            packageName = "",
            icon = null,
            timestamp = 0L,
            isNotEmpty = false
        )
    }
}

object NotificationRepository {
    private val _notifications = MutableStateFlow(NotificationData.EMPTY)
    val notifications: StateFlow<NotificationData> = _notifications.asStateFlow()

    fun postNotification(data: NotificationData) {
        _notifications.value = data
    }
}
