# Trade-mart — Local Docker Compose guide

This repository contains a multi-module Java Spring Boot project (trade-store, trade-expiry, etc.).

This README explains how to build the project artifacts, containerize the services, and run them with Docker Compose. The instructions below run two instances of `trade-expiry` and four instances of `trade-store` inside Docker.

## Prerequisites

- Docker (Engine) and Docker Compose (v2) installed on your machine
- Java 17 (for local builds)
- Gradle (or use the included Gradle wrapper `gradlew.bat` on Windows)

All commands below assume you're in the project root (where this README lives). Your default shell is PowerShell (`pwsh.exe`); commands are shown accordingly.

## Build the Spring Boot JARs

First build the fat jars for the services you want to run. From the project root run:

```pwsh
.
# On Windows (PowerShell):
.\gradlew.bat clean :trade-store:bootJar :trade-expiry:bootJar

# On Unix/macOS (bash):
# ./gradlew clean :trade-store:bootJar :trade-expiry:bootJar
```

After this completes you should have the Spring Boot executable jars in:

- `trade-store/build/libs/` (e.g. `trade-store-*.jar`)
- `trade-expiry/build/libs/` (e.g. `trade-expiry-*.jar`)

## Dockerfile (recommended)

Create one Dockerfile per service (examples below). Save these files at the repository root or next to each module — the examples below assume they are placed in the project root and will copy each module's jar into the image.

Dockerfile for `trade-store` (save as `Dockerfile.trade-store`):

```dockerfile
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# copy the built jar into the image (make sure the jar filename matches)
COPY trade-store/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

Dockerfile for `trade-expiry` (save as `Dockerfile.trade-expiry`):

```dockerfile
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY trade-expiry/build/libs/*.jar /app/app.jar

EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

Note: When you scale multiple containers of the same service you should not map the container ports to a single host port. The Compose setup below runs services on the internal Docker network and does not bind duplicate host ports.

## Example docker-compose.yml

Below is an example `docker-compose.yml` you can place at the project root. It defines Postgres and Mongo, then `trade-store` and `trade-expiry` services. The `build` section assumes the Dockerfiles above are in the project root.

```yaml
version: "3.8"
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: tradedb
      POSTGRES_USER: trade
      POSTGRES_PASSWORD: trade
    volumes:
      - postgres-data:/var/lib/postgresql/data

  mongo:
    image: mongo:6.0
    volumes:
      - mongo-data:/data/db

  trade-store:
    build:
      context: .
      dockerfile: Dockerfile.trade-store
    image: trade-mart/trade-store:local
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tradedb
      SPRING_DATASOURCE_USERNAME: trade
      SPRING_DATASOURCE_PASSWORD: trade
      SPRING_DATA_MONGODB_URI: mongodb://mongo:27017/
    depends_on:
      - postgres
      - mongo
    # do NOT bind the service port to host when scaling multiple instances
    expose:
      - "8080"

  trade-expiry:
    build:
      context: .
      dockerfile: Dockerfile.trade-expiry
    image: trade-mart/trade-expiry:local
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tradedb
      SPRING_DATASOURCE_USERNAME: trade
      SPRING_DATASOURCE_PASSWORD: trade
      SPRING_DATA_MONGODB_URI: mongodb://mongo:27017/
    depends_on:
      - postgres
      - mongo
    expose:
      - "8081"

volumes:
  postgres-data:
  mongo-data:
```

Save that file as `docker-compose.yml` in the project root.

## Build images and start scaled services

1. Build the jars (see earlier) and the Docker images:

```pwsh
# build jars
.\gradlew.bat clean :trade-store:bootJar :trade-expiry:bootJar

# build images with docker-compose (will use the Dockerfiles above)
docker compose build
```

2. Start the stack and scale the services

```pwsh
# Run in detached mode and scale: 4 trade-store instances and 2 trade-expiry instances
docker compose up -d --scale trade-store=4 --scale trade-expiry=2

# Verify the containers are running
docker compose ps

# View logs (all services)
docker compose logs -f
```

## Docker commands (copyable)

Below are the most common Docker commands collected in one place. Use PowerShell (`pwsh`) on Windows.

- Build the Spring Boot jars (if you didn't already):

```pwsh
.\gradlew.bat clean :trade-store:bootJar :trade-expiry:bootJar
```

- Build Docker images (uses the Dockerfiles named in this README):

```pwsh
docker compose build
# OR build images manually per service:
docker build -f Dockerfile.trade-store -t trade-mart/trade-store:local .
docker build -f Dockerfile.trade-expiry -t trade-mart/trade-expiry:local .
```

- Start the stack and scale services (2 trade-expiry, 4 trade-store):

```pwsh
docker compose up -d --scale trade-store=4 --scale trade-expiry=2
```

- Check running containers and ports:

```pwsh
docker compose ps
```

- Tail logs for a service (example: `trade-store`):

```pwsh
docker compose logs -f trade-store
```

- Exec into a running container (example: open a shell in one `trade-store` instance):

```pwsh
docker compose exec trade-store sh
# or (if image has /bin/bash):
docker compose exec trade-store bash
```

- Stop and remove the stack (including volumes):

```pwsh
docker compose down -v
```

- Remove local images created for debugging (optional):

```pwsh
docker image rm trade-mart/trade-store:local trade-mart/trade-expiry:local
```

### Optional: rename running containers to hyphenated pattern

If you'd like container names like `trade-store-1`, `trade-store-2`,
`trade-expiry-1`, `trade-expiry-2` (instead of the Compose-generated
`<project>_<service>_<index>` names), there's a small PowerShell script
included at `scripts/rename-containers.ps1` that will rename running
containers created by Docker Compose.

Usage (dry-run):

```pwsh
pwsh .\scripts\rename-containers.ps1 -DryRun
```

Perform rename:

```pwsh
pwsh .\scripts\rename-containers.ps1
```

Notes:
- The script locates containers by the Compose label `com.docker.compose.service`
  and renames them to the hyphenated pattern. If a target name already exists
  the script aborts to avoid collisions.
- After renaming, Docker Compose's internal names won't match the container
  names shown by `docker ps`; Compose still manages the service lifecycle but
  external tooling can use the hyphenated names for debugging.


Notes about port access
- Because multiple instances are started, the compose file above uses `expose` (internal network) and does not map container ports to the host. If you need to reach a specific instance from the host for debugging, you can run `docker compose ps` to get container IDs and then `docker compose exec <service> /bin/sh` and use curl inside the container.
- If you need external load balancing, add an nginx / traefik service that routes across the scaled containers and maps one host port to the proxy.

## Quick verification

- Check the logs for each service to ensure Spring Boot started and connected to Postgres & Mongo.
- For example, to tail logs of the scaled `trade-store` service:

```pwsh
docker compose logs -f trade-store
```

## Troubleshooting

- If containers fail to start, inspect logs (`docker compose logs <service>`).
- Ensure the jars exist in `trade-store/build/libs/` and `trade-expiry/build/libs/`.
- If you can't access services on the host, remember that scaled containers won't expose a host port unless you map one (not recommended when scaling multiple replicas). Use an HTTP proxy/load-balancer if you need one host port.

## Cleaning up

```pwsh
docker compose down -v
```

This will stop and remove containers, networks and volumes created by compose.

---

If you'd like, I can also add example Dockerfiles and the `docker-compose.yml` file into the repository for you (I only added this README). Would you like me to create those files now? If yes, tell me where you'd like the Dockerfiles placed (project root or per-module directories).
