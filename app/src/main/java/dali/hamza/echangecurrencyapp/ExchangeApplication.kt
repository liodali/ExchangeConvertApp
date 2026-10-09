package dali.hamza.echangecurrencyapp

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dali.hamza.echangecurrencyapp.di.appModule
import dali.hamza.shared.AndroidAppContext
import dali.hamza.shared.di.DEFAULT_HOST
import dali.hamza.shared.di.sharedModule
import dali.hamza.shared.domain.repository.IRepository
import dali.hamza.shared.platform.LocalNotifier
import dali.hamza.shared.platform.RateAlertScheduler
import io.github.aakira.napier.Napier
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
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

        // Rate alerts: align the WorkManager job with the stored alert
        // state (an immediate check runs when any alert is enabled).
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            runCatching {
                val koin = GlobalContext.get()
                val hasActiveAlerts = koin.get<IRepository>().getEnabledRateAlerts().isNotEmpty()
                koin.get<RateAlertScheduler>().update(hasActiveAlerts)
            }
        }

        // FCM (server-push rate alerts): fetch the registration token early
        // so it lands in logcat for backend/console testing. Guarded —
        // builds without google-services.json have no FirebaseApp and the
        // messaging SDK stays inert (see push/SovereignMessagingService).
        // prepare() creates the rate_alerts channel up-front so system-trayed
        // FCM pushes land on the right channel, not the FCM fallback.
        runCatching { GlobalContext.get().get<LocalNotifier>().prepare() }
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (BuildConfig.DEBUG) {
                        Napier.i(tag = FCM_TAG) { "FCM registration token: $token" }
                    }
                }
                .addOnFailureListener { e ->
                    Napier.w(tag = FCM_TAG, throwable = e) { "FCM token fetch failed" }
                }
        }
    }

    private companion object {
        const val FCM_TAG = "SovereignFCM"
    }
}
