package digital.tonima.bubbleslauncher.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateCounts()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateCounts()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateCounts()
    }

    private fun updateCounts() {
        try {
            val notifications = activeNotifications ?: return
            val counts = mutableMapOf<String, Int>()
            for (sbn in notifications) {
                if (!sbn.isOngoing) { // Skip persistent/ongoing notifications
                    val pkg = sbn.packageName
                    counts[pkg] = (counts[pkg] ?: 0) + 1
                }
            }
            _notificationCounts.value = counts
        } catch (_: Exception) {
            // Service might not be connected yet
        }
    }

    companion object {
        private val _notificationCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
        val notificationCounts: StateFlow<Map<String, Int>> = _notificationCounts.asStateFlow()
    }
}
