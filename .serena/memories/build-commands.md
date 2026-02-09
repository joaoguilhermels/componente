# Build Commands

All builds run inside Docker. NEVER run Java/Node locally.

## Makefile Targets

```bash
make build            # Full build (Java + Angular)
make test             # All tests (Java + Angular)
make test-java        # Java tests only
make test-angular     # Angular tests only
make verify           # Full verification (includes ArchUnit)
make build-java       # Java build (skip tests)
make build-angular    # Angular build
make clean            # Clean all build artifacts
make up               # Start Postgres + pgAdmin
make down             # Stop services
make security-scan    # Security scan (SpotBugs, OWASP)
```

## Direct Docker Commands

```bash
# Java test (specific module)
docker compose -f docker/docker-compose.yml run --rm --no-deps java-build mvn test -pl customer-registry-starter

# Angular build
docker compose -f docker/docker-compose.yml run --rm node-build ng build customer-registry-ui --configuration production

# Angular tests
docker compose -f docker/docker-compose.yml run --rm node-build npx jest --config projects/customer-registry-ui/jest.config.ts --no-cache

# Testcontainers (Docker-in-Docker)
docker compose -f docker/docker-compose.yml run --rm --no-deps \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e TESTCONTAINERS_RYUK_DISABLED=true \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  java-build mvn test -pl customer-registry-starter
```

## Docker Compose Services

- `java-build`: Maven build container (eclipse-temurin:21-jdk-alpine)
- `node-build`: Angular build container (node:20-alpine)
- `postgres`: PostgreSQL 16 database
- `pgadmin`: pgAdmin web UI
