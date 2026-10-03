package dali.hamza.shared.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual fun createDatabaseDriver(): SqlDriver {
    val context = dali.hamza.shared.AndroidAppContext.appContext
        ?: throw IllegalStateException("Set AndroidAppContext.appContext from Application.onCreate() first")
    return createDatabaseDriver(context)
}

fun createDatabaseDriver(context: Context): SqlDriver {
    return AndroidSqliteDriver(
        schema = AppDatabase.Schema,
        context = context,
        name = "currency_database.db"
    )
}
