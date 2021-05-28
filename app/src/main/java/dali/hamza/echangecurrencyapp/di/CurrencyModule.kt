package dali.hamza.echangecurrencyapp.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.components.ViewModelComponent
import dali.hamza.core.datasource.db.AppDB
import dali.hamza.core.datasource.db.dao.CurrencyDao
import dali.hamza.core.datasource.db.dao.HistoricRateDao
import dali.hamza.core.datasource.db.dao.RatesCurrencyDao
import dali.hamza.core.repository.CurrencyRepository
import dali.hamza.domain.repository.IRepository

@Module
@InstallIn(
    ActivityComponent::class,
    FragmentComponent::class,
    ViewModelComponent::class
)
object CurrencyModule {

    @Provides
    fun provideCurrencyDao(db: AppDB): CurrencyDao = db.CurrencyDao()


    @Provides
    fun provideHistoricCurrencyDao(db: AppDB): HistoricRateDao = db.HistoricRateDao()

    @Provides
    fun provideRatesCurrencyDao(db: AppDB): RatesCurrencyDao = db.RatesCurrencyDao()


    @Provides
    fun provideCurrencyRepository(repository: CurrencyRepository): IRepository =
        repository
}