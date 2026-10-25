package dali.hamza.shared.platform

import io.github.aakira.napier.Napier

/**
 * One measured backend exchange — reported to Napier (local logs) and to
 * the host-app sink (Sentry/GlitchTip breadcrumbs; slow/failed calls are
 * promoted to events by the sink).
 */
data class HttpTelemetryEvent(
    /** "historic" | "latest" — what kind of call. */
    val operation: String,
    /** Human detail: "USD/EUR 2026-07-26..2026-10-25". */
    val detail: String,
    val durationMs: Long,
    val ok: Boolean,
    /** Days served from the local cache (historic read-through). */
    val cachedDays: Long = 0,
    /** Days fetched from the backend this call. */
    val fetchedDays: Long = 0,
)

/** Implemented by the host app (Android: sentry-android; iOS: SentryCocoa). */
interface HttpTelemetrySink {
    fun report(event: HttpTelemetryEvent)
}

private var httpTelemetrySink: HttpTelemetrySink? = null

/** Host app registers its GlitchTip forwarder at startup. */
fun installHttpTelemetry(sink: HttpTelemetrySink) {
    httpTelemetrySink = sink
}

internal fun reportHttpTelemetry(event: HttpTelemetryEvent) {
    val cacheInfo = if (event.cachedDays > 0 || event.fetchedDays > 0) {
        " · cached=${event.cachedDays}d fetched=${event.fetchedDays}d"
    } else {
        ""
    }
    Napier.i(
        "HTTP ${event.operation} ${event.detail} " +
            "${if (event.ok) "ok" else "FAIL"} ${event.durationMs}ms$cacheInfo",
        tag = "HTTP",
    )
    runCatching { httpTelemetrySink?.report(event) }
}
