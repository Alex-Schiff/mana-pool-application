package dev.alexschiff.scanner.data

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import io.ktor.client.request.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class ScryfallApiTest {

    @Test
    fun `should include required headers in requests`() = runTest {
        val mockEngine = MockEngine { request ->
            val userAgent = request.headers[HttpHeaders.UserAgent]
            val accept = request.headers[HttpHeaders.Accept]

            if (userAgent == "ManaPoolScannerApp/0.0.1" && accept == "application/json") {
                respond(
                    content = """{"has_more": false, "data": []}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond(
                    content = "Missing headers",
                    status = HttpStatusCode.BadRequest
                )
            }
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val api = ScryfallApi(client)

        // This should not throw an exception and should succeed if headers are present
        val results = api.getCardIdsBySet("M21").toList()
        results.size shouldBe 0
    }
}
