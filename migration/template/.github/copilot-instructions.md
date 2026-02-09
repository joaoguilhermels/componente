# <SERVICE_NAME> -- Copilot Instructions

## Architecture

This service follows the 1ff hexagonal architecture pattern using Spring Modulith.
All modules are organized under the `<BASE_PACKAGE>` package.

### Module Boundaries

```
core/         -> model, port, spi, event, exception, service (NO infrastructure deps)
persistence/  -> depends on core only (JPA adapter)
rest/         -> depends on core only (REST controller)
events/       -> depends on core only (event publishing adapter)
observability/-> depends on core only (metrics & spans)
autoconfigure/-> wires everything together (@ConditionalOnProperty)
```

The `core/` module is `Type.OPEN` (sub-packages are accessible to other modules).
All other modules are closed by default.

### Key Rules

- **Core isolation**: `core/` has ZERO infrastructure dependencies. No JPA, no Spring Web, no Spring Data.
- **Bridge config pattern**: public `@Configuration` in each adapter module package exposes package-private beans. Auto-config uses `@Import`, NEVER `@ComponentScan`.
- **Conditional beans**: `@ConditionalOnMissingBean` on ALL auto-config beans, `@ConditionalOnProperty` gates on all features (default OFF).
- **Events ordering**: Events auto-config runs BEFORE core (`@AutoConfiguration(before = ...)`) so the Spring adapter registers before the NoOp fallback.

## Build Commands (Docker Only)

```bash
make build            # Full build (Java + Angular)
make test             # Run all tests
make test-java        # Java tests only
make test-angular     # Angular tests only
make verify           # Full verification (includes ArchUnit)
make up               # Start Postgres + pgAdmin
make down             # Stop services
```

NEVER run Java or Node locally. All commands go through Docker.

## Naming Conventions

- DB tables: `<DB_PREFIX>` prefix (e.g., `<DB_PREFIX>customer`)
- Spring properties: `<PROPERTY_PREFIX>.*`
- Feature flags: `<PROPERTY_PREFIX>.features.*`
- Angular selectors: `<ANGULAR_PREFIX>-` prefix
- CSS custom properties: `--<ANGULAR_PREFIX>-*`
- Liquibase tracking tables: `<DB_PREFIX>databasechangelog`, `<DB_PREFIX>databasechangeloglock`

## Testing

- TDD approach: write test first, see it fail, then implement
- Java: JUnit 5, Mockito, ArchUnit, Testcontainers
- `ModulithStructureTest` is the architectural gate -- must always pass
- `ArchitectureRulesTest` enforces custom rules beyond Modulith
- Angular: Jest with jest-preset-angular, standalone component testing
- Integration tests use `*IntegrationTest` suffix for selective execution

## Key Versions

- Java 21, Spring Boot 3.5.x, Spring Modulith 1.4.x
- Angular CLI 17, Node 20, TypeScript 5.4
- PostgreSQL 16

## Known Gotchas

- Hibernate 6 JSONB: use `@JdbcTypeCode(SqlTypes.JSON)` not `@Convert`
- Spring Boot 3.2+ requires `-parameters` compiler flag
- `@DataJpaTest` includes Liquibase -- disable with `spring.liquibase.enabled=false`
- `@WebMvcTest` in library needs a `@SpringBootApplication` inner class in test
- Docker images use CMD not ENTRYPOINT
