# Running integration tests (Testcontainers)

This document explains how to run the Testcontainers-based integration test for the `trade-store` module locally and in CI.

## Overview

We added a Testcontainers-based integration test that starts a PostgreSQL and a MongoDB container and exercises the `/trades` ingestion endpoint end-to-end. The test class is:

- `com.trademart.tradestore.TradeIngestionTest`

It verifies that a valid trade is persisted to Postgres and that a history record is written to Mongo.

## Prerequisites (local)

- Docker installed and running (Docker Desktop, Docker Engine, or a compatible daemon).
- Java 17 installed (JDK 17).
- Gradle wrapper included in the repo (we use `./gradlew`).

On Windows (PowerShell), make sure Docker Desktop is running and has enough resources (2+ CPUs, 4GB+ RAM recommended).

## Run locally (PowerShell)

1. Start Docker Desktop (or ensure Docker daemon is running).
2. From the `trade-store` directory, run:

```powershell
cd D:\Soni\code\trade-mart\trade-store
.\gradlew.bat test --tests "com.trademart.tradestore.TradeIngestionTest" --info
```

On Linux/macOS use `./gradlew` instead of `.\gradlew.bat`.

The first run will download images and may take a few minutes. Testcontainers will pull the `postgres` and `mongo` images used by the test.

## Run in CI (GitHub Actions)

The repository includes a GitHub Actions workflow at `.github/workflows/integration-tests.yml` that runs the integration test on `push` and `pull_request` to `main`. GitHub Actions runners provide Docker support, so Testcontainers should be able to start the necessary containers.

If you need to run the tests in another CI system, make sure the runner has access to Docker and that outgoing network access is allowed to pull images from Docker Hub.

## Troubleshooting

- Error: `Could not find a valid Docker environment`: Docker is not running or not accessible. Start Docker locally (Docker Desktop) or configure your CI runner to provide Docker.
- Slow first run: Testcontainers will pull images; subsequent runs will be faster due to image caching.
- Resource constraints: If containers fail due to lack of memory/CPU, increase Docker resources (Docker Desktop > Settings).

## Running tests without Docker

If you don't have Docker available and you want to run a subset of tests (unit tests) that don't require containers, run:

```powershell
.\gradlew.bat test --tests "com.trademart.tradestore.*Unit*"    # adjust pattern to match your unit tests
```

Alternatively, add a dedicated profile or test category for integration tests and exclude it when Docker is not available. I can help add this if you'd like.

## CI tips

- Use Gradle build cache and dependency caching to speed up runs (workflow already caches Gradle caches).
- For faster tests, consider a dedicated integration job that is scheduled/triggered separately from quick PR checks.

---

If you'd like, I can also:

- Add a `-DskipIntegration` Gradle property and gate the Testcontainers test on it (already implemented), or
- Add a dedicated Gradle test task `integrationTest` that runs integration classes separately and is skipped by default.

To run the integration tests (Testcontainers) locally, from the `trade-store` directory run:

```powershell
# run the integration test task (starts containers)
.\gradlew.bat integrationTest --info
```

To skip integration tests (useful when Docker isn't available), pass the `skipIntegration` property:

```powershell
# via Gradle project property (-P)
.\gradlew.bat -PskipIntegration=true integrationTest

# or as a system property (-D)
.\gradlew.bat -DskipIntegration=true integrationTest
```

The integration tests are separated into the `integrationTest` source set and task. Unit tests still run with `test`.
