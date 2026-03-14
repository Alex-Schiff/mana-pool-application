package dev.alexschiff.scanner.plugins

import dev.alexschiff.scanner.data.ManaPoolApi
import dev.alexschiff.scanner.data.ScryfallApi
import dev.alexschiff.scanner.services.ScannerService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

val appModule = module {
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    single { ScryfallApi(get()) }
    single { ManaPoolApi(get()) }
    single { ScannerService(get(), get()) }
}

fun Application.configureDI() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}
