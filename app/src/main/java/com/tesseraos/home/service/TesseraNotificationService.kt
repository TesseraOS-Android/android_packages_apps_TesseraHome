package com.tesseraos.home.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.tesseraos.home.model.LiveNotificationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LiveTileBus {
    private val _notifications = MutableStateFlow<Map<String, LiveNotificationData>>(emptyMap())
    val notifications: StateFlow<Map<String, LiveNotificationData>> = _notifications.asStateFlow()

    fun update(map: Map<String, LiveNotificationData>) {
        _notifications.value = map
    }

    fun isListenerEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val flat = ComponentName(context, TesseraNotificationService::class.java).flattenToString()
        return enabled.contains(flat)
    }
}

class TesseraNotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshActiveNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshActiveNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshActiveNotifications()
    }

    private fun refreshActiveNotifications() {
        val active = try {
            activeNotifications
        } catch (_: Throwable) {
            null
        } ?: return

        val grouped = mutableMapOf<String, MutableList<StatusBarNotification>>()
        for (sbn in active) {
            if (sbn.isOngoing) continue
            val extras = sbn.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            if (title.isBlank() && text.isBlank()) continue
            grouped.getOrPut(sbn.packageName) { mutableListOf() }.add(sbn)
        }

        val result = mutableMapOf<String, LiveNotificationData>()
        for ((pkg, list) in grouped) {
            val latest = list.maxByOrNull { it.postTime } ?: continue
            val extras = latest.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            result[pkg] = LiveNotificationData(
                packageName = pkg,
                count = list.size,
                title = title,
                text = text,
                timestamp = latest.postTime
            )
        }
        LiveTileBus.update(result)
    }
}
