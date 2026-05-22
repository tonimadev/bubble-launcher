package digital.tonima.bubbleslauncher.notification

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor() {
    val notificationCounts: StateFlow<Map<String, Int>> = NotificationService.notificationCounts
}
