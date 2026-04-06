package dali.hamza.shared.data.network

import io.ktor.client.HttpClient

/**
 * Expect function for creating platform-specific HttpClient
 */
expect fun createHttpClient(serverURL: String): HttpClient

