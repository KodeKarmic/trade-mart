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

Run a local dependency stack with Docker Compose (Postgres + Mongo)

1. From repository root start the stack:

```pwsh
docker compose -f specs/002-trade-store-validation/docker-compose.yml up -d
```

2. Start the `trade-store` application pointing at the local services and enable the `dev` profile (dev profile includes a permissive JwtDecoder for convenience):

```pwsh
./gradlew.bat :trade-store:bootRun --no-daemon -Dspring.profiles.active=dev -Dspring.datasource.url=jdbc:postgresql://localhost:5432/trade_store -Dspring.datasource.username=trade -Dspring.datasource.password=tradepass -Dspring.data.mongodb.uri=mongodb://localhost:27017
```

3. Send a sample trade ingestion request (the dev profile accepts a simple bearer token like `valid-token`):

```pwsh
$json = '{"tradeId":"T1","version":1,"price":100.0,"quantity":10,"maturityDate":"2025-10-27"}'
curl -v -H "Content-Type: application/json" -H "Authorization: Bearer valid-token" -d $json http://localhost:8080/trades
```

4. Verify persistence:

- PostgreSQL: connect to `localhost:5432` (db `trade_store`, user `trade`) and ensure the trades table has the new row.
- MongoDB: connect to `mongodb://localhost:27017` and inspect the history collection for an EXPIRE or INGEST history entry depending on the request.

Bring the stack down when done:

```pwsh
docker compose -f specs/002-trade-store-validation/docker-compose.yml down
```

Notes

- Integration tests spin up embedded H2 and may attempt to connect to localhost MongoDB if Testcontainers is not active; ensure Testcontainers or matching services are available for full integration validation.
- The JMH harness is synthetic; use profiling for deeper performance investigation.
