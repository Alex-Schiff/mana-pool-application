package dev.alexschiff.scanner.data

import dev.alexschiff.scanner.data.ScryfallSearchResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ScryfallApi(private val client: HttpClient) {

    private companion object {
        const val USER_AGENT = "ManaPoolScannerApp/0.0.1"
    }

    private suspend fun searchCards(url: String): ScryfallSearchResponse {
        return client.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }.body()
    }

    fun getCardIdsBySet(setCode: String): Flow<ScryfallCard> = flow {
        var nextUrl: String? = "https://api.scryfall.com/cards/search?q=set:$setCode"
        while (nextUrl != null) {
            val response = searchCards(nextUrl)
            response.data.forEach { emit(it) }
            nextUrl = response.nextPage
        }
    }
}
