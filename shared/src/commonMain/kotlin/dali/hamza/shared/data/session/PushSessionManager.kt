package dali.hamza.shared.data.session

import dali.hamza.shared.data.network.RateAlertsApi
import dali.hamza.shared.data.network.models.DeviceDataAPI
import dali.hamza.shared.data.storage.ISessionStorage
import dali.hamza.shared.platform.appVersionName
import dali.hamza.shared.platform.currentEpochMillis
import dali.hamza.shared.platform.isIos
import io.github.aakira.napier.Napier
import kotlin.random.Random

/**
 * SessionManager for server-push rate alerts (Phase 2 client wiring —
 * plans/server-push-alerts.md §5.1).
 *
 * Owns the app-launch registration loop:
 *
 *  1. `installId` — UUID minted once per install, persisted in
 *     [ISessionStorage] next to the other session keys.
 *  2. anonymous session JWT — `POST /auth/session {installId, platform}`
 *     (same call refreshes it). Cached and reused until it is within
 *     [SESSION_REFRESH_MARGIN_MS] of expiry, then re-minted on the next
 *     app open. `sub = "anon:<installId>"` stays stable across refreshes,
 *     so alerts and device registrations survive.
 *  3. device registration — `POST /alerts/devices` with the Bearer token
 *     (idempotent upsert; the server's one-device-one-session invariant
 *     makes re-posting safe and self-healing).
 *
 * Everything is best-effort: a failed call leaves the cached state intact
 * and the next app open retries.
 */
