package dali.hamza.echangecurrencyapp

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dali.hamza.echangecurrencyapp.di.appModule
import dali.hamza.echangecurrencyapp.push.PushRegistration
import dali.hamza.shared.AndroidAppContext
import dali.hamza.shared.data.widgets.WidgetSnapshotStore
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

        // shared HTTP timing → GlitchTip breadcrumbs (slow/failed → events)
        dali.hamza.shared.platform.installHttpTelemetry(
            dali.hamza.echangecurrencyapp.telemetry.SentryHttpTelemetry(),
        )

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

        // Market-rate widget: keep the 30-min snapshot refresh aligned with
        // the widget set (plans/widgets.md Phase 1). Glance keeps the LAST
        // render after an app update — an immediate pass on every app start
        // repaints placed widgets with current code. Instance detection
        // covers the zero-config grid too (it stores no config).
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            runCatching {
                val manager = androidx.glance.appwidget.GlanceAppWidgetManager(this@ExchangeApplication)
                val hasInstances = listOf(
                    dali.hamza.echangecurrencyapp.widgets.MarketRateWidget(),
                    dali.hamza.echangecurrencyapp.widgets.MarketRateCardWidget(),
                    dali.hamza.echangecurrencyapp.widgets.MarketGridWidget(),
                    dali.hamza.echangecurrencyapp.widgets.QuickConvertWidget(),
                ).any { widget -> manager.getGlanceIds(widget.javaClass).isNotEmpty() }
                val store = GlobalContext.get().get<WidgetSnapshotStore>()
                val hasWidgets = hasInstances || store.readConfigs().isNotEmpty()
                dali.hamza.echangecurrencyapp.widgets.WidgetRefreshScheduler.update(this@ExchangeApplication, hasWidgets)
                if (hasWidgets) {
                    dali.hamza.echangecurrencyapp.widgets.WidgetRefreshScheduler.refreshNow(this@ExchangeApplication)
                }
            }
        }

        // FCM (server-push rate alerts) — the Phase 2 registration loop:
        // session ensure (installId → anon JWT) happens inside
        // PushSessionManager.registerDevice, then the current FCM token is
        // POSTed to /alerts/devices. Guarded — builds without
        // google-services.json have no FirebaseApp and the messaging SDK
        // stays inert (see push/SovereignMessagingService).
        // prepare() creates the rate_alerts channel up-front so system-trayed
        // FCM pushes land on the right channel, not the FCM fallback.
        runCatching { GlobalContext.get().get<LocalNotifier>().prepare() }
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (BuildConfig.DEBUG) {
                        Napier.i(tag = FCM_TAG) { "FCM registration token: $token" }
                    }
                    // every launch, no human involved — idempotent upsert;
                    // self-heals stale sessions / reinstalls / host switches
                    PushRegistration.registerToken(this, token)
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
