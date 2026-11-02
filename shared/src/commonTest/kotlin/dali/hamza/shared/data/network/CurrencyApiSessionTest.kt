package dali.hamza.shared.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [CurrencyApi] anonymous-session behavior (plans/anonymous-session-lifecycle.md):
 * on-demand Bearer resolution and the bounded stale-JWT recovery on the
 * auth-gated data endpoints (401, and 404 only while a Bearer was sent).
 */
class CurrencyApiSessionTest {

    private fun client(
        handler: MockRequestHandleScope.(request: HttpRequestData) -> HttpResponseData,
    ): HttpClient {
        val engine = MockEngine { request -> handler(request) }
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
    }

    private fun okRates() = """{"base":"USD","time":"now","rates":{"EUR":0.9}}"""

    private fun jsonHeaders() = headersOf(
        "Content-Type",
        listOf(ContentType.Application.Json.toString()),
    )

    @Test
    fun tokenlessSoftModeRequestGoesBareAndSucceeds() = runTest {
        var authorization: String? = "sentinel"
        val api = CurrencyApi(
            httpClient = client { request ->
                authorization = request.headers[io.ktor.http.HttpHeaders.Authorization]
                respond(okRates(), HttpStatusCode.OK, jsonHeaders())
            },
            bearerProvider = { null },
        )

        assertTrue(api.getLiveRates("USD").isSuccess)
        assertNull(authorization)
    }

    @Test
    fun sessionIsResolvedOnDemandAndAttached() = runTest {
        var authorization: String? = null
        val api = CurrencyApi(
            httpClient = client { request ->
                authorization = request.headers[io.ktor.http.HttpHeaders.Authorization]
                respond(okRates(), HttpStatusCode.OK, jsonHeaders())
            },
            bearerProvider = { "fresh" },
        )

        assertTrue(api.getLiveRates("USD").isSuccess)
        assertEquals("Bearer fresh", authorization)
    }

    @Test
    fun unauthorizedSessionReMintsOnceAndRetries() = runTest {
        val requests = mutableListOf<String?>()
        var token = "stale"
        var forcedRefreshes = 0
        val api = CurrencyApi(
            httpClient = client { request ->
                val auth = request.headers[io.ktor.http.HttpHeaders.Authorization]
                requests += auth
                if (auth == null) {
                    respond(okRates(), HttpStatusCode.OK, jsonHeaders())
                } else if (auth == "Bearer stale") {
                    respond("""{"error":"unauthorized"}""", HttpStatusCode.Unauthorized, jsonHeaders())
                } else {
                    respond(okRates(), HttpStatusCode.OK, jsonHeaders())
                }
            },
            bearerProvider = { force ->
                if (force) {
                    forcedRefreshes += 1
                    token = "fresh"
                }
                token
            },
        )

        val result = api.getLiveRates("USD")

        assertTrue(result.isSuccess)
        assertEquals<List<String?>>(listOf("Bearer stale", "Bearer fresh"), requests)
        assertEquals(1, forcedRefreshes)
    }

    @Test
    fun hardGate404WithBearerHealsExactlyOnce() = runTest {
        val requests = mutableListOf<String?>()
        var token = "stale"
        val api = CurrencyApi(
            httpClient = client { request ->
                val auth = request.headers[io.ktor.http.HttpHeaders.Authorization]
                requests += auth
                if (auth == "Bearer stale") {
                    respond("""{"error":"not_found"}""", HttpStatusCode.NotFound, jsonHeaders())
                } else {
                    respond(okRates(), HttpStatusCode.OK, jsonHeaders())
                }
            },
            bearerProvider = { force ->
                if (force) token = "fresh"
                token
            },
        )

        val result = api.getLiveRates("USD")

        assertTrue(result.isSuccess)
        assertEquals<List<String?>>(listOf("Bearer stale", "Bearer fresh"), requests)
    }

    @Test
    fun secondRejectionWinsInsteadOfLooping() = runTest {
        val requests = mutableListOf<String?>()
        var forcedRefreshes = 0
        val api = CurrencyApi(
            httpClient = client { request ->
                requests += request.headers[io.ktor.http.HttpHeaders.Authorization]
                respond("""{"error":"unauthorized"}""", HttpStatusCode.Unauthorized, jsonHeaders())
            },
            bearerProvider = { force ->
                if (force) forcedRefreshes += 1
                "always-rejected"
            },
        )

        val result = api.getLiveRates("USD")

        assertTrue(result.isFailure)
        assertEquals(2, requests.size, "exactly one original attempt and one retry")
        assertEquals(1, forcedRefreshes)
    }

    @Test
    fun legacyHistorical404IsNotTreatedAsStaleAuth() = runTest {
        val requests = mutableListOf<String?>()
        var forcedRefreshes = 0
        val api = CurrencyApi(
            httpClient = client { request ->
                requests += request.headers[io.ktor.http.HttpHeaders.Authorization]
                respond("""{"error":"not_found"}""", HttpStatusCode.NotFound, jsonHeaders())
            },
            bearerProvider = { force ->
                if (force) forcedRefreshes += 1
                "fresh"
            },
        )

        val result = api.getHistoricRates(date = "2026-01-01", source = "USD")

        assertTrue(result.isFailure)
        assertEquals(1, requests.size, "legacy endpoint is outside the auth gate — no retry")
        assertEquals(0, forcedRefreshes)
    }
}
