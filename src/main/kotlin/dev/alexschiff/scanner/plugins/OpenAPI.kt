package dev.alexschiff.scanner.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*

fun Application.configureOpenAPI() {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi.yaml")
        openAPI(path = "openapi", swaggerFile = "openapi.yaml")
    }
}
