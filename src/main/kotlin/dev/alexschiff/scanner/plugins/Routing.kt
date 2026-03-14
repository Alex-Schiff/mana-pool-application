package dev.alexschiff.scanner.plugins

import dev.alexschiff.scanner.data.ScanRequest
import dev.alexschiff.scanner.services.ScannerService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.java.KoinJavaComponent.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("dev.alexschiff.scanner.plugins.Routing")

fun Application.configureRouting() {
    val scannerService by inject<ScannerService>(ScannerService::class.java)
    routing {
        post("/scan") {
            logger.info("Breakpoint: Received POST /scan request")
            val request = call.receive<ScanRequest>()
            logger.info("Breakpoint: Parsed request: {}", request)
            val result = scannerService.scan(request)
            logger.info("Breakpoint: Scan completed successfully")
            call.respond(result)
        }
    }
}
