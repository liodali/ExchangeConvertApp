package dali.hamza.shared.di

import dali.hamza.shared.data.network.CurrencyApi
import dali.hamza.shared.data.network.createHttpClient
import dali.hamza.shared.data.repository.CurrencyRepositoryImpl
import dali.hamza.shared.data.storage.createSessionStorage
import dali.hamza.shared.database.AppDatabase
import dali.hamza.shared.database.createDatabaseDriver
import dali.hamza.shared.domain.repository.IRepository
import dali.hamza.shared.ui.viewmodel.SharedViewModel
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val DEFAULT_HOST = "api.exchangerate.host"

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
    single { createDatabaseDriver() }
    single { AppDatabase(get()) }
    single { createSessionStorage() }
    single<IRepository> {
        CurrencyRepositoryImpl(
            currencyApi = get(),
            database = get(),
            sessionStorage = get()
        )
    }
    factory { SharedViewModel(get()) }
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
