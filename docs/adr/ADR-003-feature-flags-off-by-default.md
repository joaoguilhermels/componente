# ADR-003: Feature Flags Off by Default

## Status
Accepted

## Context
The Customer Registry is a reusable library consumed by multiple host applications. Each host app needs different combinations of features (persistence, REST API, event publishing, observability). A pharmacy app might need REST + persistence but not events; an internal tool might need events + observability but not REST.

We need a mechanism that is:
- **Secure by default:** no features enabled until explicitly opted in
- **Fine-grained:** each feature independently toggleable
- **Overridable:** host apps can replace any bean with their own implementation
- **Graceful:** the library starts even without infrastructure (useful for testing)

## Decision
1. All features gated by `@ConditionalOnProperty`, all OFF by default
2. All beans use `@ConditionalOnMissingBean` so host apps can override any bean
3. Fallback beans provided for core dependencies (in-memory repo, no-op event publisher)

### Property Hierarchy
```yaml
customer:
  registry:
    enabled: true                          # Master switch (required)
    features:
      persistence-jpa: true                # JPA adapter + Liquibase migrations
      rest-api: true                       # REST controller + exception handler
      publish-events: true                 # Spring ApplicationEvent publishing
      migrations: true                     # Liquibase schema management
```

### Fallback Beans
When features are disabled, the core auto-config provides:
- `InMemoryCustomerRepository` -- ConcurrentHashMap-based, no database needed
- `NoOpEventPublisher` -- logs discarded events at WARN level

## Auto-Config Ordering
Events auto-config runs BEFORE core (`@AutoConfiguration(before = CustomerRegistryCoreAutoConfiguration.class)`).

Why? Core registers `NoOpEventPublisher` via `@ConditionalOnMissingBean`. If events auto-config runs first, `SpringEventPublisherAdapter` registers first and `NoOp` is skipped. If core runs first, `NoOp` registers and the real events adapter is ignored even when enabled.

This ordering constraint applies to any adapter that provides a bean also provided as a fallback by core.

## Consequences

### Positive
- Secure by default: no features enabled until explicitly opted in
- Every bean replaceable: host apps have full control via `@ConditionalOnMissingBean`
- App starts even without infrastructure (fallback beans serve as stubs)
- Feature combination flexibility: any subset of features can be enabled
- Single dependency model: host apps add one starter, configure what they need

### Negative
- Must remember to enable features in `application.yml` (common onboarding friction)
- Auto-config ordering is implicit and can be surprising if not documented
- `@ConditionalOnMissingBean` race conditions between auto-configs require careful ordering
- Fallback beans (in-memory repo) can mask missing configuration in production

## Alternatives Considered
- **Features ON by default:** Simpler but insecure -- host app gets features it did not ask for
- **No feature flags, just exclude dependencies:** Loses the "one dependency, configure what you need" model; forces host apps to manage transitive dependencies
- **Spring Profiles:** Profiles do not compose well (can only activate, not deactivate) and do not support `@ConditionalOnMissingBean` overrides
- **Separate starters per feature:** More Maven artifacts to publish and version; host apps need multiple dependencies instead of one
