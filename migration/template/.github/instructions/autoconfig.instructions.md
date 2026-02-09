---
applyTo: "**/autoconfigure/**/*.java"
---

# Auto-Configuration Rules

Auto-configuration classes wire the hexagonal modules together and provide
sensible defaults that consumers can override.

## Class Structure

Every auto-config class MUST have a structured header comment:

```java
/**
 * ORDERING: Runs after CoreAutoConfiguration (needs service bean).
 * GATE: <PROPERTY_PREFIX>.enabled=true AND <PROPERTY_PREFIX>.features.persistence=true
 * BRIDGE: Imports PersistenceBridgeConfiguration for JPA adapter beans.
 * OVERRIDABLE: CustomerRepository (host can provide custom implementation).
 */
```

## Annotations

- `@AutoConfiguration` with explicit ordering where needed (`before`/`after`)
- `@ConditionalOnProperty(havingValue = "true")` -- all features OFF by default
- `@ConditionalOnMissingBean` on EVERY `@Bean` definition
- `@Import(XxxConfiguration.class)`, NEVER `@ComponentScan`

```java
@AutoConfiguration(after = CoreAutoConfiguration.class)
@ConditionalOnProperty(prefix = "<PROPERTY_PREFIX>", name = "enabled", havingValue = "true")
@Import(CustomerPersistenceConfiguration.class)
public class PersistenceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    CustomerRepository customerRepository(CustomerJpaRepository jpaRepo) {
        return new JpaCustomerRepositoryAdapter(jpaRepo);
    }
}
```

## Fallback Beans

Core auto-config provides fallback implementations:

- **In-memory repository**: implements the port interface, backed by `ConcurrentHashMap`
- **No-op event publisher**: silently discards events

Both use `@ConditionalOnMissingBean` so they are replaced when adapter
auto-configs activate.

## Registration

Every auto-config class must be listed in:

```
src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

One fully-qualified class name per line:

```
<BASE_PACKAGE>.autoconfigure.CoreAutoConfiguration
<BASE_PACKAGE>.autoconfigure.EventsAutoConfiguration
<BASE_PACKAGE>.autoconfigure.PersistenceAutoConfiguration
<BASE_PACKAGE>.autoconfigure.RestAutoConfiguration
<BASE_PACKAGE>.autoconfigure.ObservabilityAutoConfiguration
```

## Ordering Rules

1. Events auto-config runs BEFORE core (Spring adapter registers first)
2. Core auto-config runs with no explicit ordering (provides fallback beans)
3. All other auto-configs run AFTER core (they depend on the service bean)

```
EventsAutoConfig  -->  CoreAutoConfig  -->  PersistenceAutoConfig
                                       -->  RestAutoConfig
                                       -->  ObservabilityAutoConfig
```

## Complexity Tiers

| Tier | Auto-Configs Needed |
|------|-------------------|
| Simple | Core + one adapter (e.g., persistence only) |
| Standard | Core + Events + Persistence + REST |
| Advanced | All of the above + Observability + Migration |

Start with Core auto-config and add others as adapters are implemented.
