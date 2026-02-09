# ADR-002: Bridge Configuration Pattern

## Status
Accepted

## Context
Auto-configuration classes in the `autoconfigure` package need to register beans defined in adapter packages (persistence, rest, events). These beans are intentionally package-private to enforce module boundaries -- a `CustomerRepositoryJpaAdapter` should not be directly instantiable from outside the `persistence` package.

The obvious approach is `@ComponentScan`, but it has a critical flaw in library auto-configuration.

## Decision
Use public `@Configuration` classes (bridge configs) in each adapter package. Auto-config uses `@Import` to pull in bridge configs. Never use `@ComponentScan` in auto-configuration.

Bridge configs in this project:
- `CustomerPersistenceConfiguration` -- exposes `CustomerRepositoryJpaAdapter`
- `CustomerRestConfiguration` -- exposes `CustomerController` + `CustomerExceptionHandler`
- `CustomerEventsConfiguration` -- exposes `SpringEventPublisherAdapter`

## Why Not @ComponentScan
`@ComponentScan` in auto-config scans ALL classes in the base package, including test inner classes annotated with `@SpringBootApplication`. This causes:

1. **Test class leakage:** Test-only `@SpringBootApplication` inner classes (required for `@WebMvcTest` in a library without a main app class) are loaded in production context
2. **Unexpected beans:** Test-specific beans and configurations pollute the application context
3. **Non-deterministic behavior:** Behavior changes depending on whether test classes are on the classpath
4. **Classpath conflicts:** Test configurations may conflict with production auto-configs

This is not a theoretical risk -- it was discovered during Phase 3 development when `@ComponentScan` picked up `@SpringBootApplication` inner classes from REST controller tests.

## Naming Convention
Bridge configs are named `Customer*Configuration` (e.g., `CustomerPersistenceConfiguration`). For new services, consider the more explicit `*BridgeConfiguration` suffix to distinguish from regular `@Configuration` classes.

## Consequences

### Positive
- Deterministic bean registration -- only explicitly imported beans are loaded
- No test class leakage into production context
- Explicit documentation of module-to-autoconfig wiring via `@Import`
- Each bridge config is a single point of truth for its module's exported beans

### Negative
- Extra boilerplate file per adapter module
- Developers must remember to add `@Import` in auto-config for new bridges
- Bridge pattern is not well-known in the broader Spring community
- Adding a new bean to a module requires updating the bridge config

## Reference Files
- `CustomerPersistenceConfiguration.java` -- exposes `CustomerRepositoryJpaAdapter`
- `CustomerRestConfiguration.java` -- exposes `CustomerController` + `CustomerExceptionHandler`
- `CustomerEventsConfiguration.java` -- exposes `SpringEventPublisherAdapter`
- Auto-configs use `@Import(CustomerXxxConfiguration.class)` to wire them in
