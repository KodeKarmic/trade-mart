# Trade Store Validation — Feature README

This document summarizes the work performed for the `specs/002-trade-store-validation` feature and explains how to run the project's tests and the small performance harness added during the implementation.

What I implemented (high level)
- Project-level progress tracked in `tasks.md` (phase tasks updated).
- A small JMH harness exists under `trade-store/src/jmh` and can be executed with the Gradle `:trade-store:jmh` task.
- A lightweight unit test was added to validate the edge case where a trade's maturity date equals "today" (the system should not treat that as before today).

Quickstart — run unit tests for the `trade-store` module

From the repository root:

```pwsh
./gradlew.bat :trade-store:test
```

Run the JMH benchmark (note: this can be heavy; Gradle will produce results in `trade-store/build/results/jmh`):

```pwsh
./gradlew.bat :trade-store:jmh
```

Where to look for results and reports
- Unit test reports: `trade-store/build/reports/tests/test`
- JMH results: `trade-store/build/results/jmh/results.txt` (and console capture `build/jmh-run.log`)

Notes and next steps
- Phase 6 still has outstanding items: code cleanup, security hardening, CI vulnerability scan and quickstart validation. I added a minimal CI workflow to run the test suite (see `.github/workflows/ci.yml`) to ensure tests run on PRs.
- If you'd like, I can convert the simplified unit test into a Spring-backed repository/integration test (requires Testcontainers or running local Mongo/Postgres). Ask and I'll implement that next.

Date: 2025-10-27
