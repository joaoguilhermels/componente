# Copilot Code Reviewer Instructions

## REJECT (block the PR)

These violations break architectural invariants. The PR must not merge until they are fixed.

### Infrastructure imports in `core/` package

Any class under a `core/` package that imports infrastructure types must be rejected.

Banned import prefixes in `core/`:
- `javax.persistence`
- `jakarta.persistence`
- `org.springframework.data`
- `org.springframework.web`
- `org.springframework.jdbc`
- `org.springframework.jms`
- `org.springframework.kafka`

```java
// REJECT: core must have zero infrastructure dependencies
package com.onefinancial.customer.core.service;
import jakarta.persistence.EntityManager; // blocked
```

### Cross-adapter dependencies

Adapter modules must depend inward on `core/` only. An adapter must never import from another adapter.

```java
// REJECT: persistence adapter importing from rest adapter
package com.onefinancial.customer.persistence;
import com.onefinancial.customer.rest.CustomerController; // blocked
```

Pairs to check:
- `persistence` must not import from `rest`, `events`, or `observability`
- `rest` must not import from `persistence`, `events`, or `observability`
- `events` must not import from `persistence`, `rest`, or `observability`
- `observability` must not import from `persistence`, `rest`, or `events`

### `@ComponentScan` on `@AutoConfiguration` classes

Auto-configuration classes must use `@Import` to declare their beans. `@ComponentScan` picks up test inner classes and causes unpredictable behavior.

```java
// REJECT
@AutoConfiguration
@ComponentScan("com.onefinancial.customer.persistence") // blocked
public class PersistenceAutoConfiguration { }

// CORRECT
@AutoConfiguration
@Import(PersistenceBridgeConfiguration.class)
public class PersistenceAutoConfiguration { }
```

### Missing `ModulithStructureTest`

Every service must have a test that calls `ApplicationModules.of(...).verify()`. If a PR introduces a new service or module structure and this test is absent, reject.

```java
// Required in every service
@Test
void verifyModulithStructure() {
    ApplicationModules.of(MarkerClass.class).verify();
}
```

### Missing `@ConditionalOnMissingBean` on auto-config beans

Every `@Bean` method inside an `@AutoConfiguration` class (or its `@Import`ed configurations) must have `@ConditionalOnMissingBean` so consumers can override.

```java
// REJECT: missing override point
@Bean
public CustomerRepository customerRepository() { ... }

// CORRECT
@Bean
@ConditionalOnMissingBean
public CustomerRepository customerRepository() { ... }
```

## WARN (comment but do not block)

These are best-practice violations. Leave a review comment explaining the expected pattern.

### Missing `@ConditionalOnProperty` on feature gates

Feature-gated beans should use `@ConditionalOnProperty` with a default of `false` (secure by default). Comment if a feature bean is always active.

### Angular components not standalone or not OnPush

All Angular components should be standalone (no NgModule wrapper) and use `ChangeDetectionStrategy.OnPush`. Comment if either is missing.

### Missing structured header comment on `@AutoConfiguration` classes

Auto-configuration classes should have a Javadoc header documenting activation conditions, provided beans, and override mechanism.

### `@Service` annotation on service classes

Service beans should be wired by auto-configuration, not discovered by annotation scanning. Warn if `@Service` appears on a class that lives inside a module wired by auto-config.

### Bridge configuration naming convention

Bridge configuration classes are named `*Configuration` in the adapter package (e.g., `CustomerPersistenceConfiguration`). The `*BridgeConfiguration` suffix is recommended for new services to make intent explicit. Both patterns are acceptable.

### Missing `package-info.java` with `@ApplicationModule`

Each module package should have a `package-info.java` declaring `@ApplicationModule`. Warn if a new module package is introduced without one.

### PII exposure in logs, events, or exceptions

Sensitive data (CPF, CNPJ, email) must be masked in `toString()`, event payloads, and exception messages. Warn if a document or personal identifier appears unmasked in any of these locations.
