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

Start the stack:

```pwsh
docker compose -f specs/002-trade-store-validation/docker-compose.yml up -d
```

Start the `trade-store` application pointing at the local services and enable the `dev` profile (dev profile includes a permissive JwtDecoder for convenience):

**Note**: Set `JAVA_TOOL_OPTIONS` to force UTC timezone (avoids PostgreSQL timezone issues):

```pwsh
$env:JAVA_TOOL_OPTIONS='-Duser.timezone=UTC'
./gradlew.bat :trade-store:bootRun --args='--spring.profiles.active=dev --spring.datasource.url=jdbc:postgresql://localhost:5432/trade_store --spring.datasource.username=trade --spring.datasource.password=tradepass --spring.data.mongodb.uri=mongodb://localhost:27017/trade_history'
```

Wait for the app to start (watch for "Started TradeStoreApplication" in the console), then send a sample trade ingestion request:

```pwsh
curl.exe -v -H "Content-Type: application/json" -H "Authorization: Bearer valid-token" --data-binary "@trade-sample.json" http://localhost:8080/trades
```

Where `trade-sample.json` contains:

```json
{
  "tradeId": "T1",
  "version": 1,
  "price": 100.0,
  "quantity": 10,
  "maturityDate": "2025-12-31"
}
```

Expected response: `HTTP/1.1 201 Created` with `Location: /trades/T1`.

Verify persistence:

- PostgreSQL: connect to `localhost:5432` (db `trade_store`, user `trade`, password `tradepass`) and query the `trades` table for `trade_id='T1'`.
- MongoDB: connect to `mongodb://localhost:27017/trade_history` and inspect the `tradeHistory` collection for an INGEST entry.

Bring the stack down when done:

```pwsh
docker compose -f specs/002-trade-store-validation/docker-compose.yml down
```

End-to-end validation results (2025-10-27):

- Docker Compose brought up Postgres 14 and Mongo 6 successfully.
- App started successfully with dev profile, connected to both DBs.
- POST `/trades` with valid JWT returned HTTP 201 Created.
- Trade persisted to PostgreSQL and history written to MongoDB (verified in logs).

Notes

- Integration tests spin up embedded H2 and may attempt to connect to localhost MongoDB if Testcontainers is not active; ensure Testcontainers or matching services are available for full integration validation.
- The JMH harness is synthetic; use profiling for deeper performance investigation.
