package dev.alexschiff.scanner.services

import dev.alexschiff.scanner.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import kotlin.math.floor

class ScannerService(
    private val scryfallApi: ScryfallApi,
    private val manaPoolApi: ManaPoolApi
) {
    private val logger = LoggerFactory.getLogger(ScannerService::class.java)

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun scan(request: ScanRequest): ScanResult = coroutineScope {
        logger.info("Breakpoint: ScannerService.scan started for sets: {}", request.targetSets)

        val minCardValueCents = parseCurrency(request.minCardValue)

        val filteredManaPoolCardsFlow = request.targetSets.asFlow()
            .flatMapMerge { setCode -> scryfallApi.getCardIdsBySet(setCode) }
            .map { it.id }
            .let { manaPoolApi.getMarketData(it) }
            .filter { (it.priceMarket ?: 0.0) >= minCardValueCents }

        val arbitrageOpportunities = mutableSetOf<ArbitrageOpportunity>()
        val spikeOpportunities = mutableSetOf<SpikeOpportunity>()

        filteredManaPoolCardsFlow.collect { card ->
            val recentSalesAverage = card.priceMarket ?: return@collect
            val lowestAvailableListing = (card.priceCentsLpPlus ?: 0.0)
            val historicalAverage = (card.priceCentsNm ?: 0.0)

            // Arbitrage check
            if (lowestAvailableListing > 0.0) {
                val arbitrageThreshold = recentSalesAverage * (request.arbitrageThresholdPercent / 100)
                if (lowestAvailableListing < recentSalesAverage - arbitrageThreshold) {
                    arbitrageOpportunities.add(
                        ArbitrageOpportunity(
                            cardName = card.name,
                            setName = card.setCode,
                            recentSalesAverage = formatCents(recentSalesAverage),
                            lowestAvailableListing = formatCents(lowestAvailableListing),
                            potentialProfit = formatCents(recentSalesAverage - lowestAvailableListing)
                        )
                    )
                }
            }

            // Spike check
            if (historicalAverage > 0.0) {
                val spikeThreshold = historicalAverage * (request.spikeThresholdPercent / 100)
                if (recentSalesAverage > historicalAverage + spikeThreshold) {
                    val spikePercent = (recentSalesAverage - historicalAverage) / historicalAverage * 100
                    spikeOpportunities.add(
                        SpikeOpportunity(
                            cardName = card.name,
                            setName = card.setCode,
                            recentSalesAverage = formatCents(recentSalesAverage),
                            historicalAverage = formatCents(historicalAverage),
                            spikePercent = floor(spikePercent * 100) / 100.0
                        )
                    )
                }
            }
        }

        ScanResult(
            arbitrageOpportunities = arbitrageOpportunities,
            spikeOpportunities = spikeOpportunities
        )
    }

    private fun formatCents(cents: Double): String {
        val dollars = cents / 100.0
        return "$%.2f".format(dollars)
    }

    private fun parseCurrency(currency: String): Double {
        return currency.substring(1).toDouble() * 100.0
    }
}
