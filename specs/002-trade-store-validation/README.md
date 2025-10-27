# Trade Store Validation — Feature README

This document summarizes the work performed for the `specs/002-trade-store-validation` feature and explains how to run the project's tests and the small performance harness added during the implementation.

What I implemented (high level)
- Project-level progress tracked in `tasks.md` (phase tasks updated).
- A small JMH harness exists under `trade-store/src/jmh` and can be executed with the Gradle `:trade-store:jmh` task.
- A lightweight unit test was added to validate the edge case where a trade's maturity date equals "today" (the system should not treat that as before today).
- OWASP Dependency-Check vulnerability scanning integrated with GitHub Actions workflow.

Quickstart — run unit tests for the `trade-store` module

From the repository root:

```pwsh
./gradlew.bat :trade-store:test
```

Run the JMH benchmark (note: this can be heavy; Gradle will produce results in `trade-store/build/results/jmh`):

```pwsh
./gradlew.bat :trade-store:jmh
```

Run OWASP Dependency-Check vulnerability scan:

```pwsh
# First-time setup: Obtain a free NVD API key from https://nvd.nist.gov/developers/request-an-api-key

# Option 1: Set environment variable
$env:NVD_API_KEY="your-api-key-here"
./gradlew.bat :trade-store:dependencyCheckAnalyze

# Option 2: Pass as Gradle property
./gradlew.bat :trade-store:dependencyCheckAnalyze -Pnvd.api.key=your-api-key-here

# Reports generated at:
# - trade-store/build/reports/dependency-check-report.html
# - trade-store/build/reports/dependency-check-report.json
```

**Note:** The NVD API key is required for downloading the latest CVE data. Without it, the scan will use cached data (if available) or fail. The build is configured with `failOnError=false` to allow scans to complete with cached data when the NVD API is unavailable.

Where to look for results and reports

- Unit test reports: `trade-store/build/reports/tests/test`
- JMH results: `trade-store/build/results/jmh/results.txt` (and console capture `build/jmh-run.log`)
- Vulnerability scan: `trade-store/build/reports/dependency-check-report.html`

JMH run summary (latest)

- Benchmark: `com.trademart.perf.TradeExpiryBenchmark.checkExpiryLoop`
- Mode: Throughput (ops/s)
- Average (measured): ~159,344 ops/s (99.9% CI: [147,063 .. 171,626])
- Result file: `trade-store/build/results/jmh/results.txt`
- Notes: the benchmark is synthetic and measures a simple LocalDate-based expiry loop; use a profiler and larger factorial experiments before drawing production conclusions.

Configuration cache note

- The JMH run completed but the Gradle build logged a configuration cache serialization warning at the end (does not affect the benchmark results) pointing at `:trade-store:jmhJar`. This is a build-system artifact and does not change runtime results; if desired we can adjust the Gradle config to avoid storing problematic objects in the configuration cache.

Notes and next steps

- Phase 6 still has outstanding items: code cleanup, security hardening, CI vulnerability scan and quickstart validation. I added a minimal CI workflow to run the test suite (see `.github/workflows/ci.yml`) to ensure tests run on PRs.
- If you'd like, I can convert the simplified unit test into a Spring-backed repository/integration test (requires Testcontainers or running local Mongo/Postgres). Ask and I'll implement that next.

Date: 2025-10-27
