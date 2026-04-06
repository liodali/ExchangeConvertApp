package dali.hamza.shared.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual fun createDatabaseDriver(): SqlDriver {
    return NativeSqliteDriver(
        schema = AppDatabase.Schema,
        name = "currency_database.db"
    )
}
