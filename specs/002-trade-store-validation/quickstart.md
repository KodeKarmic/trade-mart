# Quickstart — Trade Store Validation

This quickstart shows minimal commands to validate the `trade-store` module locally.

Prerequisites

- Java 17 installed
- Gradle wrapper available (repo includes `gradlew`)

Run unit and integration tests for `trade-store`:

```pwsh
./gradlew.bat :trade-store:test
```

Run the JMH performance harness (optional — can be slow):

```pwsh
./gradlew.bat :trade-store:jmh
```

Where outputs appear

- Unit test HTML report: `trade-store/build/reports/tests/test`
- JMH results: `trade-store/build/results/jmh/results.txt`

Notes

- Integration tests spin up embedded H2 and may attempt to connect to localhost MongoDB if Testcontainers is not active; ensure Testcontainers or matching services are available for full integration validation.
- The JMH harness is synthetic; use profiling for deeper performance investigation.
