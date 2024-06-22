package dali.hamza.echangecurrencyapp.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.squareup.moshi.Moshi
import dali.hamza.core.common.ISessionManager
import dali.hamza.core.common.SessionManager
import dali.hamza.core.datasource.network.CurrencyClientApi
import dali.hamza.core.datasource.network.converter.RateConverter
import dali.hamza.echangecurrencyapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import mohamedali.hamza.database.AppDB
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory


val appModule = module {
    single(named("DB")) {
        androidContext().resources.getString(R.string.db_name)
    }
    single(named("PREF")) {
        androidContext().resources.getString(R.string.preference_name)
    }
    single(named("EXCHANGE_SERVER")) {
        androidContext().resources.getString(R.string.server)
    }
    single(named("TOKEN")) {
        androidContext().resources.getString(R.string.token)
    }
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { androidContext().preferencesDataStoreFile(get(named("PREF"))) }
        )
    }
    single<ISessionManager> {
        SessionManager(get<DataStore<Preferences>>())
    }
    single {
        MoshiConverterFactory
            .create(
                Moshi.Builder()
                    //.add(CurrencyConverter())
                    .add(RateConverter())
                    .build()
            )
    }
    single {
        OkHttpClient
            .Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                this.level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }
    single {
        Retrofit.Builder()
            .baseUrl(get<String>(named("EXCHANGE_SERVER")))
            .client(get<OkHttpClient>())
            .addConverterFactory(get<MoshiConverterFactory>())

            .build()
    }
    single<CurrencyClientApi> {
        get<Retrofit>().create(CurrencyClientApi::class.java)
    }
    single<AppDB> {
        Room.databaseBuilder(
            androidContext(),
            AppDB::class.java,
            get<String>(named("DB"))
        ).build()
    }
    includes(coreModule, currencyConverterModule)
}
/*
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
    @Named("dbname")
    fun provideDbName(application: Application) =
        application.resources.getString(R.string.db_name)


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
    fun provideAppDatabase(
        application: Application,
        @Named("dbname") DB_NAME:String,
    ): AppDB {
        return Room.databaseBuilder(
            application,
            AppDB::class.java,
            DB_NAME
        ).build()
    }

}*/