class PushSessionManager(
    private val alertsApi: RateAlertsApi,
    private val storage: ISessionStorage,
) {

    /**
     * Stable per-install identity, minted on first access and persisted.
     * Matches the server's `^[A-Za-z0-9_-]{8,64}$` installId pattern.
     */
    val installId: String
        get() = storage.getInstallId() ?: mintInstallId().also { storage.setInstallId(it) }

    /** Device platform reported to the backend (`ANDROID` / `IOS`). */
    fun platform(): String = if (isIos()) PLATFORM_IOS else PLATFORM_ANDROID

    /**
     * Valid Bearer token for the alerts endpoints, or `null` when no
     * usable session exists — offline first launch, server down, expired
     * cache. Cheap: returns the cached token unless it needs refreshing
     * (`expiresAt < now + 7d`, checked on every app open).
     */
    suspend fun ensureSession(): String? {
        val cached = storage.getSessionToken()
        val expiresAt = storage.getSessionExpiresAt()
        val now = currentEpochMillis()
        if (cached != null && expiresAt - now > SESSION_REFRESH_MARGIN_MS) {
            return cached
        }
        val minted = mintSession()
        if (minted != null) return minted
        // refresh failed — a not-yet-expired cached token still verifies
        // server-side; the next app open retries the refresh
        return cached?.takeIf { expiresAt > now }
    }

    /**
     * Step 3 of the launch loop — register (or re-register) this device's
     * push token under the current session. Called on every app launch
     * and from the FCM `onNewToken` rotation hook.
     *
     * Rotation hygiene: when the incoming token differs from the previous
     * one persisted in [ISessionStorage], the OLD token is unregistered
     * right after the new one registers — otherwise the dead row lingers
     * as "active" until a send fails or the 30-day hygiene pass removes it.
     *
     * @param pushToken FCM registration token (APNs device token as a hex
     *   string on iOS, Phase 3)
     * @param bundleId application id — APNs topic / build identity
     *   (`com.sovereignledger.app`, `.debug` variants)
     */
    suspend fun registerDevice(
        pushToken: String,
        bundleId: String,
        appVersion: String = appVersionName(),
    ): Result<Unit> {
        val bearer = ensureSession()
            ?: return Result.failure(IllegalStateException("No alert session — server unreachable or session mint failed"))
        val result = alertsApi.registerDevice(
            bearerToken = bearer,
            platform = platform(),
            token = pushToken,
            bundleId = bundleId,
            appVersion = appVersion,
        )
        result.onSuccess {
            val previous = storage.getLastPushToken()
            if (previous != null && previous != pushToken) {
                // best-effort: the old row dies on the next send anyway
                alertsApi.unregisterDevice(bearerToken = bearer, token = previous)
                Napier.i(tag = TAG) { "previous push token unregistered (rotation)" }
            }
            storage.setLastPushToken(pushToken)
            Napier.i(tag = TAG) { "device registered for push alerts (${platform()})" }
        }.onFailure { error ->
            Napier.w(tag = TAG, throwable = error) { "device registration failed" }
        }
        return result
    }

    /**
     * Explicit sign-out / opt-out: retire the current token (server marks
     * the row inactive — no more pushes until a future launch re-registers).
     */
    suspend fun unregisterDevice(pushToken: String? = null): Result<Unit> {
        val token = pushToken ?: storage.getLastPushToken()
            ?: return Result.failure(IllegalStateException("No push token registered on this install"))
        val bearer = ensureSession() ?: return Result.failure(IllegalStateException("No alert session"))
        return alertsApi.unregisterDevice(bearerToken = bearer, token = token)
            .onSuccess { Napier.i(tag = TAG) { "push token unregistered" } }
            .onFailure { Napier.w(tag = TAG) { "push token unregister failed" } }
    }

    /**
     * Per-device pause — backs the Account "Push Notifications" preference:
     * `paused = true` silences this device's pushes server-side (indefinite,
     * enforced server-max as one year) without unregistering the token or
     * touching other devices; `false` resumes. Best-effort — the local
     * engine's own preference gate remains the primary control.
     */
    suspend fun setDevicePaused(paused: Boolean) {
        val bearer = ensureSession() ?: return
        runCatching {
            val own = alertsApi.listDevices(bearerToken = bearer)
                .getOrDefault(emptyList())
                .firstOrNull { it.platform == platform() }
                ?: return
            val until = if (paused) currentEpochMillis() + PAUSE_INDEFINITE_MS else 0L
            alertsApi.setDevicePaused(bearerToken = bearer, deviceId = own.id, pausedUntil = until)
        }.onSuccess {
            Napier.i(tag = TAG) { "device ${if (paused) "paused" else "resumed"} server-side" }
        }.onFailure {
            Napier.w(tag = TAG) { "device pause sync failed (server unreachable — local gate still applies)" }
        }
    }

    /** `POST /auth/session` and cache the result. Null on failure. */
    private suspend fun mintSession(): String? =
        alertsApi.createSession(
            installId = installId,
            platform = platform(),
            appVersion = appVersionName(),
        ).fold(
            onSuccess = { response ->
                storage.setSessionToken(response.sessionToken)
                storage.setSessionExpiresAt(response.expiresAt)
                Napier.i(tag = TAG) { "anonymous session minted (expires in ${(response.expiresAt - currentEpochMillis()) / 3_600_000}h)" }
                response.sessionToken
            },
            onFailure = { error ->
                Napier.w(tag = TAG, throwable = error) { "session mint request failed" }
                null
            },
        )

    companion object {
        private const val TAG = "PushSession"

        /** Refresh the JWT when less than 7 days remain (plan §3.1). */
        const val SESSION_REFRESH_MARGIN_MS = 7L * 24 * 60 * 60 * 1000L

        /** "Indefinite" pause — the server enforces a 1-year maximum. */
        const val PAUSE_INDEFINITE_MS = 364L * 24 * 60 * 60 * 1000L

        const val PLATFORM_ANDROID = "ANDROID"
        const val PLATFORM_IOS = "IOS"

        /**
         * RFC-4122 v4 UUID from `kotlin.random` (no platform UUID API in
         * common code). Hex + dashes → always matches the server's
         * installId pattern.
         */
        internal fun mintInstallId(): String {
            val bytes = Random.Default.nextBytes(16)
            bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte() // version 4
            bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte() // IETF variant
            val hex = bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
            return buildString {
                append(hex.substring(0, 8)); append('-')
                append(hex.substring(8, 12)); append('-')
                append(hex.substring(12, 16)); append('-')
                append(hex.substring(16, 20)); append('-')
                append(hex.substring(20, 32))
            }
        }
    }
}
