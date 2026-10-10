package dali.hamza.echangecurrencyapp.push

import android.content.Context
import dali.hamza.shared.data.session.PushSessionManager
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * App-launch push registration loop (Phase 2 client wiring —
 * plans/server-push-alerts.md §5.2):
 *
 *  1. `PushSessionManager.ensureSession()` — installId → anon JWT
 *     (cached; refreshed when < 7d to expiry) — runs implicitly inside
 *     [PushSessionManager.registerDevice].
 *  2. FCM token — fetched by the caller ([ExchangeApplication] on every
 *     launch, or Firebase via `onNewToken` when the token rotates).
 *  3. `POST /alerts/devices` — idempotent upsert under the session.
 *
 * Re-posting on every launch is the point: the server's one-device-one-
 * session invariant (UNIQUE(token)) makes the loop self-healing — stale
 * sessions, restored backups and staging/prod switches all repair
 * themselves on the next app open.
 *
 * Inert without `google-services.json` (no FirebaseApp → no token), and
 * best-effort when offline — the next launch retries.
 */
object PushRegistration {

    private const val TAG = "SovereignFCM"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Register a freshly delivered FCM token. Called from
     * [dali.hamza.echangecurrencyapp.ExchangeApplication]'s token fetch
     * and [SovereignMessagingService.onNewToken] (rotation hook).
     */
    fun registerToken(context: Context, token: String) {
        scope.launch {
            runCatching {
                GlobalContext.get().get<PushSessionManager>().registerDevice(
                    pushToken = token,
                    bundleId = context.packageName,
                )
            }.onFailure { error ->
                Napier.w(tag = TAG, throwable = error) { "push registration dispatch failed" }
            }
        }
    }
}
