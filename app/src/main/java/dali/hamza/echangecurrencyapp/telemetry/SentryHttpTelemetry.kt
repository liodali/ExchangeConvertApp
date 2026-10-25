package dali.hamza.echangecurrencyapp.telemetry

import dali.hamza.shared.platform.HttpTelemetryEvent
import dali.hamza.shared.platform.HttpTelemetrySink
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel

/**
 * GlitchTip forwarder for shared HTTP timing telemetry: every measured
 * backend call lands as a breadcrumb; slow (>1.5s) or failed calls are
 * promoted to warning events so they surface in the GlitchTip UI.
 * Safe to install without a DSN — the Sentry SDK no-ops when disabled.
 */
class SentryHttpTelemetry : HttpTelemetrySink {

    override fun report(event: HttpTelemetryEvent) {
        val message = "HTTP ${event.operation} ${event.detail} " +
            "${event.durationMs}ms${if (event.ok) "" else " FAILED"}"
        Sentry.addBreadcrumb(
            Breadcrumb().apply {
                category = "http"
                this.message = message
                setData("operation", event.operation)
                setData("durationMs", event.durationMs)
                setData("ok", event.ok)
                if (event.cachedDays > 0 || event.fetchedDays > 0) {
                    setData("cachedDays", event.cachedDays)
                    setData("fetchedDays", event.fetchedDays)
                }
            },
        )
        if (!event.ok || event.durationMs > SLOW_MS) {
            Sentry.captureMessage(message, SentryLevel.WARNING)
        }
    }

    private companion object {
        const val SLOW_MS = 1_500L
    }
}
