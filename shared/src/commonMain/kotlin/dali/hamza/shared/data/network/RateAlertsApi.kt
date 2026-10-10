package dali.hamza.shared.data.network

import dali.hamza.shared.data.network.models.AlertDataAPI
import dali.hamza.shared.data.network.models.AlertsErrorResponse
import dali.hamza.shared.data.network.models.CreateAlertRequest
import dali.hamza.shared.data.network.models.DeviceDataAPI
import dali.hamza.shared.data.network.models.DevicePauseRequest
import dali.hamza.shared.data.network.models.DeviceRegistrationRequest
import dali.hamza.shared.data.network.models.DeviceRemovalRequest
import dali.hamza.shared.data.network.models.SessionRequest
import dali.hamza.shared.data.network.models.SessionResponse
import dali.hamza.shared.data.network.models.UpdateAlertRequest
import dali.hamza.shared.domain.models.RateAlertMode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

/**
 * Failure carrying the backend's `errorCode` (duplicate, alert_cap, …) so
 * callers can map to friendly copy instead of raw HTTP exceptions.
 */
class AlertsApiError(
    val errorCode: String?,
    override val message: String,
    val status: Int = -1,
) : Exception(message) {
    companion object {
        const val CODE_DUPLICATE = "duplicate"
        const val CODE_ALERT_CAP = "alert_cap"
        const val CODE_RATE_LIMITED = "rate_limited"
    }
}

/**
 * Ktor client for the server-push alert endpoints on our exchange-api
 * backend (Phase 1 server, Phase 2 client — plans/server-push-alerts.md):
 *
 * - `POST /auth/session`      anonymous session JWT (no auth)
 * - `POST /alerts/devices`    FCM/APNs token upsert          (Bearer)
 * - `GET  /alerts`            list server alerts             (Bearer)
 * - `POST /alerts`            create (tier caps server-side) (Bearer)
 * - `PATCH /alerts/{id}`      toggle/edit                    (Bearer)
 * - `DELETE /alerts/{id}`                                   (Bearer)
 *
 * Rides the same [HttpClient] as [CurrencyApi] (same base host, same
 * kotlinx JSON config).
 */
class RateAlertsApi(
    private val httpClient: HttpClient,
) {

    private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Mint (or re-mint) the anonymous session JWT for this install. */
    suspend fun createSession(
        installId: String,
        platform: String,
        appVersion: String,
    ): Result<SessionResponse> = call {
        httpClient.post("auth/session") {
            // explicit content type: request-body negotiation only kicks in
            // when the builder declares one (first POST bodies in the app)
            contentType(ContentType.Application.Json)
            setBody(
                SessionRequest(
                    installId = installId,
                    platform = platform,
                    appVersion = appVersion,
                )
            )
        }.body<SessionResponse>()
    }

    /**
     * Idempotent device registration. The server detaches the token from
     * any previous owner (UNIQUE(token)) — re-posting is the self-healing
     * path after reinstalls or staging/prod switches.
     */
    suspend fun registerDevice(
        bearerToken: String,
        platform: String,
        token: String,
        bundleId: String,
        appVersion: String,
    ): Result<Unit> = call {
        httpClient.post("alerts/devices") {
            bearerAuth(bearerToken)
            contentType(ContentType.Application.Json)
            setBody(
                DeviceRegistrationRequest(
                    platform = platform,
                    token = token,
                    bundleId = bundleId,
                    appVersion = appVersion,
                )
            )
        }
        Unit
    }

    /**
     * Remove a push token from the session (server marks the row inactive).
     * Called on token rotation — after the replacement registers — so the
     * previous token doesn't linger as a live row until a send or the 30d
     * hygiene pass removes it.
     */
    suspend fun unregisterDevice(bearerToken: String, token: String): Result<Unit> = call {
        httpClient.delete("alerts/devices") {
            bearerAuth(bearerToken)
            contentType(ContentType.Application.Json)
            setBody(DeviceRemovalRequest(token = token))
        }
        Unit
    }

    /** Own registered push devices (future admin panel / device management UI). */
    suspend fun listDevices(bearerToken: String): Result<List<DeviceDataAPI>> = call {
        httpClient.get("alerts/devices") {
            bearerAuth(bearerToken)
        }.body<List<DeviceDataAPI>>()
    }

    /**
     * Per-device pause — PATCH /alerts/devices/{id} {pausedUntil}. 0 resumes.
     * Used by the Account "Push Notifications" preference: off pauses this
     * device's pushes server-side; on resumes them.
     */
    suspend fun setDevicePaused(
        bearerToken: String,
        deviceId: Long,
        pausedUntil: Long,
    ): Result<Unit> = call {
        httpClient.patch("alerts/devices/$deviceId") {
            bearerAuth(bearerToken)
            contentType(ContentType.Application.Json)
            setBody(DevicePauseRequest(pausedUntil = pausedUntil))
        }
        Unit
    }

    suspend fun listAlerts(bearerToken: String): Result<List<AlertDataAPI>> = call {
        httpClient.get("alerts") {
            bearerAuth(bearerToken)
        }.body<List<AlertDataAPI>>()
    }

    suspend fun createAlert(
        bearerToken: String,
        base: String,
        quote: String,
        mode: RateAlertMode,
        intervalMinutes: Long,
        thresholdPercent: Double,
    ): Result<AlertDataAPI> = call {
        httpClient.post("alerts") {
            bearerAuth(bearerToken)
            contentType(ContentType.Application.Json)
            setBody(
                CreateAlertRequest(
                    base = base,
                    quote = quote,
                    mode = mode.name,
                    thresholdPercent = thresholdPercent,
                    intervalMinutes = intervalMinutes,
                )
            )
        }.body<AlertDataAPI>()
    }

    suspend fun updateAlert(
        bearerToken: String,
        id: Long,
        enabled: Boolean? = null,
        thresholdPercent: Double? = null,
        intervalMinutes: Long? = null,
    ): Result<AlertDataAPI> = call {
        httpClient.patch("alerts/$id") {
            bearerAuth(bearerToken)
            contentType(ContentType.Application.Json)
            setBody(
                UpdateAlertRequest(
                    enabled = enabled,
                    thresholdPercent = thresholdPercent,
                    intervalMinutes = intervalMinutes,
                )
            )
        }.body<AlertDataAPI>()
    }

    suspend fun deleteAlert(bearerToken: String, id: Long): Result<Unit> = call {
        httpClient.delete("alerts/$id") {
            bearerAuth(bearerToken)
        }
        Unit
    }

    // ------------------------------------------------------------- helpers

    /**
     * Uniform error surface: non-2xx responses (Ktor's default validation
     * throws) are converted to [AlertsApiError] with the server's
     * `errorCode`/`message` when parseable.
     */
    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: ResponseException) {
        val status = e.response.status.value
        val error = runCatching {
            lenientJson.decodeFromString(AlertsErrorResponse.serializer(), e.response.bodyAsText())
        }.getOrNull()
        Result.failure(
            AlertsApiError(
                errorCode = error?.errorCode,
                message = error?.message ?: "Alert service error ($status)",
                status = status,
            )
        )
    } catch (e: Exception) {
        Result.failure(e)
    }
}
