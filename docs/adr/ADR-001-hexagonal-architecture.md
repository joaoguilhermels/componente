# ADR-001: Hexagonal Architecture with Spring Modulith

## Status
Accepted

## Context
The Customer Registry is a reusable library consumed by multiple host applications. Early prototypes used a traditional layered architecture where domain logic leaked into controllers and repositories, making the code hard to test without infrastructure and creating tight coupling between business rules and framework details. Multiple teams copying the library led to inconsistent boundaries and divergent implementations.

We needed an architecture enforcement mechanism that would:
- Prevent infrastructure dependencies from leaking into domain code
- Make the core domain fully testable without databases, message brokers, or HTTP
- Allow adapters (persistence, REST, events) to be swapped independently
- Enforce these rules at build time, not just by convention

## Decision
Adopt hexagonal (ports and adapters) architecture enforced by Spring Modulith's `ApplicationModules.verify()` at build time. The core module has zero infrastructure dependencies. Adapters depend inward only through ports (interfaces) defined in the core.

Module structure:
```
core/         -> model, port, spi, event, exception, service (NO infra deps)
persistence/  -> depends on core only (JPA adapter)
rest/         -> depends on core only (REST controller)
events/       -> depends on core only (event publishing adapter)
observability/-> depends on core only (metrics and spans)
migration/    -> depends on core + persistence
autoconfigure/-> wires everything together (@ConditionalOnProperty)
```

The core module is declared as `Type.OPEN` so its sub-packages (model, port, spi) are accessible to all other modules. All other modules use the default (closed) type.

## Consequences

### Positive
- Compile-time boundary enforcement (not just convention) via `ModulithStructureTest`
- Core domain testable without infrastructure -- 70+ pure unit tests with no Spring context
- Adapters are pluggable and replaceable via `@ConditionalOnMissingBean`
- `ModulithStructureTest` is a 23-line test that enforces the entire architecture
- Clear dependency direction: outer layers depend on inner, never the reverse

### Negative
- More ceremony: `package-info.java` per module, marker class with `@Modulithic`, bridge configs
- Learning curve for Spring Modulith concepts (module types, allowed dependencies)
- Entity mapper boilerplate between domain models and JPA entities
- Developers must understand ports/adapters pattern before contributing

### Tiered Complexity
Not every service needs full ceremony. We recommend:
- **Tier 1 (Simple):** Just module boundaries + `ModulithStructureTest`
- **Tier 2 (Standard):** Full hexagonal with bridge configs
- **Tier 3 (Library):** Full auto-config with feature flags + SPIs (this project)

## Alternatives Considered
- **Manual ArchUnit rules only:** More flexible but no module discovery/documentation generation
- **Traditional layered architecture:** Doesn't enforce dependency direction at build time
- **Microservice per aggregate:** Too many services for the team size, operational overhead
- **No enforcement (convention only):** Boundaries erode over time without automated checks
