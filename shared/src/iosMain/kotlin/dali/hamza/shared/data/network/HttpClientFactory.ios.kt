package dali.hamza.shared.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.URLProtocol

actual fun createHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        configureWithDefaults()
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
            }
        }
    }
}
