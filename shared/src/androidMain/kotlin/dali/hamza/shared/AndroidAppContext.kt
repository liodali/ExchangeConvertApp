package dali.hamza.shared

import android.app.Application
import android.content.Context

/**
 * Holds the Android [Application] context so the shared KMP `actual`s
 * (`createSessionStorage()`, `createDatabaseDriver()`) can resolve without
 * changing their common signatures. Set from the platform Application's
 * [Application.onCreate] BEFORE the shared Koin module is loaded.
 */
object AndroidAppContext {
    @Volatile
    var appContext: Context? = null
}
