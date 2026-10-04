package dali.hamza.shared.data.storage

/**
 * Expect function for creating a platform-specific [ISessionStorage].
 *
 * Note (Android): the no-arg expect mirrors `createDatabaseDriver()` —
 * the Android actual requires a `Context`, use `createSessionStorage(context)`
 * from the Android host until the shared module is wired into the app module.
 */
expect fun createSessionStorage(): ISessionStorage
