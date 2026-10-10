package dali.hamza.shared.data.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Server-push alert DTOs — mirror of the exchange-api Phase 1 models
 * (`hamza.dali.models.Alerts`, Gson on the server / kotlinx here; field
 * names pinned by plans/server-push-alerts.md §3.3).
 *
 * All request fields are nullable: the server validates explicitly and
 * treats missing JSON as null, so clients must not assume defaults.
 */

/** `POST /auth/session` — mint/refresh an anonymous session JWT. */
@Serializable
data class SessionRequest(
    @SerialName("installId") val installId: String,
    @SerialName("platform") val platform: String,
    @SerialName("appVersion") val appVersion: String? = null,
)

@Serializable
data class SessionResponse(
    @SerialName("sessionToken") val sessionToken: String,
    @SerialName("expiresAt") val expiresAt: Long,
)

/** `POST /alerts/devices` — idempotent FCM/APNs token upsert (Bearer). */
@Serializable
data class DeviceRegistrationRequest(
    @SerialName("platform") val platform: String,
    @SerialName("token") val token: String,
    @SerialName("bundleId") val bundleId: String? = null,
    @SerialName("appVersion") val appVersion: String? = null,
)

/** `DELETE /alerts/devices` — sign-out / token-rotation cleanup (Bearer). */
@Serializable
data class DeviceRemovalRequest(
    @SerialName("token") val token: String,
)

/** `POST /alerts` — create one server-evaluated alert (Bearer). */
@Serializable
data class CreateAlertRequest(
    @SerialName("base") val base: String,
    @SerialName("quote") val quote: String,
    @SerialName("mode") val mode: String,
    @SerialName("thresholdPercent") val thresholdPercent: Double? = null,
    @SerialName("intervalMinutes") val intervalMinutes: Long? = null,
)

/** `PATCH /alerts/{id}` — partial update; null fields stay untouched. */
@Serializable
data class UpdateAlertRequest(
    @SerialName("enabled") val enabled: Boolean? = null,
    @SerialName("thresholdPercent") val thresholdPercent: Double? = null,
    @SerialName("intervalMinutes") val intervalMinutes: Long? = null,
)

/** One server alert row — same shape as the local `RateAlert` domain model. */
@Serializable
data class AlertDataAPI(
    @SerialName("id") val id: Long,
    @SerialName("base") val base: String,
    @SerialName("quote") val quote: String,
    @SerialName("mode") val mode: String,
    @SerialName("thresholdPercent") val thresholdPercent: Double,
    @SerialName("intervalMinutes") val intervalMinutes: Long,
    @SerialName("enabled") val enabled: Boolean,
    @SerialName("lastRate") val lastRate: Double? = null,
    @SerialName("lastNotifiedAt") val lastNotifiedAt: Long,
    @SerialName("createdAt") val createdAt: Long,
)

/** Error body on every non-2xx alerts/session response. */
@Serializable
data class AlertsErrorResponse(
    @SerialName("errorCode") val errorCode: String? = null,
    @SerialName("message") val message: String? = null,
)
