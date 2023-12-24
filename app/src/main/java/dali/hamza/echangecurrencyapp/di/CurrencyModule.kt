package dali.hamza.echangecurrencyapp.di

import dali.hamza.core.common.SessionManager
import dali.hamza.core.datasource.db.AppDB
import dali.hamza.core.datasource.db.dao.CurrencyDao
import dali.hamza.core.datasource.db.dao.HistoricRateDao
import dali.hamza.core.datasource.db.dao.RatesCurrencyDao
import dali.hamza.core.datasource.network.CurrencyClientApi
import dali.hamza.core.repository.CurrencyRepository
import dali.hamza.domain.repository.IRepository
import dali.hamza.echangecurrencyapp.ui.MainActivity
import dali.hamza.echangecurrencyapp.viewmodel.MainViewModel
import dali.hamza.echangecurrencyapp.viewmodel.DialogCurrencyViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.definition.Definition
import org.koin.core.module.dsl.DefinitionOptions
import org.koin.core.qualifier.named
import org.koin.core.qualifier.qualifier
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
        MainViewModel(get<CurrencyRepository>(),get<SessionManager>())
    }
    viewModel {
        DialogCurrencyViewModel(get<CurrencyRepository>(),get<SessionManager>())
    }
}