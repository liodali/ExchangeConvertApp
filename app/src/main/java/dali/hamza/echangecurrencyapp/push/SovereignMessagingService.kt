package dali.hamza.echangecurrencyapp.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dali.hamza.echangecurrencyapp.BuildConfig
import dali.hamza.shared.platform.LocalNotifier
import io.github.aakira.napier.Napier
import org.koin.core.context.GlobalContext

/**
 * FCM entry point — server-push rate alerts (plans/server-push-alerts.md).
 *
 * The backend sends data messages (title/body + alert metadata); they are
 * rendered through the shared [LocalNotifier] on the existing `rate_alerts`
 * channel, so push notifications look identical to the local engine's.
 *
 * Token lifecycle: [onNewToken] re-registers with the backend immediately
 * (session ensure → POST /alerts/devices) so pushes keep flowing to the
 * rotated token — Firebase calls this exactly when the token changes.
 *
 * The whole class is inert when no `google-services.json` is present: FCM
 * never delivers to an app without a FirebaseApp, and the manifest entry
 * alone does not initialize anything.
 */
class SovereignMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // full token only in debug builds — it identifies this install
        val printable = if (BuildConfig.DEBUG) token else "${token.take(12)}…"
        Napier.i(tag = TAG) { "FCM token rotated: $printable" }
        PushRegistration.registerToken(this, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // data payload wins (server-rendered copy); fall back to the
        // notification payload for console test pushes
        val data = message.data
        val title = data["title"] ?: message.notification?.title
        val body = data["body"] ?: message.notification?.body
        if (title == null || body == null) {
            Napier.w(tag = TAG) { "FCM message without title/body — ignored (${message.messageId})" }
            return
        }
        Napier.i(tag = TAG) { "FCM alert push: $title" }
        val alertId = data["alertId"]?.toLongOrNull() ?: System.currentTimeMillis()
        runCatching {
            GlobalContext.get().get<LocalNotifier>().notify(alertId, title, body)
        }.onFailure { Napier.e(throwable = it, tag = TAG) { "posting notification failed" } }
    }

    private companion object {
        const val TAG = "SovereignFCM"
    }
}
