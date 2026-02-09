# OneFinancial Customer Registry

## Build Commands (Docker Only -- NEVER run Java/Node locally)

```bash
make build            # Full build (Java + Angular)
make test             # Run all tests
make test-java        # Java tests only
make test-angular     # Angular tests only
make verify           # Full verification (includes ArchUnit)
make up               # Start Postgres + pgAdmin
make down             # Stop services
make security-scan    # Security scan (SpotBugs, OWASP)
```

### Direct Docker Commands

```bash
# Java build
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

## Architecture Rules

- **Hexagonal architecture**: core/ has ZERO infrastructure deps, adapters depend inward only
- **Spring Modulith**: core module is `Type.OPEN`, all other modules are default (closed)
- `@ConditionalOnMissingBean` required on all auto-config beans (fallback pattern)
- `@ConditionalOnProperty` gates for all features (secure-by-default, all off)
- **Bridge config pattern**: public `@Configuration` in module package exposes package-private beans; auto-config uses `@Import` (NEVER `@ComponentScan` -- picks up test classes)
- Events auto-config MUST run BEFORE core (`@AutoConfiguration(before = ...)`) so Spring adapter registers before NoOp fallback
- `@WebMvcTest` in library (no `@SpringBootApplication`) needs a `@SpringBootApplication` inner class in test

## Naming Conventions

- Java DB tables: `cr_` prefix (e.g., `cr_customer`, `cr_address`)
- Spring properties: `customer.registry.*` prefix
- Feature flags: `customer.registry.features.*`
- Angular selectors: `crui-` prefix
- Angular CSS custom properties: `--crui-*`
- Liquibase tracking tables: `cr_databasechangelog`, `cr_databasechangeloglock`

## Module Boundaries

```
core/         -> model, port, spi, event, exception, service (NO infra deps)
persistence/  -> depends on core only (JPA adapter)
rest/         -> depends on core only (REST controller)
events/       -> depends on core only (event publishing adapter)
observability/-> depends on core only (metrics & spans)
migration/    -> depends on core + persistence
autoconfigure/-> wires everything together (@ConditionalOnProperty)
```

## Testing

- TDD approach -- tests first, red-green-refactor
- Java: JUnit 5, Mockito, ArchUnit, Testcontainers 1.20.4
- Angular: Jest with jest-preset-angular@14.6.2
- Jest setup: `setupFilesAfterEnv` (NOT `setupFilesAfterSetup`)
- Jest zone setup: `setupZoneTestEnv()` from `jest-preset-angular/setup-env/zone`
- `@DataJpaTest` includes LiquibaseAutoConfiguration -- disable via `spring.liquibase.enabled=false`

## Frontend Architecture

- Angular 17, standalone components, `ChangeDetectionStrategy.OnPush`
- Signal-based state management (`CustomerStateService`)
- Feature flags: `search`, `inlineEdit`, `addresses`, `contacts` (in `CustomerRegistryUiFeatures`)
- i18n fallback chain: host overrides -> built-in[locale] -> built-in['en'] -> key
- Graceful degradation: `SafeFieldRendererHostComponent` catches renderer errors, falls back to Material input

## Known Gotchas

- Hibernate 6 JSONB: use `@JdbcTypeCode(SqlTypes.JSON)` not `@Convert`
- PostgreSQL JSONB normalizes JSON (reorders keys) -- assertions must account for this
- `@Input() formControl` naming collides with Angular's `FormControlDirective` -- use `control` instead
- `ts-node` required as devDependency for Jest to parse `.ts` config
- Maven flatten plugin needed for `${revision}` CI-friendly versioning
- Spring Boot 3.2+ requires `-parameters` compiler flag for `@PathVariable`/`@RequestParam`
- `UUID.nameUUIDFromBytes` generates UUID v3 (MD5), not v5 (SHA-1)
- Docker images use CMD not ENTRYPOINT (Makefile passes full commands)
- ng-packagr requires at least one export in `public-api.ts` (empty barrel fails)

## Key Versions

- Java 21, Spring Boot 3.5.9, Spring Modulith 1.4.7
- Angular CLI 17, Node 20, TypeScript 5.4
- jest-preset-angular@14.6.2, Testcontainers 1.20.4, ArchUnit 1.3.0
- PostgreSQL 16

## Deep Dives

See [README.md](README.md) for full architecture diagrams, REST API docs, event schemas, observability metrics, JSONB evolution strategy, and upgrade guide.
See [CONTRIBUTING.md](CONTRIBUTING.md) for development workflow, branch naming, PR process, and deprecation policy.
