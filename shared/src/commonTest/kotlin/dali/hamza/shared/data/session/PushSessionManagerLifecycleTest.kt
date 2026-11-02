package dali.hamza.shared.data.session

import dali.hamza.shared.data.network.AlertsApiError
import dali.hamza.shared.data.network.RateAlertsApi
import dali.hamza.shared.data.storage.ISessionStorage
import dali.hamza.shared.platform.currentEpochMillis
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Anonymous-session lifecycle tests (plans/anonymous-session-lifecycle.md):
 * single-flight minting, stable installId, forced re-mint, and the
 * one-shot 401 recovery — a JWT may be replaced, the anonymous identity
 * (`installId` / JWT `sub`) never is.
 */
class PushSessionManagerLifecycleTest {

    private class FakeSessionStorage : ISessionStorage {
        var storedInstallId: String? = null
        var storedSessionToken: String? = null
        var storedSessionExpiresAt: Long = 0L
        var storedLastPushToken: String? = null

        override fun getCurrency(): String = "USD"
        override fun setCurrency(value: String) = Unit
        override fun getLastUpdate(): Long = 0L
        override fun setLastUpdate(timestamp: Long) = Unit
        override fun setUsername(value: String) = Unit

        override fun getInstallId(): String? = storedInstallId
        override fun setInstallId(value: String) {
            storedInstallId = value
        }

        override fun getSessionToken(): String? = storedSessionToken
        override fun setSessionToken(token: String?) {
            storedSessionToken = token
        }

        override fun getSessionExpiresAt(): Long = storedSessionExpiresAt
        override fun setSessionExpiresAt(timestamp: Long) {
            storedSessionExpiresAt = timestamp
        }

        override fun getLastPushToken(): String? = storedLastPushToken
        override fun setLastPushToken(token: String?) {
            storedLastPushToken = token
        }
    }

    private fun alertsApi(token: String, status: HttpStatusCode = HttpStatusCode.OK): RateAlertsApi {
        val engine = MockEngine { _ ->
            val body = if (status.value !in 200..299) {
                """{"error":"server_error","message":"unreachable"}"""
            } else {
                """{"sessionToken":"$token","expiresAt":${Long.MAX_VALUE}}"""
            }
            respond(
                content = body,
                status = status,
                headers = headersOf("Content-Type", listOf(ContentType.Application.Json.toString())),
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }.let(::RateAlertsApi)
    }

    @Test
    fun concurrentEnsureSessionMintsOnlyOnce() = runTest {
        val api = alertsApi(token = "single")
        val manager = PushSessionManager(alertsApi = api, storage = FakeSessionStorage())

        val tokens = (1..8).map { async { manager.ensureSession() } }.awaitAll()

        assertEquals(listOf("single"), tokens.mapNotNull { it }.toSet().toList())
    }

    @Test
    fun usableCachedSessionAvoidsMinting() = runTest {
        val storage = FakeSessionStorage().apply {
            storedSessionToken = "cached"
            storedSessionExpiresAt = Long.MAX_VALUE
        }
        val manager = PushSessionManager(alertsApi = alertsApi("unused"), storage = storage)

        assertEquals("cached", manager.ensureSession())
    }

    @Test
    fun forcedRefreshKeepsInstallIdAndReplacesOnlyOnSuccess() = runTest {
        val storage = FakeSessionStorage().apply {
            storedSessionToken = "stale"
            storedSessionExpiresAt = Long.MAX_VALUE
        }
        val manager = PushSessionManager(alertsApi = alertsApi("fresh"), storage = storage)

        assertEquals("fresh", manager.refreshSession())
        assertEquals("fresh", storage.storedSessionToken)
        assertEquals(storage.storedInstallId, manager.installId)
    }

    @Test
    fun authRejectionReMintsOnceWithSameIdentity() = runTest {
        val storage = FakeSessionStorage().apply {
            storedSessionToken = "stale"
            storedSessionExpiresAt = Long.MAX_VALUE
        }
        val manager = PushSessionManager(alertsApi = alertsApi("fresh"), storage = storage)
        val bearerCalls = mutableListOf<String>()

        val outcome = manager.withSession { bearer ->
            bearerCalls += bearer
            Result.failure<Unit>(AlertsApiError(errorCode = null, message = "unauthorized", status = 401))
        }

        assertEquals(listOf("stale", "fresh"), bearerCalls)
        assertTrue(outcome.isFailure, "second rejection must win — no retry loop")
        assertEquals(storage.storedInstallId, manager.installId)
    }

    @Test
    fun nearExpirySessionRefreshesWithSameIdentity() = runTest {
        val storage = FakeSessionStorage().apply {
            storedSessionToken = "aging"
            storedSessionExpiresAt = currentEpochMillis() + 60_000L
        }
        val manager = PushSessionManager(alertsApi = alertsApi("renewed"), storage = storage)

        assertEquals("renewed", manager.ensureSession())
        assertEquals("renewed", storage.storedSessionToken)
        assertEquals(storage.storedInstallId, manager.installId)
    }

    @Test
    fun failedMintPreservesUnexpiredCachedToken() = runTest {
        val storage = FakeSessionStorage().apply {
            storedSessionToken = "fallback"
            // inside the refresh margin but not yet expired
            storedSessionExpiresAt = currentEpochMillis() + 60_000L
        }
        val manager = PushSessionManager(
            alertsApi = alertsApi(token = "unreachable", status = HttpStatusCode.ServiceUnavailable),
            storage = storage,
        )

        assertEquals("fallback", manager.ensureSession())
        assertEquals("fallback", storage.storedSessionToken)
    }

    @Test
    fun failedFirstMintReturnsNullWhenNoCacheExists() = runTest {
        val manager = PushSessionManager(
            alertsApi = alertsApi(token = "unreachable", status = HttpStatusCode.ServiceUnavailable),
            storage = FakeSessionStorage(),
        )

        assertNull(manager.ensureSession())
    }
}
