package dali.hamza.shared.platform

import dali.hamza.shared.data.session.PushSessionManager
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

/**
 * Swift entry points for the push registration loop (Phase 3 — the iOS
 * twin of Android's `app/push/PushRegistration.kt`).
 *
 * The AppDelegate forwards the raw APNs device token (hex) here after
 * `registerForRemoteNotifications()`; the shared [PushSessionManager]
 * ensures the anonymous session and upserts the token via
 * `POST /alerts/devices` (platform IOS, bundleId as the APNs topic —
 * `.debug` builds register their own bundle id).
 */
fun registerPushToken(tokenHex: String, bundleId: String, onDone: (Boolean) -> Unit = {}) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    scope.launch {
        val manager = runCatching {
            KoinPlatform.getKoin()?.get<PushSessionManager>()
        }.getOrNull()
        if (manager == null) {
            Napier.w(tag = TAG) { "PushSessionManager not in Koin — token registration skipped" }
            onDone(false)
            return@launch
        }
        val result = runCatching {
            manager.registerDevice(pushToken = tokenHex, bundleId = bundleId)
        }.onFailure { error ->
            Napier.w(tag = TAG, throwable = error) { "APNs token registration dispatch failed" }
        }
        onDone(result.getOrNull()?.isSuccess == true)
    }
}

private const val TAG = "PushSession"
