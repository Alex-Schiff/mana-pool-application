package dev.alexschiff.scanner.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory

class ManaPoolApi(private val client: HttpClient) {

    private val logger = LoggerFactory.getLogger(ManaPoolApi::class.java)

    private companion object {
        const val API_BASE_URL = "https://manapool.com/api/v1"
        const val CHUNK_SIZE = 100
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMarketData(scryfallIds: Flow<String>): Flow<ManaPoolCard> = flow {
        logger.info("Breakpoint: ManaPoolApi.getMarketData called")

        val chunkedFlow = flow {
            val chunk = mutableListOf<String>()
            scryfallIds.collect { id ->
                chunk.add(id)
                if (chunk.size >= CHUNK_SIZE) {
                    emit(chunk.toList())
                    chunk.clear()
                }
            }
            if (chunk.isNotEmpty()) {
                emit(chunk.toList())
            }
        }

        chunkedFlow.flatMapMerge { chunk ->
            flow {
                logger.info("Breakpoint: Processing chunk of size: {}", chunk.size)
                val response: ManaPoolResponse = client.get("$API_BASE_URL/products/singles") {
                    url {
                        chunk.forEach { id ->
                            parameters.append("scryfall_ids", id)
                        }
                        parameters.append("languages", "EN")
                    }
                }.body()
                response.data.forEach { emit(it) }
            }
        }.collect { emit(it) }
    }
}