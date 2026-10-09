package dali.hamza.shared.platform

/**
 * Local notification poster (no push server involved).
 *
 * Rate alerts are delivered entirely on-device: the engine fetches fresh
 * rates in the background and posts through this abstraction.
 */
interface LocalNotifier {
    /**
     * Whether the app may currently show notifications (permission granted
     * + channel/app-level toggle). Cheap, synchronous, possibly cached.
     */
    fun areNotificationsEnabled(): Boolean

    /**
     * Ask the OS for notification permission (Android 13+ runtime
     * permission / iOS authorization request). The result is delivered on
     * the main thread; `false` when no host activity is available to
     * prompt from (Android) or the request fails.
     */
    fun requestPermission(onResult: (Boolean) -> Unit)

    /**
     * Make the platform ready to show notifications before any [notify]
     * call — Android: create the channel eagerly so system-trayed pushes
     * (FCM notification payloads) land on the right channel instead of the
     * FCM fallback. Safe to call at app start; no-ops where unnecessary.
     */
    fun prepare() {}

    /**
     * Post a notification immediately. Implementations must be safe to
     * call from a background worker and silently no-op without permission.
     */
    fun notify(id: Long, title: String, body: String)
}

expect fun createLocalNotifier(): LocalNotifier
