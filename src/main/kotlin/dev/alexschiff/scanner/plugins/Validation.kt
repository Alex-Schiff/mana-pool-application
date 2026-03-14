package dev.alexschiff.scanner.plugins

import dev.alexschiff.scanner.data.ScanRequest
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*

fun Application.configureValidation() {
    install(RequestValidation) {
        validate<ScanRequest> { request ->
            val currencyRegex = Regex("""^\$\d+\.\d{2}$""")
            if (request.targetSets.isEmpty()) {
                ValidationResult.Invalid("target_sets must not be empty.")
            } else if (request.targetSets.any { it.length != 3 }) {
                ValidationResult.Invalid("All set codes in target_sets must be exactly 3 characters long.")
            } else if (!request.minCardValue.matches(currencyRegex)) {
                ValidationResult.Invalid("min_card_value must match the format '$#.##'.")
            } else if (request.arbitrageThresholdPercent <= 0) {
                ValidationResult.Invalid("arbitrage_threshold_percent must be a positive number.")
            } else if (request.spikeThresholdPercent <= 0) {
                ValidationResult.Invalid("spike_threshold_percent must be a positive number.")
            } else {
                ValidationResult.Valid
            }
        }
    }
}