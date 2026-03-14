package dev.alexschiff.scanner

import io.ktor.server.plugins.calllogging.*
import org.slf4j.event.Level
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.ktor.server.plugins.statuspages.*
import io.ktor.http.*
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.response.*
import dev.alexschiff.scanner.plugins.configureRouting
import dev.alexschiff.scanner.plugins.configureDI
import dev.alexschiff.scanner.plugins.configureValidation
import dev.alexschiff.scanner.plugins.configureOpenAPI

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(CallLogging) {
        level = Level.INFO
    }
    configureDI()
    configureValidation()
    configureOpenAPI()
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respondText(cause.reasons.joinToString(), status = HttpStatusCode.BadRequest)
        }
    }
    configureRouting()
}
