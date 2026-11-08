package dali.hamza.echangecurrencyapp.telemetry

import dali.hamza.shared.platform.HttpTelemetryEvent
import dali.hamza.shared.platform.HttpTelemetrySink
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb

/**
 * GlitchTip forwarder for shared HTTP timing telemetry: every measured
 * backend call lands as a breadcrumb; slow (>1.5s) or failed calls are
 * promoted to warning events so they surface in the GlitchTip UI.
 * Uses the Sentry KMP API (same surface the shared module reports
 * through). Safe to install without a DSN — the SDK no-ops when
 * disabled.
 */
class SentryHttpTelemetry : HttpTelemetrySink {

    override fun report(event: HttpTelemetryEvent) {
        val message = "HTTP ${event.operation} ${event.detail} " +
            "${event.durationMs}ms${if (event.ok) "" else " FAILED"}"
        val crumb = Breadcrumb()
        crumb.category = "http"
        crumb.message = message
        crumb.setData("operation", event.operation)
        crumb.setData("durationMs", event.durationMs)
        crumb.setData("ok", event.ok)
        if (event.cachedDays > 0 || event.fetchedDays > 0) {
            crumb.setData("cachedDays", event.cachedDays)
            crumb.setData("fetchedDays", event.fetchedDays)
        }
        Sentry.addBreadcrumb(crumb)
        if (!event.ok || event.durationMs > SLOW_MS) {
            Sentry.captureMessage(message) { scope ->
                scope.level = SentryLevel.WARNING
            }
        }
    }

    private companion object {
        const val SLOW_MS = 1_500L
    }
}
