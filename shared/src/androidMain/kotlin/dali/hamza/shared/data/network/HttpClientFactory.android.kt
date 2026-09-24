package dali.hamza.shared.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual fun createHttpClient(serverURL: String): HttpClient {
    return HttpClient(OkHttp) {

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("HTTP: $message")
                }
            }
            level = LogLevel.INFO
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                allowStructuredMessageKeys = true
            })
        }
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = serverURL
            }
        }
    }

}