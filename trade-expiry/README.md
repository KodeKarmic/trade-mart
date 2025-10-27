# Trade Expiry Service

Standalone Spring Boot module that encapsulates trade maturity validation and scheduled expiry processing.

Responsibilities:

- Owns the Trade JPA entity and repository
- Owns Trade history Mongo document + repository
- Provides maturity validation component
- Runs scheduled expiry job (marks ACTIVE trades whose maturityDate < today UTC as EXPIRED)

Run locally:

```bash
./gradlew :trade-expiry:bootRun
```

Configuration via environment variables (fallback defaults shown):

```bash
EXPIRY_DB_URL=jdbc:postgresql://localhost:5432/trades
EXPIRY_DB_USER=postgres
EXPIRY_DB_PASSWORD=postgres
EXPIRY_MONGO_URI=mongodb://localhost:27017/trade_history
```

Prometheus metrics: /actuator/prometheus (port 8091)

Future improvements:

- REST endpoint to trigger on-demand expiry
- gRPC / messaging bridge back to trade-store
- Distributed lock for multi-instance scheduling
