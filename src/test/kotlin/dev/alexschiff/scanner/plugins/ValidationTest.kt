package dev.alexschiff.scanner.plugins

import dev.alexschiff.scanner.data.ScanRequest
import io.ktor.server.plugins.requestvalidation.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ValidationTest {

    @Test
    fun `valid ScanRequest should pass validation`() {
        val request = ScanRequest(
            targetSets = setOf("M21"),
            minCardValue = "$10.00",
            arbitrageThresholdPercent = 10.0,
            spikeThresholdPercent = 20.0
        )
        val result = validate(request)
        result shouldBe ValidationResult.Valid
    }

    @Test
    fun `ScanRequest with empty target_sets should fail validation`() {
        val request = ScanRequest(
            targetSets = emptySet(),
            minCardValue = "$10.00",
            arbitrageThresholdPercent = 10.0,
            spikeThresholdPercent = 20.0
        )
        val result = validate(request)
        result as ValidationResult.Invalid
        result.reasons.first() shouldBe "target_sets must not be empty."
    }

    @Test
    fun `ScanRequest with invalid set code length should fail validation`() {
        val request = ScanRequest(
            targetSets = setOf("M21", "LONG"),
            minCardValue = "$10.00",
            arbitrageThresholdPercent = 10.0,
            spikeThresholdPercent = 20.0
        )
        val result = validate(request)
        result as ValidationResult.Invalid
        result.reasons.first() shouldBe "All set codes in target_sets must be exactly 3 characters long."
    }

    @Test
    fun `ScanRequest with invalid min_card_value format should fail validation`() {
        val request = ScanRequest(
            targetSets = setOf("M21"),
            minCardValue = "10.00",
            arbitrageThresholdPercent = 10.0,
            spikeThresholdPercent = 20.0
        )
        val result = validate(request)
        result as ValidationResult.Invalid
        result.reasons.first() shouldBe "min_card_value must match the format '$#.##'."
    }

    @Test
    fun `ScanRequest with non-positive arbitrage_threshold_percent should fail validation`() {
        val request = ScanRequest(
            targetSets = setOf("M21"),
            minCardValue = "$10.00",
            arbitrageThresholdPercent = -10.0,
            spikeThresholdPercent = 20.0
        )
        val result = validate(request)
        result as ValidationResult.Invalid
        result.reasons.first() shouldBe "arbitrage_threshold_percent must be a positive number."
    }

    @Test
    fun `ScanRequest with non-positive spike_threshold_percent should fail validation`() {
        val request = ScanRequest(
            targetSets = setOf("M21"),
            minCardValue = "$10.00",
            arbitrageThresholdPercent = 10.0,
            spikeThresholdPercent = 0.0
        )
        val result = validate(request)
        result as ValidationResult.Invalid
        result.reasons.first() shouldBe "spike_threshold_percent must be a positive number."
    }

    private fun validate(request: ScanRequest): ValidationResult {
        val currencyRegex = Regex("""^\$\d+\.\d{2}$""")
        if (request.targetSets.isEmpty()) {
            return ValidationResult.Invalid("target_sets must not be empty.")
        }
        if (request.targetSets.any { it.length != 3 }) {
            return ValidationResult.Invalid("All set codes in target_sets must be exactly 3 characters long.")
        }
        if (!request.minCardValue.matches(currencyRegex)) {
            return ValidationResult.Invalid("min_card_value must match the format '$#.##'.")
        }
        if (request.arbitrageThresholdPercent <= 0) {
            return ValidationResult.Invalid("arbitrage_threshold_percent must be a positive number.")
        }
        if (request.spikeThresholdPercent <= 0) {
            return ValidationResult.Invalid("spike_threshold_percent must be a positive number.")
        }
        return ValidationResult.Valid
    }
}