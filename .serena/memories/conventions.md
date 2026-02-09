# Conventions

## Naming

- Java DB tables: `cr_` prefix (e.g., `cr_customer`, `cr_address`, `cr_contact`)
- Liquibase tracking: `cr_databasechangelog`, `cr_databasechangeloglock`
- Spring properties: `customer.registry.*`
- Feature flags: `customer.registry.features.*` (rest-api, publish-events, persistence-jpa, migrations)
- Angular selectors: `crui-` prefix
- Angular CSS custom properties: `--crui-*`

## Feature Flags

### Backend
| Property | Default |
|----------|---------|
| `customer.registry.enabled` | `false` |
| `customer.registry.features.rest-api` | `false` |
| `customer.registry.features.publish-events` | `false` |
| `customer.registry.features.persistence-jpa` | `false` |
| `customer.registry.features.migrations` | `false` |

### Frontend (`CustomerRegistryUiFeatures`)
| Key | Default |
|-----|---------|
| `search` | `true` |
| `inlineEdit` | `false` |
| `addresses` | `true` |
| `contacts` | `true` |

## Code Style

- Java 21: records for value objects, sealed interfaces for type hierarchies
- TDD: red-green-refactor
- Conventional Commits: `feat(scope):`, `fix(scope):`, `docs(scope):`
- Standalone Angular components, OnPush change detection
- Signal-based state management (no NgRx or BehaviorSubject)

## Known Gotchas

- Hibernate 6 JSONB: use `@JdbcTypeCode(SqlTypes.JSON)` not `@Convert`
- PostgreSQL JSONB normalizes JSON (reorders keys) -- assertions must account for this
- `@Input() formControl` collides with Angular's `FormControlDirective` -- use `control`
- `ts-node` needed as devDependency for Jest to parse `.ts` config
- Maven flatten plugin needed for `${revision}` CI-friendly versioning
- Spring Boot 3.2+ requires `-parameters` compiler flag
- `UUID.nameUUIDFromBytes` is UUID v3 (MD5), not v5 (SHA-1)
