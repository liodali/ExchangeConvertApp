package dali.hamza.shared.data.session

import dali.hamza.shared.data.network.AlertsApiError
import dali.hamza.shared.data.network.RateAlertsApi
import dali.hamza.shared.data.storage.ISessionStorage
import dali.hamza.shared.platform.appVersionName
import dali.hamza.shared.platform.currentEpochMillis
import dali.hamza.shared.platform.isIos
import io.github.aakira.napier.Napier
import kotlin.random.Random
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    /** Serializes minting so concurrent callers share one `/auth/session`. */
    private val sessionMutex = Mutex()

    /** In-memory mint-failure backoff (epoch ms, 0 = no recent failure). */
    private var lastMintFailureAt = 0L

    /**
     * Stable per-install identity, minted once per install and persisted.
     * Matches the server's `^[A-Za-z0-9_-]{8,64}$` installId pattern.
     * `lazy` is thread-safe: concurrent first callers cannot mint two
     * identities (one logical anonymous principal per installation).
     */
    val installId: String by lazy {
        storage.getInstallId() ?: mintInstallId().also { storage.setInstallId(it) }
    }

    /** Device platform reported to the backend (`ANDROID` / `IOS`). */
    fun platform(): String = if (isIos()) PLATFORM_IOS else PLATFORM_ANDROID

    /**
     * Valid Bearer token for the backend, or `null` when no usable session
     * exists — offline first launch, server down, expired cache. Cheap:
     * returns the cached token unless it needs refreshing (`expiresAt <
     * now + 7d`, checked on every app open). Minting is single-flight:
     * concurrent callers share one `/auth/session` request.
     */
    suspend fun ensureSession(): String? {
        usableCachedSession()?.let { return it }
        return sessionMutex.withLock {
            // lost the mint race — the winner refreshed the cache
            usableCachedSession()?.let { return@withLock it }
            mintSession() ?: unexpiredCachedSession()
        }
    }

    /**
     * Force a re-mint with the SAME [installId] — the stale-JWT recovery
     * path (plans/anonymous-session-lifecycle.md §3). Bypasses the cached
     * token and the failure backoff; the cache is only replaced after a
     * successful mint, so a failed refresh never destroys local state.
     *
     * Returns null when the re-mint fails — callers then surface the
     * original request failure instead of retrying with the bad token.
     */
    suspend fun refreshSession(): String? = sessionMutex.withLock { mintSession(force = true) }

    /**
     * Run one authenticated operation under the anonymous session, healing
     * a stale cached JWT exactly once: on a `401`, re-mint with the same
     * [installId] and retry the original call. The second rejection wins —
     * no loop, no new installId, and the JWT `sub` (hence alert/device
     * ownership) never changes.
     */
    suspend fun <T> withSession(
        block: suspend (bearerToken: String) -> Result<T>,
    ): Result<T> {
        val bearer = ensureSession()
            ?: return Result.failure(IllegalStateException(NO_SESSION_MESSAGE))
        val first = block(bearer)
        if (!first.isAuthRejection()) return first
        Napier.i(tag = TAG) { "session rejected — re-minting and retrying once" }
        val refreshed = refreshSession()
        return if (refreshed != null) block(refreshed) else first
    }

    /** `401` from an authed alert call — the signal to re-mint once. */
    private fun <T> Result<T>.isAuthRejection(): Boolean =
        (exceptionOrNull() as? AlertsApiError)?.status == 401

    /** Cached JWT still comfortably valid (outside the refresh margin). */
    private fun usableCachedSession(): String? {
        val cached = storage.getSessionToken() ?: return null
        val expiresAt = storage.getSessionExpiresAt()
        return cached.takeIf { expiresAt - currentEpochMillis() > SESSION_REFRESH_MARGIN_MS }
    }

    /** Cached JWT not yet expired (may be inside the refresh margin). */
    private fun unexpiredCachedSession(): String? {
        val cached = storage.getSessionToken() ?: return null
        return cached.takeIf { storage.getSessionExpiresAt() > currentEpochMillis() }
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
    ): Result<Unit> = withSession { bearer ->
        alertsApi.registerDevice(
            bearerToken = bearer,
            platform = platform(),
            token = pushToken,
            bundleId = bundleId,
            appVersion = appVersion,
        ).onSuccess {
            val previous = storage.getLastPushToken()
            if (previous != null && previous != pushToken) {
                // best-effort: the old row dies on the next send anyway
                alertsApi.unregisterDevice(bearerToken = bearer, token = previous)
                Napier.i(tag = TAG) { "previous push token unregistered (rotation)" }
            }
            storage.setLastPushToken(pushToken)
            Napier.i(tag = TAG) { "device registered for push alerts (${platform()})" }
        }
    }.onFailure { error ->
        Napier.w(tag = TAG, throwable = error) { "device registration failed" }
    }

    /**
     * Explicit sign-out / opt-out: retire the current token (server marks
     * the row inactive — no more pushes until a future launch re-registers).
     */
    suspend fun unregisterDevice(pushToken: String? = null): Result<Unit> {
        val token = pushToken ?: storage.getLastPushToken()
            ?: return Result.failure(IllegalStateException("No push token registered on this install"))
        return withSession { bearer -> alertsApi.unregisterDevice(bearerToken = bearer, token = token) }
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
        val outcome = withSession { bearer ->
            runCatching {
                val own = alertsApi.listDevices(bearerToken = bearer)
                    .getOrThrow()
                    .firstOrNull { it.platform == platform() }
                    ?: return@runCatching
                val until = if (paused) currentEpochMillis() + PAUSE_INDEFINITE_MS else 0L
                alertsApi.setDevicePaused(bearerToken = bearer, deviceId = own.id, pausedUntil = until)
                Unit
            }
        }
        outcome.onSuccess {
            Napier.i(tag = TAG) { "device ${if (paused) "paused" else "resumed"} server-side" }
        }.onFailure {
            Napier.w(tag = TAG) { "device pause sync failed (server unreachable — local gate still applies)" }
        }
    }

    /**
     * `POST /auth/session` and cache the result. Null on failure.
     *
     * [force] bypasses the cached token and the failure backoff (stale-JWT
     * recovery — the server just rejected the cache, so it is reachable).
     * Ordinary mints back off for [MINT_FAILURE_BACKOFF_MS] after a failure
     * so concurrent/best-effort data calls cannot stampede the issuer.
     */
    private suspend fun mintSession(force: Boolean = false): String? {
        val now = currentEpochMillis()
        if (!force && now - lastMintFailureAt < MINT_FAILURE_BACKOFF_MS) return null
        val minted = alertsApi.createSession(
            installId = installId,
            platform = platform(),
            appVersion = appVersionName(),
        ).fold(
            onSuccess = { response ->
                storage.setSessionToken(response.sessionToken)
                storage.setSessionExpiresAt(response.expiresAt)
                lastMintFailureAt = 0L
                Napier.i(tag = TAG) { "anonymous session minted (expires in ${(response.expiresAt - currentEpochMillis()) / 3_600_000}h)" }
                response.sessionToken
            },
            onFailure = { error ->
                lastMintFailureAt = now
                Napier.w(tag = TAG, throwable = error) { "session mint request failed" }
                null
            },
        )
        return minted
    }

    companion object {
        private const val TAG = "PushSession"

        /** Uniform failure for "no session could be established". */
        const val NO_SESSION_MESSAGE =
            "No anonymous session — server unreachable or session mint failed"

        /** Refresh the JWT when less than 7 days remain (plan §3.1). */
        const val SESSION_REFRESH_MARGIN_MS = 7L * 24 * 60 * 60 * 1000L

        /** Best-effort mint backoff after a failed `/auth/session`. */
        const val MINT_FAILURE_BACKOFF_MS = 60L * 1000L

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
