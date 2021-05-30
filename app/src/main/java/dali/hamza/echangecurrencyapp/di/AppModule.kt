package dali.hamza.echangecurrencyapp.di

import android.app.Application
import androidx.room.Room
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dali.hamza.core.common.SessionManager
import dali.hamza.core.datasource.db.AppDB
import dali.hamza.core.datasource.network.CurrencyClientApi
import dali.hamza.core.datasource.network.converter.CurrencyConverter
import dali.hamza.core.datasource.network.converter.RateConverter
import dali.hamza.echangecurrencyapp.R
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(
    SingletonComponent::class
)
object AppModule {

    @Provides
    @Named("pref")
    fun providePrefName(application: Application) =
        application.resources.getString(R.string.preference_name)

    @Provides
    @Singleton
    fun provideSessionManager(
        @Named("pref") PREF_NAME: String,
        application: Application
    ): SessionManager {
        return SessionManager(PREF_NAME, application)
    }

//    @Provides
//    @Named("token")
//    fun provideTokenApp(application: Application) =
//        application.getString(R.string.token)


    @Provides
    fun provideBaseUrl(application: Application) =
        application.getString(R.string.server)


    @Provides
    fun provideMoshi(): MoshiConverterFactory {
        val moshi = Moshi.Builder()
            .add(CurrencyConverter())
            .add(RateConverter())
            .build()
        return MoshiConverterFactory
            .create(moshi)
    }


    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        BASE_URL: String,
        moshiConverter: MoshiConverterFactory
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(moshiConverter)
        .build()


    @Singleton
    @Provides
    fun provideClientApi(
        retrofit: Retrofit
    ): CurrencyClientApi = retrofit.create(CurrencyClientApi::class.java)


    @Singleton
    @Provides
    fun provideAppDatabase(application: Application): AppDB {
        return Room.databaseBuilder(
            application,
            AppDB::class.java,
            "ConvertCurrencyAPp.db"
        ).build()
    }

}