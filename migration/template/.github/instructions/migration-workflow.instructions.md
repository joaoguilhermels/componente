# Migration Workflow Instructions

> **Scope**: These instructions apply to all files in the migration target repository.
> They guide GitHub Copilot Chat through a phased migration from legacy Spring Boot
> services to hexagonal architecture with Spring Modulith.

---

## Role and Context

You are assisting with migrating a legacy Spring Boot service to hexagonal architecture.
The workspace has three directories:

- `reference/` -- The OneFinancial Customer Registry (read-only reference patterns)
- `legacy/` -- The existing service being migrated (read-only source)
- `target/` -- The new service being created (your output goes here)

**Always read reference patterns before generating code.** Never invent patterns that
differ from the reference implementation.

---

## Migration Phases

Follow these phases strictly in order. Do not skip phases or combine steps.

### Phase 0 -- Legacy Analysis

**Goal**: Understand the legacy service before writing any code.

**Rules**:
- Identify all JPA entities and their relationships
- List all external dependencies (databases, queues, APIs, caches)
- Map existing services to potential port interfaces
- Classify the service tier: Simple, Standard, or Advanced
- Document findings before proceeding to Phase 1

**Output**: A summary report with entity list, dependency map, and tier classification.

### Phase 1 -- Scaffold

**Goal**: Create the package structure and architectural gate tests.

**Rules**:
- Create the `@Modulithic` marker class (NOT `@SpringBootApplication` -- this is a library)
- Create `package-info.java` for each module with correct `@ApplicationModule` annotations
- Core module MUST be `Type.OPEN` so sub-packages are accessible
- Other modules use default (closed) type with `allowedDependencies = {"core"}`
- Create `ModulithStructureTest` and `ArchitectureRulesTest`
- Follow the placeholder pattern from the reference templates

**Verification**: `ModulithStructureTest` must compile and pass.

### Phase 2 -- Core Extraction

**Goal**: Extract pure domain model with zero infrastructure dependencies.

