package dali.hamza.shared.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Expect function for creating platform-specific SqlDriver
 */
expect fun createDatabaseDriver(): SqlDriver
