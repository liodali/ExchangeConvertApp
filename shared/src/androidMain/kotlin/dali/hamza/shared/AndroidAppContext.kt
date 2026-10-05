package dali.hamza.shared

import android.content.Context
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

/**
 * Holds the Android [Application] context so the shared KMP `actual`s
 * (`createSessionStorage()`, `createDatabaseDriver()`) can resolve without
 * changing their common signatures. Set from the platform Application's
 * [Application.onCreate] BEFORE the shared Koin module is loaded.
 *
 * The assignment also plants Napier's debug sink — the shared module logs
 * (rate-alert engine, HTTP client) go to logcat from the very first call.
 */
object AndroidAppContext {
    @Volatile
    var appContext: Context? = null
        set(value) {
            field = value
            if (value != null) runCatching { Napier.base(DebugAntilog()) }
        }

    /** Current foreground activity — the biometric prompt needs one. */
    @Volatile
    var currentActivity: androidx.fragment.app.FragmentActivity? = null
}
