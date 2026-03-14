package dev.alexschiff.scanner.data

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class ManaPoolApiTest {

    @Test
    fun `should hit the correct endpoint with repeated scryfall_ids and language parameters`() = runTest {
        val mockEngine = MockEngine { request ->
            val url = request.url
            val expectedBase = "https://manapool.com/api/v1/products/singles"

            // Check if the URL starts with the expected base (ignoring query params for a moment)
            val actualBase = "${url.protocol.name}://${url.host}${url.encodedPath}"

            if (actualBase == expectedBase &&
                url.parameters.getAll("scryfall_ids") == listOf("id1", "id2") &&
                url.parameters["languages"] == "EN"
            ) {
                respond(
                    content = """{"meta": {"as_of": "2024-03-14T08:58:00Z"}, "data": []}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond(
                    content = "Unexpected request: $url",
                    status = HttpStatusCode.BadRequest
                )
            }
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val api = ManaPoolApi(client)
        val result = api.getMarketData(flowOf("id1", "id2")).toList()

        result.size shouldBe 0
    }
}
