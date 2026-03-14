package dev.alexschiff.scanner.services

import dev.alexschiff.scanner.data.*
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class ScannerServiceTest {

    private val scryfallApi = mockk<ScryfallApi>()
    private val manaPoolApi = mockk<ManaPoolApi>()
    private val scannerService = ScannerService(scryfallApi, manaPoolApi)

    @Test
    fun `should identify arbitrage opportunity`() = runTest {
        val request = ScanRequest(
            targetSets = setOf("M21"),
            minCardValue = "$0.50",
            arbitrageThresholdPercent = 10.0,
            spikeThresholdPercent = 20.0
        )

        val scryfallCard = ScryfallCard("scryfall-id-1", "Test Card", "Core Set 2021")
        val manaPoolCard = ManaPoolCard(
            scryfallId = "scryfall-id-1",
            name = "Test Card",
            setCode = "M21",
            priceMarket = 100.0,
            priceCentsLpPlus = 20.0,
            priceCentsNm = 90.0
        )

        coEvery { scryfallApi.getCardIdsBySet("M21") } returns flowOf(scryfallCard)
        coEvery { manaPoolApi.getMarketData(any()) } returns flowOf(manaPoolCard)

        val result = scannerService.scan(request)

        result.arbitrageOpportunities shouldHaveSize 1
        result.spikeOpportunities.shouldBeEmpty()

        val opportunity = result.arbitrageOpportunities.first()
        opportunity.cardName shouldBe "Test Card"
        opportunity.recentSalesAverage shouldBe "$1.00"
        opportunity.lowestAvailableListing shouldBe "$0.20"
        opportunity.potentialProfit shouldBe "$0.80"
    }

    @Test
    fun `should identify spike opportunity`() = runTest {
        val request = ScanRequest(
            targetSets = setOf("M21"),
            minCardValue = "$0.50",
            arbitrageThresholdPercent = 10.0,
            spikeThresholdPercent = 20.0
        )

        val scryfallCard = ScryfallCard("scryfall-id-1", "Test Card", "Core Set 2021")
        val manaPoolCard = ManaPoolCard(
            scryfallId = "scryfall-id-1",
            name = "Test Card",
            setCode = "M21",
            priceMarket = 120.0,
            priceCentsLpPlus = 11500.0,
            priceCentsNm = 90.0
        )

        coEvery { scryfallApi.getCardIdsBySet("M21") } returns flowOf(scryfallCard)
        coEvery { manaPoolApi.getMarketData(any()) } returns flowOf(manaPoolCard)

        val result = scannerService.scan(request)

        result.arbitrageOpportunities.shouldBeEmpty()
        result.spikeOpportunities shouldHaveSize 1

        val opportunity = result.spikeOpportunities.first()
        opportunity.cardName shouldBe "Test Card"
        opportunity.recentSalesAverage shouldBe "$1.20"
        opportunity.historicalAverage shouldBe "$0.90"
        opportunity.spikePercent shouldBe Math.floor(((120.0 - 90.0) / 90.0 * 100) * 100) / 100.0
    }

    @Test
    fun `should filter out cards below min value`() = runTest {
        val request = ScanRequest(
            targetSets = setOf("M21"),
            minCardValue = "$150.00",
            arbitrageThresholdPercent = 10.0,
            spikeThresholdPercent = 20.0
        )

        val scryfallCard = ScryfallCard("scryfall-id-1", "Test Card", "Core Set 2021")
        val manaPoolCard = ManaPoolCard(
            scryfallId = "scryfall-id-1",
            name = "Test Card",
            setCode = "M21",
            priceMarket = 120.0,
            priceCentsLpPlus = 11500.0,
            priceCentsNm = 9000.0
        )

        coEvery { scryfallApi.getCardIdsBySet("M21") } returns flowOf(scryfallCard)
        coEvery { manaPoolApi.getMarketData(any()) } returns flowOf(manaPoolCard)

        val result = scannerService.scan(request)

        result.arbitrageOpportunities.shouldBeEmpty()
        result.spikeOpportunities.shouldBeEmpty()
    }

    @Test
    fun `should round down spike percent to 2 decimal places`() = runTest {
        val request = ScanRequest(
            targetSets = setOf("M21"),
            minCardValue = "$0.50",
            arbitrageThresholdPercent = 10.0,
            spikeThresholdPercent = 10.0
        )

        val scryfallCard = ScryfallCard("scryfall-id-1", "Spike Card", "Core Set 2021")
        // (1.57 - 1.41) / 1.41 * 100 = 11.3475... -> should be 11.34
        val manaPoolCard = ManaPoolCard(
            scryfallId = "scryfall-id-1",
            name = "Spike Card",
            setCode = "M21",
            priceMarket = 157.0,
            priceCentsNm = 141.0
        )

        coEvery { scryfallApi.getCardIdsBySet("M21") } returns flowOf(scryfallCard)
        coEvery { manaPoolApi.getMarketData(any()) } returns flowOf(manaPoolCard)

        val result = scannerService.scan(request)

        result.spikeOpportunities shouldHaveSize 1
        val opportunity = result.spikeOpportunities.first()
        opportunity.spikePercent shouldBe 11.34
    }
}
