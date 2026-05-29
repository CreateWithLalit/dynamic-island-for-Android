package com.miui.dynamicisland.data.repository

import android.app.Notification
import android.app.PendingIntent
import android.graphics.drawable.Drawable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationData(
    val appName: String,
    val title: String,
    val content: String,
    val packageName: String,
    val appIcon: Drawable?,
    val timestamp: Long,
    val contentIntent: PendingIntent? = null,
    val actions: Array<Notification.Action>? = null,
    val notificationKey: String = "",
    val isMessage: Boolean = false,
    val isNotEmpty: Boolean = false
) {
    companion object {
        val EMPTY = NotificationData(
            appName = "",
            title = "",
            content = "",
            packageName = "",
            appIcon = null,
            timestamp = 0L,
            contentIntent = null,
            actions = null,
            notificationKey = "",
            isMessage = false,
            isNotEmpty = false
        )
    }
}

object NotificationRepository {
    private const val MAX_QUEUE_SIZE = 8

    data class NotificationQueueState(
        val items: List<NotificationData> = emptyList(),
        val index: Int = 0
    ) {
        val isNotEmpty: Boolean get() = items.isNotEmpty()
        val safeIndex: Int get() = index.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        val current: NotificationData?
            get() = items.getOrNull(safeIndex)
    }

    private val _notifications = MutableStateFlow(NotificationQueueState())
    val notifications: StateFlow<NotificationQueueState> = _notifications.asStateFlow()

    fun postNotification(data: NotificationData) {
        if (!data.isNotEmpty) {
            clearAll()
            return
        }
        val current = _notifications.value
        val updated = listOf(data) + current.items
            .filterNot { it.packageName == data.packageName && it.timestamp == data.timestamp }
        _notifications.value = current.copy(
            items = updated.take(MAX_QUEUE_SIZE),
            index = 0
        )
    }

    fun clearAll() {
        _notifications.value = NotificationQueueState()
    }

    fun removeByKey(key: String) {
        val current = _notifications.value
        val newItems = current.items.filterNot { it.notificationKey == key }
        if (newItems.size != current.items.size) {
            val newIndex = current.index.coerceAtMost((newItems.size - 1).coerceAtLeast(0))
            _notifications.value = current.copy(items = newItems, index = newIndex)
        }
    }

    fun deleteCurrent() {
        val current = _notifications.value
        if (current.items.isEmpty()) return
        val newItems = current.items.toMutableList().also { it.removeAt(current.safeIndex) }
        val newIndex = current.safeIndex.coerceAtMost((newItems.size - 1).coerceAtLeast(0))
        _notifications.value = current.copy(items = newItems, index = newIndex)
    }

    fun markCurrentRead() {
        deleteCurrent()
    }

    fun navigateNext() {
        val current = _notifications.value
        if (current.items.size <= 1) return
        val newIndex = (current.safeIndex + 1) % current.items.size
        _notifications.value = current.copy(index = newIndex)
    }

    fun navigatePrevious() {
        val current = _notifications.value
        if (current.items.size <= 1) return
        val newIndex = (current.safeIndex - 1 + current.items.size) % current.items.size
        _notifications.value = current.copy(index = newIndex)
    }
}
