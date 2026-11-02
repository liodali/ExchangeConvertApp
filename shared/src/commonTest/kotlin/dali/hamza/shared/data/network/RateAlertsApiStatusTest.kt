package dali.hamza.shared.data.network

import dali.hamza.shared.domain.models.RateAlertMode
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [RateAlertsApi] must surface non-2xx statuses as [AlertsApiError] — the
 * session wrapper's 401 self-heal keys off that status, and device
 * registration previously ignored response codes entirely.
 */
class RateAlertsApiStatusTest {

    private fun api(status: HttpStatusCode): RateAlertsApi {
        val engine = MockEngine { _ ->
            respond(
                content = """{"errorCode":"unauthorized","message":"Invalid token"}""",
                status = status,
                headers = headersOf(
                    "Content-Type",
                    listOf(ContentType.Application.Json.toString()),
                ),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        return RateAlertsApi(client)
    }

    @Test
    fun listAlertsUnauthorizedSurfacesStatus401() = runTest {
        val error = api(HttpStatusCode.Unauthorized).listAlerts("stale")
            .exceptionOrNull() as AlertsApiError

        assertEquals(401, error.status)
        assertEquals("unauthorized", error.errorCode ?: error.message)
    }

    @Test
    fun registerDeviceFailureIsNotSwallowedAsSuccess() = runTest {
        val result = api(HttpStatusCode.Unauthorized).registerDevice(
            bearerToken = "stale",
            platform = "ANDROID",
            token = "fcm-token",
            bundleId = "com.example",
            appVersion = "test",
        )

        assertTrue(result.isFailure)
        assertEquals(401, (result.exceptionOrNull() as AlertsApiError).status)
    }

    @Test
    fun createAlertCapKeepsErrorCodeForFriendlyCopy() = runTest {
        val error = api(HttpStatusCode.Forbidden).createAlert(
            bearerToken = "valid",
            base = "EUR",
            quote = "USD",
            mode = RateAlertMode.PERIODIC,
            intervalMinutes = 120,
            thresholdPercent = 1.0,
        ).exceptionOrNull() as AlertsApiError

        assertEquals(403, error.status)
    }
}
