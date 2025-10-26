src/
ios/ or android/
directories captured above]

# Implementation Plan: Trade Store Validation

**Branch**: `002-trade-store-validation` | **Date**: 2025-10-21 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/002-trade-store-validation/spec.md`

## Summary

Implement a Java backend using Spring Boot for ingestion and service layers, Apache Kafka as the streaming backbone, PostgreSQL for relational storage and MongoDB for document storage. All functionality will be developed with a TDD approach using JUnit. CI/CD will be implemented with GitHub Actions (primary) with an optional Jenkins pipeline. The pipeline will run regression tests and an OSS vulnerability scan (e.g., OWASP Dependency-Check or Snyk); the build will fail on critical or blocker vulnerabilities. PlantUML diagrams will be generated under `docs/`.

## Technical Context

**Language/Version**: Java 17 (LTS)
**Primary Dependencies**: Spring Boot, Spring Kafka, Spring Data JPA (Hibernate) + async DB drivers as needed, MongoDB Java Driver, JUnit 5
**Storage**: PostgreSQL (SQL) and MongoDB (NoSQL)
**Testing**: JUnit 5 for unit, integration, and regression tests; Testcontainers for DB integration where appropriate
**Target Platform**: Linux servers and cloud containers (Docker)
**Project Type**: Single backend application (Spring Boot microservice)
**Performance Goals**: Real-time ingestion, <2s end-to-end latency P95 under normal load; scalable to thousands of trades/sec with horizontal scaling
**Constraints**: Build must fail on critical vulnerabilities; tests must be written first (TDD); performance and monitoring hooks required
**Scale/Scope**: Support bursts up to 5k trades/sec, sustained ~1k trades/sec with horizontal scaling

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Code Quality: Linting (checkstyle/spotless), code review, modularity enforced
- TDD: All features covered by tests before implementation
- UX Consistency: Not applicable (backend)
- Performance: Real-time ingestion and processing required; explicit performance tests included
- Additional Constraints: Security reviews, zero-downtime deployment

## Project Structure

### Documentation (this feature)

```
specs/002-trade-store-validation/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
└── tasks.md
```

### Source Code (repository root)

```
src/
├── main/java/com/trademart/app/
│   ├── model/
│   ├── service/
│   ├── streaming/
│   ├── repository/
│   └── config/
└── test/java/com/trademart/app/
    ├── unit/
    ├── integration/
    └── regression/

.github/workflows/
docs/                  # PlantUML diagrams
docker/                # Dockerfiles and compose
```

**Structure Decision**: Use Java 17 and Spring Boot to implement a robust streaming microservice with clear separation of concerns.

## Complexity Tracking

No constitution violations detected in this plan after reverting to Java; a separate constitution amendment will be prepared to permit Java as an allowed stack.

