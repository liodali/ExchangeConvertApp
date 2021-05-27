package dali.hamza.echangecurrencyapp.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(
    ActivityComponent::class,
    ViewModelComponent::class
)
object AppModule {
}