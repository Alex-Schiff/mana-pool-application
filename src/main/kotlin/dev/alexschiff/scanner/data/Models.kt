package dev.alexschiff.scanner.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScanRequest(
    @SerialName("target_sets") val targetSets: Set<String>,
    @SerialName("min_card_value") val minCardValue: String,
    @SerialName("arbitrage_threshold_percent") val arbitrageThresholdPercent: Double,
    @SerialName("spike_threshold_percent") val spikeThresholdPercent: Double
)

@Serializable
data class ArbitrageOpportunity(
    @SerialName("card_name") val cardName: String,
    @SerialName("set_name") val setName: String,
    @SerialName("recent_sales_average") val recentSalesAverage: String,
    @SerialName("lowest_available_listing") val lowestAvailableListing: String,
    @SerialName("potential_profit") val potentialProfit: String
)

@Serializable
data class SpikeOpportunity(
    @SerialName("card_name") val cardName: String,
    @SerialName("set_name") val setName: String,
    @SerialName("recent_sales_average") val recentSalesAverage: String,
    @SerialName("historical_average") val historicalAverage: String,
    @SerialName("spike_percent") val spikePercent: Double
)

@Serializable
data class ScanResult(
    @SerialName("arbitrage_opportunities") val arbitrageOpportunities: Set<ArbitrageOpportunity>,
    @SerialName("spike_opportunities") val spikeOpportunities: Set<SpikeOpportunity>
)

@Serializable
data class ScryfallCard(
    val id: String,
    val name: String,
    @SerialName("set_name") val setName: String
)

@Serializable
data class ScryfallSearchResponse(
    @SerialName("has_more") val hasMore: Boolean,
    @SerialName("next_page") val nextPage: String? = null,
    val data: List<ScryfallCard>
)

@Serializable
data class ManaPoolCard(
    @SerialName("scryfall_id") val scryfallId: String,
    val name: String,
    @SerialName("set_code") val setCode: String,
    @SerialName("price_market") val priceMarket: Double? = null,
    @SerialName("price_cents_lp_plus") val priceCentsLpPlus: Double? = null,
    @SerialName("price_cents_nm") val priceCentsNm: Double? = null
)

@Serializable
data class ManaPoolMeta(
    @SerialName("as_of") val asOf: String
)

@Serializable
data class ManaPoolResponse(
    val meta: ManaPoolMeta,
    val data: List<ManaPoolCard>
)
