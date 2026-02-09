# Architecture

## Hexagonal Architecture (Ports & Adapters)

The Customer Registry follows hexagonal architecture. The `core` module contains all domain logic and defines ports (interfaces). Adapter modules implement those ports.

## Module Dependency Rules

```
core           -> NO infrastructure dependencies (pure domain)
persistence    -> depends on core only
rest           -> depends on core only
events         -> depends on core only
observability  -> depends on core only
migration      -> depends on core + persistence
autoconfigure  -> wires all modules together
```

Cross-adapter dependencies are forbidden. ArchUnit tests enforce this.

## Spring Modulith

- `core` module is `Type.OPEN` (sub-packages model, port, spi, event, exception, service are all accessible)
- All other modules use default type (closed)
- Module structure verified by `ModularityTests` using ArchUnit

## Auto-Configuration Pattern

1. Every bean uses `@ConditionalOnMissingBean` so the host app can override
2. Every feature gated by `@ConditionalOnProperty(prefix = "customer.registry.features")`
3. All features are OFF by default (secure-by-default)
4. Bridge config pattern: public `@Configuration` in module package exposes package-private beans
5. Auto-config uses `@Import`, NEVER `@ComponentScan` (picks up test inner classes)
6. Events auto-config runs BEFORE core (`@AutoConfiguration(before = ...)`) so the Spring adapter registers before the NoOp fallback

## Frontend Architecture

- Angular 17 standalone components with `ChangeDetectionStrategy.OnPush`
- Signal-based state management via `CustomerStateService`
- Graceful degradation: `SafeFieldRendererHostComponent` catches custom renderer errors
- i18n fallback: host overrides -> built-in[locale] -> built-in['en'] -> key
