package dali.hamza.shared.di

import dali.hamza.shared.data.alerts.RateAlertsEngine
import dali.hamza.shared.data.network.CurrencyApi
import dali.hamza.shared.data.network.RateAlertsApi
import dali.hamza.shared.data.network.createHttpClient
import dali.hamza.shared.data.repository.CurrencyRepositoryImpl
import dali.hamza.shared.data.session.PushSessionManager
import dali.hamza.shared.data.storage.createSessionStorage
import dali.hamza.shared.platform.createBiometricAuthenticator
import dali.hamza.shared.platform.createLocalNotifier
import dali.hamza.shared.platform.createRateAlertScheduler
import dali.hamza.shared.database.AppDatabase
import dali.hamza.shared.database.createDatabaseDriver
import dali.hamza.shared.domain.repository.IRepository
import dali.hamza.shared.ui.viewmodel.AccountViewModel
import dali.hamza.shared.ui.viewmodel.HomeViewModel
import dali.hamza.shared.ui.viewmodel.HistoryViewModel
import dali.hamza.shared.ui.viewmodel.RateAlertsViewModel
import dali.hamza.shared.ui.viewmodel.SharedViewModel
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Default backend host (our exchange-api deployment). Platform hosts may
 * override it at build time (Android: `SERVER_HOST` BuildConfig field fed
 * by the `server.host` gradle prop; iOS: `ExchangeSecrets.apiHost`).
 */
const val DEFAULT_HOST = "api.exchange.dev.adetify.com"

/**
 * Shared Koin module (KMP). Wired by [initSharedKoin] from the platform host
 * (Android application / iOS MainViewController).
 */
fun sharedModule(
    serverURL: String = DEFAULT_HOST,
    accessKey: String = "",
) = module {
    single(named("SERVER")) { serverURL }
    single(named("TOKEN")) { accessKey }
    single { createHttpClient(get(named("SERVER"))) }
    single { CurrencyApi(httpClient = get(), accessKey = get(named("TOKEN"))) }
    // server-push alerts (Phase 2): same host/client as CurrencyApi
    single { RateAlertsApi(httpClient = get()) }
    single { PushSessionManager(alertsApi = get(), storage = get()) }
    single { createDatabaseDriver() }
    single { AppDatabase(get()) }
    single { createSessionStorage() }
    single { createBiometricAuthenticator() }
    single { createLocalNotifier() }
    single { createRateAlertScheduler() }
    single {
        RateAlertsEngine(
            repository = get(),
            currencyApi = get(),
            sessionStorage = get(),
            notifier = get(),
        )
    }
    single<IRepository> {
        CurrencyRepositoryImpl(
            currencyApi = get(),
            database = get(),
            sessionStorage = get(),
            alertsApi = get(),
            sessionManager = get(),
        )
    }
    factory { SharedViewModel(get()) }
    factory { AccountViewModel(storage = get(), repository = get()) }
    factory { HistoryViewModel(get()) }
    factory { HomeViewModel(get()) }
    factory { RateAlertsViewModel(get(), get(), get(), get()) }
}

private var koinInstance: Koin? = null

/**
 * Start the shared Koin context. Idempotent: subsequent calls return the
 * existing instance (a token/host change requires an app restart).
 */
fun initSharedKoin(
    serverURL: String = DEFAULT_HOST,
    accessKey: String = "",
): Koin {
    koinInstance?.let { return it }
    val application = startKoin {
        modules(sharedModule(serverURL, accessKey))
    }
    koinInstance = application.koin
    return application.koin
}
