```markdown
# Feature Specification: Trade Store Validation

**Feature Branch**: `002-trade-store-validation`
**Created**: 2025-10-21
**Status**: Draft

## User Scenarios & Testing (mandatory)

### User Story 1 - Trade Ingestion and Sequencing (Priority: P1)

Trades are received by the store in real-time and are organized and stored in a specific sequence.

Independent Test: Send a batch of trades and verify they are stored in the correct sequence.

Acceptance Scenarios:

1. Given the store is running, when trades are received, then they are stored in the correct sequence.
2. Given trades are received out of order, when processed, then the store organizes them in the correct sequence.

---

### User Story 2 - Trade Version Validation (Priority: P2)

When a trade is received, the store checks its version against the existing record. Lower versions are rejected; same-version trades replace the record.

Independent Test: Submit trades with varying versions and verify correct acceptance, rejection, or replacement.

Acceptance Scenarios:

1. Given a trade with a lower version is received, when processed, then the store rejects it and generates an exception.
2. Given a trade with the same version is received, when processed, then it replaces the current record.

---

### User Story 3 - Maturity Date Validation and Expiry (Priority: P3)

The store rejects trades with a maturity date earlier than today and marks trades expired when maturity is surpassed.

Independent Test: Submit trades with various maturity dates and verify correct rejection or expiry marking.

Acceptance Scenarios:

1. Given a trade with a maturity date earlier than today is received, when processed, then the store rejects it.
2. Given a trade's maturity date is surpassed, when checked, then the store marks it as expired.

---

### Edge Cases

- Missing or malformed fields in trade payloads
- Simultaneous submissions of trades with the same ID and version
- System clock skew or sudden changes
- Trades with future maturity dates

## Requirements (mandatory)

Functional requirements include: ingestion sequencing, version validation, maturity validation, expiry marking, malformed-data handling, deterministic tie-breaker for concurrent same-ID/version, and retention (1 year by default).

## Clarifications

### Session 2025-10-21

- AuthN/AuthZ: OAuth2 / JWT tokens over TLS (verify scopes/roles)
- Ingestion surface: Canonical ingestion API — authenticated HTTPS REST POST endpoint; optional Kafka bridge/adapter
- Observability: Structured logs, Prometheus metrics, OpenTelemetry traces, SLIs/SLOs, and alerting
- Clock authority: External trusted time service (signed timestamps/timeproofs); NTP fallback
- Malformed-data format: Structured JSON error envelope (machine code, human message, field errors, correlation_id)
- Backpressure: Accept-all design; rely on autoscaling and persistent buffers (Kafka) for smoothing

### Integrated changes (security)

The system MUST require TLS for transport. Clients MUST authenticate with OAuth2 JWTs issued by a trusted OIDC provider. The service MUST validate JWTs (JWKS), check scopes/roles, and support key rotation. CI must include secret and dependency vulnerability scanning.

### Integrated changes (ingestion)

FR-010: The system MUST expose a canonical ingestion API (authenticated HTTPS POST). Other ingestion methods must use adapters that normalize to the canonical contract or forward into the ingestion pipeline.

### Integrated changes (observability)

Expose Prometheus-compatible metrics on /metrics (minimum):

```text
trade_ingest_requests_total
trade_ingest_errors_total
trade_ingest_latency_seconds (histogram)
trade_persist_duration_seconds (histogram)
trade_expiry_jobs_run_total
current_inflight_requests
queue_depth
```

Structured JSON logs and OpenTelemetry traces are required. SLIs/SLOs:

- P95 ingest latency <= 2s (SLO)
- Availability 99.9% over 30 days (SLO)
- Error rate < 0.1% per minute (SLO)

Alert on SLO breach, sustained high error rate, or large queue depth spikes. Dashboards and runbooks MUST be provided.

### Integrated changes (clock authority)

Use signed timeproofs (header X-TimeProof or payload) as authoritative for expiry. If missing/invalid, fall back to NTP-synced local clock and mark non-authoritative-time; alert on large skew.

### Integrated changes (malformed-data)

FR-011: For malformed or invalid trades, return a structured JSON error envelope with: code, message, details (field-level errors), and correlation_id. Rejected payloads MUST be written to `errors/` with a retention policy (default 90 days).

### Integrated changes (throttling/backpressure)

Design: Accept all incoming requests and rely on horizontal autoscaling; recommend Kafka buffering for smoothing. Monitoring MUST include autoscaler metrics, queue depth, and resource saturation alerts. Runbooks MUST include mitigation plans.

## Success Criteria (mandatory)

- Trades are stored in correct sequence 100% of the time
- Lower-version trades are rejected and logged
- Same-version trades replace existing records
- Trades with maturity earlier than today are rejected
- Trades are marked expired within 1 minute of maturity passing

## Testing & Instrumentation

- CI: unit tests, integration tests (adapters), instrumentation tests (metrics present), vulnerability scanning, secret scanning
- Perf harness: assert P95 <= 2s under test load and collect metrics/traces


Design: Accept all incoming requests and rely on horizontal autoscaling; recommend Kafka buffering for smoothing. Monitoring MUST include autoscaler metrics, queue depth, and resource saturation alerts. Runbooks MUST include mitigation plans.