**Rules**:
- **FORBIDDEN in core/**: JPA annotations (`jakarta.persistence.*`), Spring Data (`org.springframework.data.*`), Spring Web (`org.springframework.web.*`), Spring stereotypes (`@Service`, `@Component`, `@Repository`), JDBC (`java.sql.*`), messaging
- **ALLOWED in core/**: `org.springframework.transaction.annotation.Transactional` (cross-cutting concern), `org.springframework.modulith.*` (module metadata)
- Aggregate roots: classes with named static factories (e.g., `Customer.createPF()`)
- Value objects: Java records with compact constructor validation
- Ports: interfaces in `core/port/` that adapters will implement
- SPIs: extension points in `core/spi/` with default implementations
- Events: immutable records -- omit PII fields entirely (never expose CPF/CNPJ/documents, not even masked)
- Services: no Spring stereotypes (`@Service`, `@Component`) -- wired by auto-config
- Use `UUID` for entity IDs, generated via `UUID.randomUUID()` in static factories

**Verification**: `grep -r "import jakarta\.\|import org\.springframework\.data\.\|import org\.springframework\.web\.\|import org\.springframework\.stereotype\." target/src/main/java/**/core/` returns zero results.

### Phase 3 -- Adapters

**Goal**: Create infrastructure adapters that depend only on core.

**Rules**:
- Each adapter module gets a `package-info.java` with `allowedDependencies = {"core"}`
- **Bridge config pattern** (MANDATORY):
  - Public `@Configuration` class in the adapter package
  - Exposes package-private adapter beans
  - Never use `@ComponentScan` (picks up test classes) -- use `@Import` instead
- **Persistence adapter**:
  - JPA entities are separate from domain model
  - Create a separate package-private mapper utility class (e.g., `CustomerEntityMapper`)
    with static `toEntity()` and `toDomain()` methods -- do NOT put mappers on the entity
  - Use `@JdbcTypeCode(SqlTypes.JSON)` for JSONB columns (not `@Convert`)
  - Implement `Persistable<UUID>` to avoid extra SELECT on insert
  - Use `@BatchSize(size=25)` on `@OneToMany` collections
- **REST adapter**:
  - Controllers MUST be package-private (not public)
  - Spring Boot 3.2+ requires `-parameters` compiler flag
  - `@WebMvcTest` needs `@SpringBootApplication` inner class (library has none)
- **Events adapter**:
  - Auto-config must run BEFORE core (`@AutoConfiguration(before = ...)`)
  - Never include PII in published events

**Verification**: Bridge config files exist for each adapter module.

### Phase 4 -- Auto-Configuration

**Goal**: Wire all modules with conditional, overridable configuration.

**Rules**:
- Every auto-config class needs a **structured header comment**:
  ```java
  /*
   * ORDERING: before/after which auto-configs
   * GATE: @ConditionalOnProperty — which properties must be true
   * BRIDGE: which bridge config is @Import-ed (if applicable)
   * OVERRIDABLE: which beans have @ConditionalOnMissingBean (if applicable)
   */
  ```
- **Dual-gate pattern**: Adapter auto-configs require BOTH master switch AND feature flag:
  `@ConditionalOnProperty(prefix = "<PREFIX>", name = {"enabled", "features.<feature>"}, havingValue = "true")`
  Core auto-config uses only the master switch:
  `@ConditionalOnProperty(name = "<PREFIX>.enabled", havingValue = "true")`
- Use kebab-case feature names: `features.publish-events`, `features.persistence-jpa`, `features.rest-api`
- `matchIfMissing` defaults to `false` when omitted (secure-by-default) -- either omit or set explicitly
- `@ConditionalOnMissingBean` on all default/fallback beans and bridge config beans (consumers can override)
  Exception: infrastructure beans like Liquibase do NOT need `@ConditionalOnMissingBean`
- `@ConditionalOnMissingBean` goes on bridge config `@Bean` methods (where adapter beans are created),
  not on the auto-config class itself (which only `@Import`s the bridge)
- Register in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Events auto-config MUST be ordered `before` core auto-config
- Provide fallback beans in core auto-config: in-memory repository, no-op event publisher

**Verification**: All adapter auto-configs use the dual-gate pattern. No `matchIfMissing = true` anywhere.

### Phase 5 -- Frontend (if applicable)

**Goal**: Migrate Angular components to library pattern.

**Rules**:
- All components MUST be standalone with `ChangeDetectionStrategy.OnPush`
- Use signal-based state: `WritableSignal` private with `_` prefix, `asReadonly()` for public API
- i18n fallback chain: host overrides -> built-in[locale] -> built-in['en'] -> key
- CSS custom properties prefixed with `--<prefix>-*`
- Never name `@Input()` as `formControl` (collides with Angular directive)
- `ts-node` required as devDependency for Jest config
- `ng-packagr` requires at least one export in `public-api.ts`

**Verification**: All components use `OnPush` and standalone.

---

## Cross-Cutting Rules

### Naming Conventions
- DB tables: `<db_prefix>_` (e.g., `cr_customer`)
- Properties: `<property_prefix>.*` (e.g., `customer.registry.features.search`)
- Angular selectors: `<angular_prefix>-` (e.g., `crui-customer-list`)

### Testing
- Follow TDD: write tests first, then implementation
- `ModulithStructureTest` is the architectural gate -- it MUST pass before any PR merge
- Use `ApplicationContextRunner` to verify auto-config bean registration
- Integration tests use Testcontainers (Docker-in-Docker setup required)

### Docker Only
- NEVER run Java or Node builds locally
- All builds go through Docker Compose
- Maven cache: `/home/builder/.m2`, npm cache: `/home/builder/.npm`

### Lessons Learned
- After completing each phase, record observations in `MIGRATION-LESSONS.md`
- Note: what worked on first try, what needed corrections, what patterns were missing
- This feedback loop improves the migration framework for future services

---

## Anti-Patterns to Reject

| Anti-Pattern | Why It Fails | Correct Approach |
|--------------|-------------|------------------|
| `@ComponentScan` in auto-config | Picks up test inner classes | Use `@Import(BridgeConfig.class)` |
| JPA annotations in core model | Violates hexagonal boundary | Separate JPA entity in persistence module |
| Public controllers | Exposes internals | Package-private with bridge config |
| `@Service` on domain service | Tight Spring coupling | Wire via auto-config `@Bean` method |
| Feature flags `matchIfMissing = true` | Not secure-by-default | Omit `matchIfMissing` (defaults to `false`) or set explicitly |
| Single-gate on adapter auto-config | Bypasses master switch | Use dual-gate: `name = {"enabled", "features.<name>"}` |
| Mapper methods on JPA entity | Couples entity to domain | Separate mapper utility class (e.g., `CustomerEntityMapper`) |
| `@Convert` for JSONB | Hibernate 6 incompatible | `@JdbcTypeCode(SqlTypes.JSON)` |
| Naming `@Input() formControl` | Collides with Angular directive | Use `control` instead |
| `ENTRYPOINT` in Docker | Incompatible with Makefile CMD | Use `CMD` instead |
| Running builds outside Docker | Inconsistent environments | Always use Docker Compose |
