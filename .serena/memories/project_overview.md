# OneFinancial Customer Registry - Project Overview

## Purpose
Spring Boot starter library + Angular UI library for customer registration with Brazilian document validation (CPF/CNPJ), extensible pipelines (validators, enrichers), and pluggable persistence.

## Tech Stack
- Java 21, Spring Boot 3.5.9, Spring Modulith 1.4.7
- Angular 17, Jest, jest-preset-angular@14
- PostgreSQL 16, Testcontainers, Liquibase
- Maven reactor with BOM + Starter pattern
- Docker-based builds (never run Java/Node locally)

## Key Commands
```bash
# Java tests
docker compose -f docker/docker-compose.yml run --rm --no-deps java-build mvn test
# Angular tests
docker compose -f docker/docker-compose.yml run --rm node-build npx jest --config projects/customer-registry-ui/jest.config.ts
# Angular build
docker compose -f docker/docker-compose.yml run --rm node-build ng build customer-registry-ui --configuration production
```

## Structure
- `customer-registry-starter/` - Main Spring Boot starter (core, persistence, REST, events, observability, auto-config)
- `customer-registry-bom/` - Bill of Materials
- `customer-registry-client-example-backend/` - Example Spring Boot app
- `frontend/projects/customer-registry-ui/` - Angular library
- `frontend/projects/example-crm-app/` - Example Angular app
- `docker/` - Docker Compose for builds
