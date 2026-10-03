package dali.hamza.echangecurrencyapp

import android.app.Application
import dali.hamza.echangecurrencyapp.di.appModule
import dali.hamza.shared.AndroidAppContext
import dali.hamza.shared.di.DEFAULT_HOST
import dali.hamza.shared.di.sharedModule
import io.sentry.android.core.SentryAndroid
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.loadKoinModules


class ExchangeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // shared KMP module: context first (storage/driver actuals need it)
        AndroidAppContext.appContext = this

        // crash reporting → self-hosted GlitchTip (Sentry-compatible OSS).
        // DSN comes from local.properties / CI secret; empty = disabled
        // (local dev without a DSN stays fully functional). PII stays off
        // and only crashes/ANRs are reported — no tracing, no user ids.
        if (BuildConfig.GLITCHTIP_DSN.isNotBlank()) {
            SentryAndroid.init(this) { options ->
                options.dsn = BuildConfig.GLITCHTIP_DSN
                options.environment = if (BuildConfig.DEBUG) "debug" else "production"
                options.release = "sovereign-ledger@${BuildConfig.VERSION_NAME}"
                options.isSendDefaultPii = false
                options.tracesSampleRate = 0.0 // crashes/ANRs only
                options.isDebug = BuildConfig.DEBUG // SDK transport logs in dev builds
            }
        }

        startKoin {
            // Reference Android context
            androidContext(this@ExchangeApplication)
            // Load modules
            modules(appModule)
        }
        // shared module rides on the same Koin instance. Backend host:
        // BuildConfig.SERVER_HOST (gradle prop / CI variable) or the
        // shared module's DEFAULT_HOST when unset.
        loadKoinModules(
            sharedModule(serverURL = BuildConfig.SERVER_HOST.ifBlank { DEFAULT_HOST })
        )
    }
}
