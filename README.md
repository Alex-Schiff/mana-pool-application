# Mana Pool Arbitrage & Spike Scanner

## Overview
The Mana Pool Arbitrage & Spike Scanner is a stateless backend service built with Kotlin and Ktor. It acts as a market analysis tool for Magic: The Gathering, specifically designed to find discrepancies between current market listings and recent sales data on Mana Pool's website.

The application orchestrates concurrent, non-blocking REST calls across two external APIs:
1. **Scryfall API:** Resolves standard MTG set codes into distinct Scryfall card IDs.
2. **Mana Pool API:** Fetches the latest pricing and sales data for those specific IDs.

By comparing the `lowest_available_listing` against the `recent_sales_average` and historical trends, the scanner identifies **Arbitrage** opportunities (undervalued listings) and market **Spikes** (surging sales averages).

## How to Run (Using Docker)
This project uses the Jib Gradle plugin to assemble the Ktor application into a lightweight, daemonless Docker image tarball.

If you have extracted the `jib-image.tar` file from the project archive, you can load and run it directly in your local Docker environment without needing to execute a build step.

1. **Load the image into Docker:**
   ```bash
   docker load -i jib-image.tar
   ```
   *Note: This will output the loaded image name and tag, typically mana-pool-application:0.0.1.*
2. **Run the container:**
   ```bash
   docker run -p 8080:8080 mana-pool-application:0.0.1
   ```
3. **Execute a scan:**
   ```bash
   curl -X POST http://localhost:8080/scan \
     -H "Content-Type: application/json" \
     -d '{
           "target_sets": ["ROE"],
           "min_card_value": 5.0,
           "arbitrage_threshold_percent": 25.0,
           "spike_threshold_percent": 30.0
         }'
   ```
## Future Enhancements & Productionalization
To respect the requested one-hour timebox for this assignment, development was strictly focused on core API orchestration, coroutine concurrency, and deep immutability. If this were a production application, the following enhancements would be immediately targeted for the next sprint:

### Performance & Caching
* **Redis Caching Layer:** The translation of MTG set codes to Scryfall IDs is static data that rarely changes. Introducing a Redis cache for Scryfall API responses would drastically cut down execution time and reduce external rate-limiting risks.
* **Latency Diagnostics:** Current performance under heavy concurrent loads (e.g., scanning massive sets like *Commander Masters*) isn't quite at the target threshold. I would prioritize profiling to diagnose exactly where I/O bottlenecks or JSON deserialization slowdowns are occurring.

### Data Validation
* **Flexible Set Code Validation:** The current request validation enforces 3-character MTG set codes. While this works for most sets, it breaks for some sets (like `PUMA`). The validation rules need to be expanded to accommodate the full range of Scryfall-supported set codes.

### Architecture & Refactoring
* **Domain-Driven Package Structure:** The current `Models.kt` file is a monolith. This should be refactored and separated into distinct files grouped by API boundaries (e.g., `ScryfallModels.kt`, `ManaPoolModels.kt`, and `ScannerModels.kt`).
* **Dependency Management:** Migrate the Gradle build script to use a `libs.versions.toml` Version Catalog for centralized, type-safe dependency updates.

### Testing & CI/CD Tooling
* **Expanded Test Suite:** While the core logic is unit-tested, the repository needs a robust suite of integration tests utilizing Ktor's `MockEngine` or Testcontainers to validate the full HTTP request lifecycle.
* **Static Analysis & Formatting:** Integrate Detekt for code smell identification and Spotless/ktfmt to enforce a uniform Kotlin style guide.
* **Git Hooks & Automation:** Implement pre-commit hooks to automatically format code and validate Git commit messages before pushing. Finally, wire up a GitHub Actions pipeline to run the test suite, build the Jib tarball, and execute Detekt on every pull request.
