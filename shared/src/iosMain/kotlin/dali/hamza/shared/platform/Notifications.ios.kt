package dali.hamza.shared.platform

import dali.hamza.shared.common.nowMillis
import platform.Foundation.setValue
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/**
 * iOS actual — UNUserNotificationCenter. The authorization status is
 * queried asynchronously and cached: [areNotificationsEnabled] returns the
 * last known value (false until the first query completes) and refreshes
 * the cache for the next call.
 */
internal class LocalNotifierIos : LocalNotifier {

    // authorization status cache (best-effort; the OS query is async and
    // the worst case of a stale read is one redundant check)
    private var authorized: Boolean = false

    private var statusKnown: Boolean = false

    init {
        refreshState()
    }

    override fun areNotificationsEnabled(): Boolean {
        // Until the async OS query lands, assume allowed: a wrong "off"
        // banner is worse than a redundant check (posting without
        // permission is a silent no-op, and the add flow still prompts).
        if (!statusKnown) refreshState()
        return !statusKnown || authorized
    }

    override fun requestPermission(onResult: (Boolean) -> Unit) {
        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or
                    UNAuthorizationOptionBadge or
                    UNAuthorizationOptionSound
            ) { granted, _ ->
                authorized = granted
                statusKnown = true
                onResult(granted)
            }
    }

    override fun notify(id: Long, title: String, body: String) {
        val content = UNMutableNotificationContent()
        // K/N binds UNMutableNotificationContent's properties as read-only
        // (declared readonly on the UNNotificationContent superclass) —
        // set them through KVC instead
        content.setValue(title, forKey = "title")
        content.setValue(body, forKey = "body")
        content.setValue(UNNotificationSound.defaultSound, forKey = "sound")
        // unique id per post — repeating ids would replace an older alert
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = "rate-alert-$id-${nowMillis()}",
            content = content,
            trigger = null,
        )
        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request) { /* delivery errors are not actionable here */ }
    }

    private fun refreshState() {
        UNUserNotificationCenter.currentNotificationCenter()
            .getNotificationSettingsWithCompletionHandler { settings ->
                authorized = settings?.authorizationStatus == UNAuthorizationStatusAuthorized
                statusKnown = true
            }
    }
}

actual fun createLocalNotifier(): LocalNotifier = LocalNotifierIos()
