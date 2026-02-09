# 1ff Organization Copilot Instructions

## Architecture

All services follow hexagonal architecture (ports and adapters) with Spring Modulith enforcement.

- `core/` has ZERO infrastructure dependencies. It contains model, port, SPI, event, exception, and service classes.
- Adapters (`persistence/`, `rest/`, `events/`, `observability/`) depend inward on `core/` only.
- No cross-adapter dependencies. Adapters never import from other adapters.
- Auto-configuration (`autoconfigure/`) wires adapters to core ports.

## Tiered Complexity Model

Choose the tier that matches the service's complexity. Do not over-engineer simple services.

### Tier 1 -- Simple (<5 entities, no complex rules)

- Module boundaries enforced via `package-info.java` and `ModulithStructureTest` only.
- Skip bridge configurations. Skip auto-configuration classes.
- Wire beans directly in a single `@Configuration` class.
- Example: a lookup/reference-data service.

### Tier 2 -- Standard (business rules, validation, events)

- Full hexagonal with bridge configurations.
- Ports and SPIs in `core/`. Adapter implementations in dedicated modules.
- Bridge config pattern to expose package-private beans.
- Example: a service with domain events, validation pipelines, or multi-step workflows.

### Tier 3 -- Library (consumed as a dependency by other projects)

- Full auto-configuration with `@ConditionalOnMissingBean` on every bean.
- Feature flags via `@ConditionalOnProperty`, all OFF by default.
- SPIs for extension points. Fallback (no-op) implementations provided.
- Published as a Spring Boot starter with a BOM.
- Example: a shared customer registry or notification library.

## Auto-Configuration

- Annotate with `@AutoConfiguration`. Never use `@Configuration` for auto-config classes.
- Use `@ConditionalOnMissingBean` on ALL bean definitions so consumers can override.
- Gate features with `@ConditionalOnProperty(prefix = "<service>.features", name = "<feature>", havingValue = "true")`. Default is OFF.
- Use `@Import` to pull in bridge configurations. NEVER use `@ComponentScan` -- it picks up test inner classes.
- Bridge config pattern: a public `@Configuration` class in the module package exposes package-private beans for auto-config `@Import`.
- Event auto-config must declare `@AutoConfiguration(before = CoreAutoConfiguration.class)` so Spring adapters register before no-op fallbacks.
- Add a structured header comment on each `@AutoConfiguration` class:

```java
/**
 * Auto-configuration for <module>.
 *
 * <p>Activates when: <conditions>.
 * <p>Provides: <beans>.
 * <p>Overridable via: @ConditionalOnMissingBean.
 */
```

## Spring Modulith

- Every service has a `@Modulithic` marker class (typically the main application class or a library marker).
- Each module has a `package-info.java` with `@ApplicationModule`. Core module uses `Type.OPEN`; all others use the default (closed).
- `ApplicationModules.of(MarkerClass.class).verify()` runs as a mandatory test (`ModulithStructureTest`).

## Testing

- TDD is mandatory. Write tests first, then implementation (red-green-refactor).
- `ModulithStructureTest` is the architectural gate. It must pass before any PR merges.
- Use ArchUnit for custom architectural rules beyond what Modulith enforces.
- Use Testcontainers for integration tests against real databases. Disable Liquibase in `@DataJpaTest` with `spring.liquibase.enabled=false`.
- Use `ApplicationContextRunner` to test auto-configuration conditions.
- `@WebMvcTest` in a library (no `@SpringBootApplication`) requires a `@SpringBootApplication` inner class in the test.
- Name integration test classes with the `*IntegrationTest` suffix so they can be filtered separately.

## Frontend (Angular)

- Standalone components only. No NgModules for feature components.
- `ChangeDetectionStrategy.OnPush` on every component.
- Signal-based state management. Use `asReadonly()` for public signals; keep `WritableSignal` private with `_` prefix.
- Configuration via `InjectionToken` and `provideXxx()` factory functions.
- Feature flags control component visibility with graceful degradation.
- Selector prefix: `crui-`. CSS custom property prefix: `--crui-*`.
- Pure pipes with locale/key caching for i18n.

## Naming Conventions

- DB tables: `cr_` prefix (e.g., `cr_customer`, `cr_address`).
- Spring properties: `customer.registry.*` prefix.
- Feature flags: `customer.registry.features.*`.
- Angular selectors: `crui-` prefix.
- Liquibase tracking tables: `cr_databasechangelog`, `cr_databasechangeloglock`.
- Bridge configurations: `*Configuration` in the adapter package (e.g., `CustomerPersistenceConfiguration`). For new services, the `*BridgeConfiguration` suffix is recommended for clarity.

## Commits

Use Conventional Commits format: `type(scope): description`.

- `feat`: new feature
- `fix`: bug fix
- `refactor`: code restructuring without behavior change
- `test`: adding or updating tests
- `docs`: documentation only
- `chore`: build, CI, or tooling changes

## Builds

Docker-only. Never run Java or Node locally.

```bash
make build    # Full build (Java + Angular)
make test     # All tests
make verify   # Full verification including ArchUnit
```
