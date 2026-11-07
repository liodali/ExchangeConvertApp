package dali.hamza.shared.data.widgets

import dali.hamza.shared.data.network.CurrencyApi
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
 * Widget snapshot fetching (plans/widgets.md tests, deferred from Phase 1):
 * fiat pairs ride the no-symbol `latest` payload, but asset bases
 * (XAU/XAG/BTC) never do — they resolve through the symbol-filtered pair
 * call so the backend's all-fiat USD pivot applies (plans/gold-section.md),
 * and their day-Δ is omitted (mixed spot/futures provenance).
 */
class WidgetsRepositoryTest {

    private class Recorder {
        val requests = mutableListOf<String>()
        val responses = mutableMapOf<String, String>()
        fun respond(path: String, body: String) {
            responses[path] = body
        }
    }

    private fun api(recorder: Recorder): CurrencyApi {
        val engine = MockEngine { request ->
            recorder.requests += "${request.url.encodedPath}?${request.url.encodedQuery}"
            val body = recorder.responses["${request.url.encodedPath}?${request.url.encodedQuery}"]
                ?: recorder.responses[request.url.encodedPath]
                ?: return@MockEngine respond(
                    """{"base":"X","time":"now","rates":{}}""",
                    HttpStatusCode.OK,
                    jsonHeaders(),
                )
            respond(body, HttpStatusCode.OK, jsonHeaders())
        }
        return CurrencyApi(
            httpClient = HttpClient(engine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true; isLenient = true })
                }
            },
            bearerProvider = { null },
        )
    }

    private fun jsonHeaders() = headersOf(
        "Content-Type",
        listOf(ContentType.Application.Json.toString()),
    )

    @Test
    fun fiatPairReadsTheNoSymbolLatestPayload() = runTest {
        val recorder = Recorder().apply {
            respond(
                "/latest?base=EUR",
                """{"base":"EUR","time":"now","rates":{"TND":3.35,"USD":1.08}}""",
            )
        }
        val snapshots = fetchSnapshots(api(recorder), listOf("EUR" to "TND"), now = 1_000L, yesterday = "2026-11-06")

        assertEquals(1, snapshots.size)
        assertEquals(3.35, snapshots.single().rate)
        assertEquals("EUR", snapshots.single().base)
        assertEquals("TND", snapshots.single().quote)
    }

    @Test
    fun goldPairUsesTheSymbolFilteredCall() = runTest {
        val recorder = Recorder().apply {
            // the no-symbol payload has NO TND row for XAU (direct quotes
            // only) — the pivot only answers explicit symbol requests
            respond(
                "/latest?base=XAU",
                """{"base":"XAU","time":"now","rates":{"USD":3876.4,"EUR":3471.7}}""",
            )
            respond(
                "/latest?base=XAU&symbol=TND&amount=1.0",
                """{"base":"XAU","time":"now","rates":{"TND":11599.05}}""",
            )
        }
        val snapshots = fetchSnapshots(api(recorder), listOf("XAU" to "TND"), now = 1_000L, yesterday = "2026-11-06")

        assertEquals(1, snapshots.size)
        assertEquals(11599.05, snapshots.single().rate, "the widget must not read the direct-only payload")
        assertTrue(
            recorder.requests.any { it.startsWith("/latest?base=XAU&symbol=TND") },
            "the pair call is the one that must be made",
        )
    }

    @Test
    fun metalDayDeltaIsOmitted() = runTest {
        val recorder = Recorder().apply {
            respond(
                "/latest?base=XAU&symbol=USD&amount=1.0",
                """{"base":"XAU","time":"now","rates":{"USD":3876.4}}""",
            )
            // even if a historic close existed, metals must not show a Δ
            respond(
                "/historical",
                """{"base":"XAU","time":"now","quotes":{"XAUUSD":4100.0}}""",
            )
        }
        val snapshots = fetchSnapshots(api(recorder), listOf("XAU" to "USD"), now = 1_000L, yesterday = "2026-11-06")

        assertNull(snapshots.single().prevClose, "mixed spot/futures provenance — no day Δ")
        assertNull(snapshots.single().dayDeltaPercent)
    }

    @Test
    fun quotableNowherePairIsSkippedNotZero() = runTest {
        val recorder = Recorder() // everything answers empty rates
        val snapshots = fetchSnapshots(api(recorder), listOf("XAU" to "TND", "EUR" to "USD"), now = 1L, yesterday = "d")

        assertTrue(snapshots.isEmpty(), "failed pairs are skipped, never fabricated as 0.0")
    }

    @Test
    fun invalidQuotesAreRejected() = runTest {
        val recorder = Recorder().apply {
            respond(
                "/latest?base=XAU&symbol=TND&amount=1.0",
                """{"base":"XAU","time":"now","rates":{"TND":0.0}}""",
            )
        }
        val snapshots = fetchSnapshots(api(recorder), listOf("XAU" to "TND"), now = 1L, yesterday = "d")
        assertTrue(snapshots.isEmpty(), "zero is not a price")
    }

    @Test
    fun perGramGoldMathMatchesTheHomeCard() = runTest {
        val recorder = Recorder().apply {
            respond(
                "/latest?base=XAU&symbol=TND&amount=1.0",
                """{"base":"XAU","time":"now","rates":{"TND":11599.05}}""",
            )
        }
        val snapshots = fetchSnapshots(api(recorder), listOf("XAU" to "TND"), now = 1L, yesterday = "d")
        // widget renders rate/TROY_OUNCE_GRAMS — 24K per gram, same as Home
        val perGram = snapshots.single().rate / dali.hamza.shared.common.MetalPricing.TROY_OUNCE_GRAMS
        assertEquals(372.918, perGram, 0.01)
    }
}
