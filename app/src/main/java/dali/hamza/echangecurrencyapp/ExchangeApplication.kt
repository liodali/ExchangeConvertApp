package dali.hamza.echangecurrencyapp

import android.app.Application
import dali.hamza.echangecurrencyapp.di.appModule
import dali.hamza.shared.AndroidAppContext
import dali.hamza.shared.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.loadKoinModules


class ExchangeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // shared KMP module: context first (storage/driver actuals need it)
        AndroidAppContext.appContext = this

        startKoin {
            // Reference Android context
            androidContext(this@ExchangeApplication)
            // Load modules
            modules(appModule)
        }
        // shared module rides on the same Koin instance (backend host
        // defaults to api.exchange.dev.adetify.com inside sharedModule)
        loadKoinModules(sharedModule())
    }
}
