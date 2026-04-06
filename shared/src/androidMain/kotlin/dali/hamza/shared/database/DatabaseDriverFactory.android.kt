package dali.hamza.shared.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual fun createDatabaseDriver(): SqlDriver {
    // This will be called from Android platform
    // The context should be provided from Android side
    throw IllegalStateException("Use createDatabaseDriver(context) for Android")
}

fun createDatabaseDriver(context: Context): SqlDriver {
    return AndroidSqliteDriver(
        schema = AppDatabase.Schema,
        context = context,
        name = "currency_database.db"
    )
}
