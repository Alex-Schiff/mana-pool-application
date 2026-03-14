package dev.alexschiff.scanner.plugins

import dev.alexschiff.scanner.data.ManaPoolApi
import dev.alexschiff.scanner.data.ScryfallApi
import dev.alexschiff.scanner.services.ScannerService
import dev.alexschiff.scanner.plugins.configureOpenAPI
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.testing.*
import io.ktor.server.response.respondText
import io.ktor.test.dispatcher.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutingTest {

    private val mockEngine = MockEngine { request ->
        when {
            request.url.host.contains("api.scryfall.com") -> {
                respond(
                    content = """
                        {
                            "object": "list",
                            "total_cards": 1,
                            "has_more": false,
                            "data": [
                                {
                                    "object": "card",
                                    "id": "scryfall-id-1",
                                    "name": "Test Card",
                                    "set_name": "Test Set"
                                }
                            ]
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
            request.url.host.contains("manapool.com") -> {
                respond(
                    content = """
                        {
                            "data": [
                                {
                                    "scryfall_id": "scryfall-id-1",
                                    "name": "Test Card",
                                    "set_name": "Test Set",
                                    "recent_sales_average": 100.0,
                                    "lowest_available_listing": 80.0,
                                    "historical_average": 90.0
                                }
                            ]
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
            else -> error("Unhandled ${request.url}")
        }
    }

    private val mockHttpClient = HttpClient(mockEngine) {
        install(ClientContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private val testModule = module {
        single { mockHttpClient }
        single { ScryfallApi(get()) }
        single { ManaPoolApi(get()) }
        single { ScannerService(get(), get()) }
    }

    @Test
    fun `test post scan endpoint with valid request`() = testApplication {
        application {
            install(Koin) {
                modules(testModule)
            }
            install(ServerContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            configureRouting()
            configureValidation()
            configureOpenAPI()
            install(StatusPages) {
                exception<RequestValidationException> { call, cause ->
                    call.respondText(cause.reasons.joinToString(), status = HttpStatusCode.BadRequest)
                }
            }
        }

        val response = client.post("/scan") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "target_sets": ["M21"],
                    "min_card_value": 10.0,
                    "arbitrage_threshold_percent": 10.0,
                    "spike_threshold_percent": 5.0
                }
            """.trimIndent())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        // Further assertions on the body can be added here
    }

    @Test
    fun `test post scan with invalid request`() = testApplication {
        application {
            install(ServerContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            configureRouting()
            configureValidation()
            install(StatusPages) {
                exception<RequestValidationException> { call, cause ->
                    call.respondText(cause.reasons.joinToString(), status = HttpStatusCode.BadRequest)
                }
            }
        }

        val response = client.post("/scan") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "target_sets": [],
                    "min_card_value": -10.0,
                    "arbitrage_threshold_percent": 10.0,
                    "spike_threshold_percent": 20.0
                }
            """.trimIndent())
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}