package dali.hamza.echangecurrencyapp.di

import dali.hamza.core.common.SessionManager
import dali.hamza.core.datasource.network.CurrencyClientApi
import dali.hamza.core.repository.CurrencyRepository
import dali.hamza.echangecurrencyapp.viewmodel.DialogCurrencyViewModel
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel
import mohamedali.hamza.database.AppDB
import mohamedali.hamza.database.dao.CurrencyDao
import mohamedali.hamza.database.dao.HistoricRateDao
import mohamedali.hamza.database.dao.RatesCurrencyDao
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module


val coreModule = module {
    single<CurrencyDao> {
        val database = get<AppDB>()
        database.CurrencyDao()
    }
    single<HistoricRateDao> {
        val database = get<AppDB>()
        database.HistoricRateDao()
    }
    single<RatesCurrencyDao> {
        val database = get<AppDB>()
        database.RatesCurrencyDao()
    }
    single<CurrencyRepository> {
        CurrencyRepository(
            currencyClientAPI = get<CurrencyClientApi>(),
            currencyDao = get<CurrencyDao>(),
            ratesCurrencyDao = get<RatesCurrencyDao>(),
            historicRateDao = get<HistoricRateDao>(),
            sessionManager = get<SessionManager>(),
            tokenAPI = get(named("TOKEN"))
        )
    }

    viewModel {
        MainViewModel(get<CurrencyRepository>(), get<SessionManager>())
    }
    viewModel {
        DialogCurrencyViewModel(get<CurrencyRepository>(), get<SessionManager>())
    }
}