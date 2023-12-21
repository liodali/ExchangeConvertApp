package dali.hamza.echangecurrencyapp

import android.app.Application
import dali.hamza.echangecurrencyapp.di.appModule
import dali.hamza.echangecurrencyapp.di.coreModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class ExchangeApplication :Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            // Reference Android context
            androidContext(this@ExchangeApplication)
            // Load modules
            modules(appModule,coreModule)
        }

    }

}