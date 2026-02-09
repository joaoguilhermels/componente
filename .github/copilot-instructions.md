# Customer Registry - Copilot Instructions

## Build System
- All builds run inside Docker containers. NEVER run Java, Maven, Node, or npm locally.
- Use `make build`, `make test`, `make verify` for common tasks.
- Direct Docker: `docker compose -f docker/docker-compose.yml run --rm --no-deps java-build mvn test`

## Architecture: Hexagonal (Ports & Adapters)
- `core/` contains domain model, ports (interfaces), SPIs, service logic. It has ZERO infrastructure dependencies.
- Adapters (`persistence/`, `rest/`, `events/`, `observability/`) depend ONLY on `core/`.
- Never create dependencies between adapter modules.
- Spring Modulith enforces boundaries. ArchUnit tests verify at build time.

## Auto-Configuration
- Every bean: `@ConditionalOnMissingBean` (host app can override any bean)
- Every feature: `@ConditionalOnProperty(prefix = "customer.registry.features")`, OFF by default
- Bridge config pattern: public `@Configuration` in module package exposes package-private beans
- Auto-config uses `@Import`, NEVER `@ComponentScan`
- Events auto-config runs BEFORE core auto-config

## Feature Flags
Backend: `customer.registry.features.rest-api`, `.publish-events`, `.persistence-jpa`, `.migrations`
Frontend: `search`, `inlineEdit`, `addresses`, `contacts` in `CustomerRegistryUiFeatures`

## Naming
- DB tables: `cr_` prefix
- Properties: `customer.registry.*`
- Angular selectors: `crui-`
- CSS variables: `--crui-*`

## Angular
- Standalone components, `ChangeDetectionStrategy.OnPush`
- Signal-based state (no NgRx or BehaviorSubject)
- i18n: ship `pt-BR` and `en`, host can override any key
- Never name `@Input()` as `formControl` (collides with Angular directive)

## Testing
- TDD approach (red-green-refactor)
- Java: JUnit 5, Mockito, ArchUnit, Testcontainers
- Angular: Jest + jest-preset-angular@14.6.2
- `@DataJpaTest` auto-includes Liquibase -- disable with property `spring.liquibase.enabled=false`

## Commit Messages
Conventional Commits: `feat(scope): description`, `fix(scope): description`
