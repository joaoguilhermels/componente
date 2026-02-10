# Copilot Chat Migration Prompts

> **Purpose**: Ready-to-paste prompts for GitHub Copilot Chat (VS Code) to migrate legacy
> Spring Boot services to hexagonal architecture with Spring Modulith.
>
> **Workspace layout**: Three directories in a multi-root workspace:
> - `reference/` -- OneFinancial Customer Registry (read-only)
> - `legacy/` -- The legacy service being migrated (read-only)
> - `target/` -- Empty directory where new code is generated
>
> **How to use**: Copy the prompt text, replace `<placeholders>`, and paste into Copilot Chat.
> Include the referenced `#file:` paths so Copilot has the right context.

---

## Table of Contents

- [Phase 0: Legacy Analysis](#phase-0--legacy-analysis) (includes Tier Assessment)
- [Phase 1: Scaffold](#phase-1--scaffold)
- [Phase 2: Core Extraction](#phase-2--core-extraction)
- [Phase 3: Adapters](#phase-3--adapters)
- [Phase 4: Auto-Configuration](#phase-4--auto-configuration)
- [Phase 5: Frontend](#phase-5--frontend-optional)
- [Inter-Phase Validation](#inter-phase-validation)
- [Verification](#verification)
- [Lessons Learned](#lessons-learned)

---

## Phase 0 -- Legacy Analysis

### Prompt 0.0: Tier Assessment

**Context**: Before analyzing code, determine the migration tier. This guides which modules
and prompts apply.

**Prompt**:
```
Before analyzing code, I need to determine the correct migration tier.
Answer these questions about the legacy service:

1. How many JPA @Entity classes does it have?
   - 1 entity -> likely Simple
   - 2-5 entities -> likely Standard
   - 6+ entities -> likely Advanced

2. Does it have dynamic user-defined fields (JSONB, EAV pattern, or key-value attributes)?
   - Yes -> at minimum Standard, probably Advanced
   - No -> could be Simple or Standard

3. Which adapters does it need?
   - REST only -> Simple
   - REST + persistence -> Standard
   - REST + persistence + events -> Standard or Advanced
   - REST + persistence + events + observability + migration -> Advanced

4. Does it have an Angular/React frontend?
   - No frontend -> Simple or Standard
   - Yes, with feature flags and i18n -> Advanced

5. Does it support multi-tenancy or complex lifecycle states?
   - Yes -> Advanced
   - No -> Simple or Standard

Based on the answers, classify as:

| Tier | Criteria | Modules | Estimated Phases |
|------|----------|---------|-----------------|
| **Simple** | 1 entity, 1-2 adapters, no UI, no events | core, persistence, rest, autoconfigure | Phase 0-4 (skip 5) |
| **Standard** | 2-5 entities, REST + persistence + events, no complex JSONB | core, persistence, rest, events, autoconfigure | Phase 0-4 (skip 5) |
| **Advanced** | 6+ entities, JSONB attributes, Angular UI, observability, migration | All modules including frontend | Phase 0-5 |

Output: A one-paragraph justification for the chosen tier and a list of modules to generate.
Save as target/TIER-ASSESSMENT.md
```

**Expected Output**: Tier classification with justification and module list.

**Validation**: The chosen tier determines which subsequent prompts are required. Skip Phase 5 prompts for Simple/Standard tiers.

**Recovery**: If the tier seems wrong after Phase 2:
```
The migration is more complex than initially assessed.
Re-evaluate the tier: the presence of <specific pattern> suggests
upgrading from <current_tier> to <higher_tier>.
Adjust the migration plan and add the missing modules.
```

**Lessons Learned Entry**: Record whether the initial tier assessment was correct, or if it needed to be revised mid-migration.

---

### Prompt 0.1: Analyze Legacy Service Structure

**Context**: Show Copilot the main legacy source directory.

**Prompt**:
```
Analyze the legacy service in @workspace for migration to hexagonal architecture.

Look at the source code under legacy/src/main/java/ and produce a report with:

1. **Entity Inventory**: List all JPA entities with their fields, relationships
   (@OneToMany, @ManyToOne, etc.), and table names
2. **Service Layer**: List all @Service/@Component classes and their dependencies
3. **External Dependencies**: Databases, message queues, REST clients, caches
4. **API Surface**: All @RestController endpoints (method, path, request/response types)
5. **Configuration**: application.yml/properties keys, feature flags, profiles
6. **Tier Classification**: Based on the criteria below, classify as Simple/Standard/Advanced

Tier criteria:
- Simple: Single entity, 1-2 adapters, no UI
- Standard: Multiple entities, REST + persistence, possible events
- Advanced: Full lifecycle, multi-tenant, JSONB, Angular UI

Format the output as a markdown document I can save as target/LEGACY-ANALYSIS.md
```

**Expected Output**: A markdown report with entity list, dependency map, and tier classification.

**Validation**: Cross-check entity count against actual JPA `@Entity` classes in legacy code.

**Recovery**: If Copilot misses entities, point it to specific packages:
```
You missed the entities in legacy/src/main/java/com/example/<package>/model/.
Please include those in the analysis.
```

**Lessons Learned Entry**: Record in Phase 0 section -- accuracy of entity/dependency identification, any patterns Copilot missed.

---

### Prompt 0.2: Map Legacy to Hexagonal Modules

**Context**: Use the analysis from Prompt 0.1.

**Prompt**:
```
Based on the legacy analysis, map the existing code to hexagonal modules:

Read the migration scorecard: #file:reference/migration/scorecard.md

Now create a migration plan mapping:
- Which legacy classes become core domain model?
- Which services become port interfaces?
- Which DAOs/repositories become persistence adapter?
- Which controllers become REST adapter?
- Which event publishers become events adapter?
- What new code needs to be written (bridge configs, auto-configs)?

Format as a table with columns: Legacy Class | Target Module | Target Package | Notes
Save as target/MIGRATION-PLAN.md
```

**Expected Output**: A mapping table from legacy classes to hexagonal module locations.

**Validation**: Every legacy class should appear in the mapping. No class should be left unmapped.

**Recovery**: If mappings look wrong, clarify the rules:
```
The core/ module must have ZERO infrastructure dependencies.
<LegacyEntity>.java uses @Entity -- it cannot go in core/ as-is.
The domain model in core/ must be a plain Java class without JPA annotations.
A separate JPA entity class goes in persistence/.
```

**Lessons Learned Entry**: Record whether Copilot correctly separated domain model from JPA entities, and any confusion about module boundaries.

---

## Phase 1 -- Scaffold

### Prompt 1.1: Generate Package Structure and Marker Class

**Context**: Reference marker class template and Modulith test template.

**Prompt**:
```
Read these reference files:
#file:reference/migration/template/src/main/java/MarkerClass.java.template
#file:reference/migration/template/src/test/java/ModulithStructureTest.java.template
#file:reference/migration/template/src/test/java/ArchitectureRulesTest.java.template

Now create the foundation for the migrated service in target/:

Service details:
- Service name: <SERVICE_NAME_PASCAL> (e.g., BillingService)
- Base package: <BASE_PACKAGE> (e.g., com.onefinancial.billing)
- DB prefix: <DB_PREFIX> (e.g., bs_)
- Property prefix: <PROPERTY_PREFIX> (e.g., billing.service)

Generate:
1. Marker class: target/src/main/java/<PACKAGE_PATH>/<SERVICE_NAME_PASCAL>Module.java
   - @Modulithic annotation (NOT @SpringBootApplication -- this is a library)
   - Replace all <PLACEHOLDER> values with actual service values

2. Package structure with package-info.java for each module:
   - core (Type.OPEN): target/src/main/java/<PACKAGE_PATH>/core/package-info.java
   - persistence: target/src/main/java/<PACKAGE_PATH>/persistence/package-info.java
   - rest: target/src/main/java/<PACKAGE_PATH>/rest/package-info.java
   - events: target/src/main/java/<PACKAGE_PATH>/events/package-info.java
   - autoconfigure: target/src/main/java/<PACKAGE_PATH>/autoconfigure/package-info.java

3. Tests:
   - target/src/test/java/<PACKAGE_PATH>/ModulithStructureTest.java
   - target/src/test/java/<PACKAGE_PATH>/ArchitectureRulesTest.java

Rules:
- core module MUST be Type.OPEN
- All other modules: allowedDependencies = {"core"}
- Follow the exact template patterns from the reference files
```

**Expected Output**: Marker class, 5 package-info.java files, 2 test files.

**Validation**:
- Marker class has `@Modulithic` (not `@SpringBootApplication`)
- Core `package-info.java` has `@ApplicationModule(type = Type.OPEN)`
- Other modules have `allowedDependencies = {"core"}`
- Test files reference the correct marker class

**Recovery**: If Copilot uses `@SpringBootApplication`:
```
WRONG: This is a library module, not a standalone application.
Use @Modulithic annotation, not @SpringBootApplication.
See #file:reference/migration/template/src/main/java/MarkerClass.java.template
```

**Lessons Learned Entry**: Record whether scaffold was correct on first try, and any placeholder substitution errors.

---

### Prompt 1.2: Generate Build Configuration

**Context**: Reference Makefile and docker-compose templates.

**Prompt**:
```
Read these reference files:
#file:reference/migration/template/Makefile.template
#file:reference/migration/template/docker/docker-compose.yml.template

Generate the build infrastructure for the target service:

1. target/Makefile -- replace all placeholders:
   - <SERVICE_NAME>: <ACTUAL_SERVICE_NAME>
   - Docker images: eclipse-temurin:21-jdk-alpine, node:20-alpine

2. target/docker/docker-compose.yml -- configure:
   - java-build service (Maven builds, no Docker socket)
   - java-build-testcontainers service (with Docker socket mount)
   - node-build service (if Angular frontend applies)

3. target/pom.xml skeleton with:
   - Parent: spring-boot-starter-parent 3.5.9
   - Java 21
   - Spring Modulith 1.4.7 BOM
   - Maven flatten plugin for ${revision} versioning
   - -parameters compiler flag

Rules:
- Docker images use CMD not ENTRYPOINT
- Non-root user paths: /home/builder/.m2, /home/builder/.npm
- NEVER run builds outside Docker
```

**Expected Output**: Makefile, docker-compose.yml, and pom.xml skeleton.

**Validation**: `make test` target exists and uses Docker Compose.

**Recovery**: If Copilot uses ENTRYPOINT:
```
WRONG: Docker images must use CMD, not ENTRYPOINT.
The Makefile passes full commands to the container.
See the reference Makefile.template for the correct pattern.
```

**Lessons Learned Entry**: Record any Docker configuration issues, especially around non-root user paths and socket mounts.

---

## Phase 2 -- Core Extraction

### Prompt 2.1: Extract Domain Model

**Context**: Legacy entity classes and reference domain model.

**Prompt**:
```
Read the hexagonal architecture rules:
#file:reference/migration/template/.github/instructions/core-domain.instructions.md

Read the reference domain model:
#file:reference/customer-registry-starter/src/main/java/com/onefinancial/customer/core/model/Customer.java

Now analyze this legacy entity:
#file:legacy/src/main/java/<LEGACY_ENTITY_PATH>

Based on the rules and reference pattern, create a pure domain model class:

1. Remove ALL JPA annotations (@Entity, @Table, @Column, @Id, @OneToMany, etc.)
2. Remove ALL Spring annotations (@Component, @Service, etc.)
3. Use named static factories instead of public constructors
4. Add compact constructor validation for value objects (use records)
5. Use UUID for the entity ID, generated in the static factory
6. Mask PII in toString() -- never expose full CPF/CNPJ/documents

Generate: target/src/main/java/<PACKAGE_PATH>/core/model/<DomainClass>.java

FORBIDDEN in this file:
- import jakarta.persistence.*
- import org.springframework.data.*
- import org.springframework.web.*
- import org.springframework.stereotype.* (@Service, @Component, @Repository)
- import java.sql.*
- Any annotation from the above packages

ALLOWED Spring imports in core (these are acceptable exceptions):
- org.springframework.transaction.annotation.Transactional (cross-cutting concern)
- org.springframework.modulith.* (module metadata)
```

**Expected Output**: A pure domain model class with static factories, validation, and no infrastructure imports.

**Validation**:
```bash
# Must return zero results
grep -n "import jakarta\.\|import org\.springframework\.data\.\|import org\.springframework\.web\." \
  target/src/main/java/**/core/model/<DomainClass>.java
```

**Recovery**: If Copilot leaves JPA annotations:
```
VIOLATION: The domain model in core/ must have ZERO infrastructure imports.
These lines violate the rule:
- Line X: import jakarta.persistence.Entity
- Line Y: @Column(name = "...")

Remove ALL Jakarta/Spring annotations. The JPA entity is a SEPARATE class
that lives in the persistence/ module, not in core/.
```

**Lessons Learned Entry**: Record whether JPA annotations were properly removed, quality of static factories, and any deviations from the Customer.java pattern.

---

### Prompt 2.2: Create Port Interfaces

**Context**: Legacy repository/service interfaces and reference ports.

**Prompt**:
```
Read the reference port interface:
#file:reference/customer-registry-starter/src/main/java/com/onefinancial/customer/core/port/CustomerRepository.java

Analyze the legacy service dependencies:
#file:legacy/src/main/java/<LEGACY_SERVICE_PATH>

For each external dependency in the legacy service, create a port interface in core/port/:

Rules:
- Port interfaces live in core/port/
- Method signatures use ONLY domain model types (from core/model/)
- No Spring Data, JPA, or infrastructure types in method signatures
- Use Optional<T> for nullable returns
- Use domain-specific exceptions, not infrastructure exceptions
- Name ports after the domain concept, not the technology
  (e.g., CustomerRepository, not JpaCustomerRepository)

Generate port interfaces in:
target/src/main/java/<PACKAGE_PATH>/core/port/
```

**Expected Output**: Port interfaces with clean method signatures using only domain types.

**Validation**: Port interfaces contain zero infrastructure imports.

**Recovery**: If Copilot puts Spring Data types in port signatures:
```
VIOLATION: Port interface must not reference Page, Pageable, or Sort from Spring Data.
These are infrastructure types. Use domain equivalents:
- Instead of Page<T>: return List<T> or a custom PageResult<T> record
- Instead of Pageable: accept int page, int size parameters
- Instead of Sort: accept a domain-specific SortCriteria enum
```

**Lessons Learned Entry**: Record port design quality -- were the right abstractions chosen? Were domain types used consistently?

---

### Prompt 2.3: Create Service Layer

**Context**: Legacy service classes, new ports, and new domain model.

**Prompt**:
```
Read the reference service class:
#file:reference/customer-registry-starter/src/main/java/com/onefinancial/customer/core/service/CustomerRegistryService.java

Now create the service layer for the migrated domain.

Use the domain model from: #file:target/src/main/java/<PACKAGE_PATH>/core/model/<DomainClass>.java
Use the port interfaces from: target/src/main/java/<PACKAGE_PATH>/core/port/

Rules:
- NO @Service or @Component annotations (wired via auto-config @Bean)
- Constructor injection of port interfaces only
- Business logic uses domain model methods (e.g., aggregate.validate())
- Throw domain-specific exceptions (define in core/exception/)
- Publish domain events via an EventPublisher port (if applicable)

Generate:
- target/src/main/java/<PACKAGE_PATH>/core/service/<ServiceClass>.java
- target/src/main/java/<PACKAGE_PATH>/core/exception/<DomainException>.java (if needed)
```

**Expected Output**: Service class depending only on ports, with domain exception classes.

**Validation**: No Spring stereotype annotations, no direct infrastructure calls.

**Recovery**: If Copilot adds `@Service`:
```
WRONG: Domain service classes must NOT have @Service annotation.
They are wired by auto-configuration using @Bean methods.
This keeps core/ free of Spring coupling.
Remove @Service and @Autowired. Use constructor injection with final fields.
```

**Lessons Learned Entry**: Record whether the service layer maintained clean separation, and if domain exceptions were properly created.

---

### Prompt 2.4: Create Core Unit Tests

**Context**: The new domain model and service layer.

**Prompt**:
```
Read the reference test patterns:
#file:reference/migration/template/.github/instructions/tests.instructions.md

Write unit tests for the core domain following TDD principles.

Test the domain model:
- Static factory methods with valid and invalid inputs
- Validation rules in value objects
- Business logic methods on aggregate roots
- Event generation (if applicable)
- PII masking in toString()

Test the service layer:
- Mock port interfaces with Mockito
- Test happy paths and error cases
- Verify domain events are published
- Verify domain exceptions are thrown for invalid operations

Generate tests in:
target/src/test/java/<PACKAGE_PATH>/core/model/<DomainClass>Test.java
target/src/test/java/<PACKAGE_PATH>/core/service/<ServiceClass>Test.java

Rules:
- JUnit 5 + Mockito
- No Spring context needed for core tests (pure unit tests)
- Test names: should_<expected>_when_<condition>
- Cover edge cases: null inputs, empty collections, boundary values
```

**Expected Output**: Unit tests for domain model and service layer.

**Validation**: Tests compile and pass without Spring context.

**Recovery**: If Copilot adds `@SpringBootTest`:
```
WRONG: Core unit tests must NOT use @SpringBootTest.
Core code has zero Spring dependencies, so tests should be pure unit tests.
Use @ExtendWith(MockitoExtension.class) for mocking ports.
No Spring context = faster tests = better TDD cycle.
```

**Lessons Learned Entry**: Record test coverage quality, whether TDD naming convention was followed, and any unnecessary Spring context usage.

---

## Phase 3 -- Adapters

### Prompt 3.1: Create Persistence Adapter

**Context**: Domain model, port interfaces, and reference persistence adapter.

**Prompt**:
```
Read the adapter rules:
#file:reference/migration/template/.github/instructions/adapters.instructions.md

Read the reference persistence adapter pattern. Look at these files in the reference project:
- The JPA entity (separate from domain model)
- The Spring Data repository interface
- The adapter class implementing the core port
- The bridge configuration

Now create the persistence adapter for our service:

Domain model: #file:target/src/main/java/<PACKAGE_PATH>/core/model/<DomainClass>.java
Port interface: #file:target/src/main/java/<PACKAGE_PATH>/core/port/<PortInterface>.java

Generate:
1. JPA Entity: target/src/main/java/<PACKAGE_PATH>/persistence/<EntityClass>Entity.java
   - Table name: <DB_PREFIX>_<table_name>
   - Implement Persistable<UUID> (avoids extra SELECT on insert)
   - Use @JdbcTypeCode(SqlTypes.JSON) for JSONB columns (NOT @Convert)
   - Use @BatchSize(size=25) on @OneToMany collections
   - Do NOT put mapping methods on the entity itself

2. Entity Mapper: target/src/main/java/<PACKAGE_PATH>/persistence/<EntityClass>EntityMapper.java
   - Package-private final class with private constructor (utility class)
   - Static toEntity(<DomainClass> model, boolean isNew) method
   - Static toDomain(<EntityClass>Entity entity) method
   - Follow the reference pattern in CustomerEntityMapper.java

3. Spring Data Repository: target/src/main/java/<PACKAGE_PATH>/persistence/<EntityClass>JpaRepository.java
   - Package-private interface

4. Adapter: target/src/main/java/<PACKAGE_PATH>/persistence/<EntityClass>PersistenceAdapter.java
   - Package-private class implementing the core port
   - Uses the entity mapper class (not methods on the entity)

5. Bridge Config: target/src/main/java/<PACKAGE_PATH>/persistence/<ServiceName>PersistenceConfiguration.java
   - PUBLIC @Configuration class
   - Exposes the package-private adapter as a bean
   - @EnableJpaRepositories with basePackageClasses
   - @EntityScan with basePackageClasses
   - @ConditionalOnMissingBean on the @Bean method (this is where overridability lives)
```

**Expected Output**: JPA entity, repository, adapter, and bridge configuration.

**Validation**:
- Bridge config is `public` class
- Adapter class is package-private
- JPA entity has `Persistable<UUID>` implementation
- No `@Convert` annotations (use `@JdbcTypeCode(SqlTypes.JSON)` instead)

**Recovery**: If Copilot uses `@Convert` for JSONB:
```
WRONG: Hibernate 6 does not support @Convert for JSONB properly.
Use @JdbcTypeCode(SqlTypes.JSON) instead.
See the reference project for the correct pattern.
```

**Lessons Learned Entry**: Record mapper method quality, whether `Persistable<UUID>` was applied, and bridge config correctness.

---

### Prompt 3.2: Create REST Adapter

**Context**: Domain model, service layer, and reference REST adapter.

**Prompt**:
```
Read the adapter rules:
#file:reference/migration/template/.github/instructions/adapters.instructions.md

Create the REST adapter for the migrated service.

Service class: #file:target/src/main/java/<PACKAGE_PATH>/core/service/<ServiceClass>.java

Generate:
1. Request/Response DTOs: target/src/main/java/<PACKAGE_PATH>/rest/dto/
   - Use Java records
   - Include validation annotations (@NotNull, @Size, etc.)
   - Include toModel() and static fromModel() mapper methods

2. Controller: target/src/main/java/<PACKAGE_PATH>/rest/<ServiceName>Controller.java
   - MUST be package-private (not public)
   - @RestController with @RequestMapping("/<resource-path>")
   - Constructor injection of the domain service
   - Use @Valid for request validation
   - Return ResponseEntity with proper HTTP status codes

3. Bridge Config: target/src/main/java/<PACKAGE_PATH>/rest/<ServiceName>RestConfiguration.java
   - PUBLIC @Configuration class
   - Exposes the package-private controller
   - @ConditionalOnMissingBean on the bean method

Rules:
- Spring Boot 3.2+ requires -parameters compiler flag for @PathVariable/@RequestParam
  without explicit names. Either add the flag or use explicit names.
- @WebMvcTest needs @SpringBootApplication inner class in tests (library has none)
```

**Expected Output**: DTOs, controller, and bridge configuration.

**Validation**:
- Controller class visibility is package-private
- Bridge config class visibility is public
- DTOs are records with validation annotations

**Recovery**: If Copilot makes the controller public:
```
WRONG: Controllers MUST be package-private (default visibility), not public.
The bridge config exposes them. This is the hexagonal adapter pattern --
the controller is an implementation detail, not a public API.
Remove the 'public' modifier from the controller class declaration.
```

**Lessons Learned Entry**: Record controller visibility, DTO design, and whether `-parameters` flag was addressed.

---

### Prompt 3.3: Create Events Adapter

**Context**: Domain events and reference events adapter.

**Prompt**:
```
Read the adapter rules, paying special attention to the events section:
#file:reference/migration/template/.github/instructions/adapters.instructions.md

Create the events adapter for the migrated service.

Domain events: #file:target/src/main/java/<PACKAGE_PATH>/core/event/

Generate:
1. Event publisher adapter: target/src/main/java/<PACKAGE_PATH>/events/<ServiceName>EventPublisher.java
   - Package-private class implementing the event publisher port from core
   - Uses Spring's ApplicationEventPublisher
   - NEVER include PII in published events (mask documents)

2. Bridge Config: target/src/main/java/<PACKAGE_PATH>/events/<ServiceName>EventsConfiguration.java
   - PUBLIC @Configuration class
   - @ConditionalOnMissingBean on the bean method

CRITICAL: The events auto-configuration MUST run BEFORE core auto-configuration.
This ensures the Spring adapter registers before the NoOp fallback bean.
This ordering is handled in Phase 4 (auto-config), but note it here.
```

**Expected Output**: Event publisher adapter and bridge configuration.

**Validation**: No PII in event payloads, bridge config is public.

**Recovery**: If events contain PII:
```
VIOLATION: Events MUST NOT contain PII (Personally Identifiable Information).
Mask documents: "123.456.789-00" -> "***.***.789-00"
Mask emails: "user@example.com" -> "u***@example.com"
Never include full CPF, CNPJ, or other sensitive identifiers in events.
```

**Lessons Learned Entry**: Record PII handling, event structure quality, and whether ordering requirement was noted.

---

### Prompt 3.4: Create Adapter Tests

**Context**: All adapters created in Phase 3.

**Prompt**:
```
Read the testing rules:
#file:reference/migration/template/.github/instructions/tests.instructions.md

Create tests for the adapters:

1. Persistence adapter test (Integration):
   - Name: <ServiceName>PersistenceAdapterIntegrationTest.java
   - Use @DataJpaTest (disable Liquibase: spring.liquibase.enabled=false)
   - Test CRUD operations through the adapter
   - Verify entity<->model mapping roundtrips
   - For JSONB columns: PostgreSQL normalizes JSON (reorders keys) -- assertions
     must account for this

2. REST adapter test:
   - Name: <ServiceName>ControllerTest.java
   - Use @WebMvcTest with a @SpringBootApplication inner class (library has none)
   - Mock the domain service
   - Test request validation, response mapping, HTTP status codes

3. Events adapter test:
   - Test event publishing with a mock ApplicationEventPublisher
   - Verify PII is masked in published events

Generate tests in:
target/src/test/java/<PACKAGE_PATH>/persistence/
target/src/test/java/<PACKAGE_PATH>/rest/
target/src/test/java/<PACKAGE_PATH>/events/
```

**Expected Output**: Integration and unit tests for all adapters.

**Validation**:
- Persistence test disables Liquibase
- REST test has `@SpringBootApplication` inner class
- Event test verifies PII masking

**Recovery**: If persistence test doesn't disable Liquibase:
```
@DataJpaTest includes LiquibaseAutoConfiguration by default.
Without Liquibase changesets, the test will fail.
Add: @TestPropertySource(properties = "spring.liquibase.enabled=false")
```

**Lessons Learned Entry**: Record testing patterns that required manual fixes, especially around `@DataJpaTest` and `@WebMvcTest` configuration.

---

## Phase 4 -- Auto-Configuration

### Prompt 4.1: Create Auto-Configuration Classes

**Context**: All bridge configs from Phase 3 and reference auto-config.

**Prompt**:
```
Read the auto-configuration rules:
#file:reference/migration/template/.github/instructions/autoconfig.instructions.md

Create auto-configuration classes for the migrated service.

Bridge configs available:
- #file:target/src/main/java/<PACKAGE_PATH>/persistence/<ServiceName>PersistenceConfiguration.java
- #file:target/src/main/java/<PACKAGE_PATH>/rest/<ServiceName>RestConfiguration.java
- #file:target/src/main/java/<PACKAGE_PATH>/events/<ServiceName>EventsConfiguration.java

Generate auto-config classes in target/src/main/java/<PACKAGE_PATH>/autoconfigure/:

IMPORTANT: The reference uses a dual-gate pattern. Adapter auto-configs require BOTH
the master switch AND the individual feature flag to be true:
@ConditionalOnProperty(prefix = "<PROPERTY_PREFIX>", name = {"enabled", "features.<feature>"}, havingValue = "true")
The core auto-config uses only the master switch:
@ConditionalOnProperty(name = "<PROPERTY_PREFIX>.enabled", havingValue = "true")
Note: matchIfMissing defaults to false when omitted, which is the desired secure-by-default
behavior. Either explicitly set it or omit it — both are acceptable.

Use kebab-case feature names in properties (Spring Boot relaxed binding maps them to
camelCase Java fields automatically):
- features.publish-events (not features.events)
- features.persistence-jpa (not features.persistence)
- features.rest-api (not features.rest)

1. <ServiceName>EventsAutoConfiguration.java
   - MUST be ordered BEFORE core auto-config
   - Dual gate: prefix="<PROPERTY_PREFIX>", name={"enabled", "features.publish-events"}
   - @Import(<ServiceName>EventsConfiguration.class)
   - The bridge config's @Bean method has @ConditionalOnMissingBean (not here)

2. <ServiceName>CoreAutoConfiguration.java
   - Gate: @ConditionalOnProperty(name = "<PROPERTY_PREFIX>.enabled", havingValue = "true")
   - Creates the domain service bean
   - @ConditionalOnMissingBean on ALL beans in this class (fallbacks + service)
   - Provides fallback beans (no-op event publisher, in-memory repository)

3. <ServiceName>PersistenceAutoConfiguration.java
   - MUST be ordered AFTER core auto-config
   - Dual gate: prefix="<PROPERTY_PREFIX>", name={"enabled", "features.persistence-jpa"}
   - @Import(<ServiceName>PersistenceConfiguration.class)
   - The bridge config's @Bean method has @ConditionalOnMissingBean (not here)

4. <ServiceName>RestAutoConfiguration.java
   - MUST be ordered AFTER core auto-config
   - Dual gate: prefix="<PROPERTY_PREFIX>", name={"enabled", "features.rest-api"}
   - @Import(<ServiceName>RestConfiguration.class)
   - The bridge config's @Bean method has @ConditionalOnMissingBean (not here)

EVERY auto-config class MUST have the structured header comment:
/*
 * ORDERING: runs before/after <which auto-configs>
 * GATE: @ConditionalOnProperty — which properties must be true
 * BRIDGE: @Import(<BridgeConfig>.class) — if applicable
 * OVERRIDABLE: <list of @ConditionalOnMissingBean beans> — if applicable
 */

5. META-INF registration file:
   target/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
   - List all auto-config classes, one per line
   - Order: Events, Core, Persistence, REST
```

**Expected Output**: 4 auto-config classes + META-INF registration file.

**Validation**:

Check that `@ConditionalOnMissingBean` is used on all default/fallback beans:
- Core auto-config: every `@Bean` that provides a default (fallback repo, no-op publisher,
  service) MUST have `@ConditionalOnMissingBean` so consumers can override
- Bridge configs: every `@Bean` that creates an adapter MUST have `@ConditionalOnMissingBean`
  so host apps can substitute their own implementation
- Exception: infrastructure beans like Liquibase `SpringLiquibase` do NOT need
  `@ConditionalOnMissingBean` because they are not meant to be overridden

```bash
# Verify no matchIfMissing = true anywhere
grep -r "matchIfMissing.*true" target/src/main/java/**/autoconfigure/
# Expected: zero results (secure-by-default means false, which is the default when omitted)

# Verify dual-gate on adapter auto-configs
grep -A3 '@ConditionalOnProperty' target/src/main/java/**/autoconfigure/*AutoConfiguration.java
# Expected: adapter auto-configs show name = {"enabled", "features.<name>"}
```

**Recovery**: If Copilot uses `matchIfMissing = true`:
```
SECURITY VIOLATION: Feature flags must be OFF by default (secure-by-default).
Remove matchIfMissing = true — the default (false) is the correct behavior.
Features should only activate when explicitly enabled in configuration.
```

**Lessons Learned Entry**: Record feature flag defaults, header comment completeness, ordering correctness, and META-INF registration accuracy.

---

### Prompt 4.2: Create Properties Configuration Class

**Context**: Auto-config feature flags.

**Prompt**:
```
Create a @ConfigurationProperties class for the service:

Generate: target/src/main/java/<PACKAGE_PATH>/autoconfigure/<ServiceName>Properties.java

Requirements:
- Prefix: <PROPERTY_PREFIX>
- Nested Features class with boolean fields for each feature
- All features default to false (secure-by-default)
- Use @ConfigurationProperties (not @Value)
- Document each property with Javadoc

Example structure (use camelCase fields — Spring Boot relaxed binding maps
kebab-case YAML keys like rest-api to restApi automatically):

@ConfigurationProperties(prefix = "<PROPERTY_PREFIX>")
public class <ServiceName>Properties {
    private boolean enabled = false;  // master switch
    private Features features = new Features();

    public static class Features {
        private boolean restApi = false;         // -> features.rest-api in YAML
        private boolean persistenceJpa = false;  // -> features.persistence-jpa in YAML
        private boolean publishEvents = false;   // -> features.publish-events in YAML
        // getters/setters for each field
    }
}

Note: There is no "core" feature flag. Core auto-config is gated only by the
master "enabled" switch. Individual feature flags control the adapters.
```

**Expected Output**: Properties class with nested Features.

**Validation**: All feature defaults are `false`.

**Recovery**: If defaults are `true`:
```
WRONG: All feature flags must default to false.
This is the "secure-by-default" principle -- features are opt-in.
Change all default values from true to false.
```

**Lessons Learned Entry**: Record whether secure-by-default was maintained.

---

### Prompt 4.3: Create Auto-Configuration Tests

**Context**: Auto-config classes and reference test patterns.

**Prompt**:
```
Read the testing rules for auto-configuration:
#file:reference/migration/template/.github/instructions/tests.instructions.md

Create ApplicationContextRunner tests for each auto-config class:

For each auto-config, test:
1. Bean is NOT registered when feature flag is absent (default off)
2. Bean IS registered when feature flag is set to true
3. Bean can be overridden by consumer (@ConditionalOnMissingBean works)
4. Fallback beans are used when adapter is not present

Generate: target/src/test/java/<PACKAGE_PATH>/autoconfigure/<ServiceName><Feature>AutoConfigurationTest.java

Use ApplicationContextRunner pattern:
```java
private final ApplicationContextRunner runner = new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(<ServiceName><Feature>AutoConfiguration.class));

@Test
void should_not_register_beans_when_feature_disabled() {
    runner.run(context -> assertThat(context).doesNotHaveBean(<BeanType>.class));
}

@Test
void should_register_beans_when_feature_enabled() {
    runner.withPropertyValues("<PROPERTY_PREFIX>.features.<feature>=true")
        .run(context -> assertThat(context).hasSingleBean(<BeanType>.class));
}
```
```

**Expected Output**: Auto-config test classes using `ApplicationContextRunner`.

**Validation**: Tests verify both enabled and disabled states for each feature.

**Recovery**: If Copilot uses `@SpringBootTest` instead of `ApplicationContextRunner`:
```
WRONG: Auto-config tests should use ApplicationContextRunner, not @SpringBootTest.
ApplicationContextRunner is faster and more focused -- it tests exactly the
auto-configuration behavior without loading the full application context.
```

**Lessons Learned Entry**: Record whether `ApplicationContextRunner` was used correctly, and whether both states (enabled/disabled) were tested.

---

## Phase 5 -- Frontend (Optional)

### Prompt 5.1: Create Angular Library Structure

**Context**: Reference Angular workspace structure.

**Prompt**:
```
Read the Angular rules:
#file:reference/migration/template/.github/instructions/angular.instructions.md

Set up an Angular library in target/frontend/ for the migrated service:

1. Create workspace configuration:
   - Angular CLI 17
   - Library project: <angular-lib-name>
   - Prefix: <ANGULAR_PREFIX>

2. Create the public API barrel:
   - target/frontend/projects/<angular-lib-name>/src/public-api.ts
   - MUST have at least one export (empty barrel fails ng-packagr)

3. Create Jest configuration:
   - jest.config.ts (NOT jest.config.js -- ts-node is required)
   - Use jest-preset-angular@14.6.2
   - setupFilesAfterEnv (NOT setupFilesAfterSetup)
   - Import setupZoneTestEnv() from jest-preset-angular/setup-env/zone

Rules:
- ts-node is required as devDependency for Jest to parse .ts config
- All components: standalone + ChangeDetectionStrategy.OnPush
- Signal-based state: WritableSignal private with _ prefix, asReadonly() for public
- CSS custom properties: --<ANGULAR_PREFIX>-*
- NEVER name @Input() as 'formControl' (collides with Angular directive)
```

**Expected Output**: Angular library workspace with Jest configuration.

**Validation**: `ng build <angular-lib-name>` succeeds (via Docker).

**Recovery**: If Jest config uses wrong setup key:
```
WRONG: The Jest config key is "setupFilesAfterEnv", not "setupFilesAfterSetup".
Also, use setupZoneTestEnv() from 'jest-preset-angular/setup-env/zone'
(the old 'jest-preset-angular/setup-jest' import is deprecated).
```

**Lessons Learned Entry**: Record any Angular-specific configuration issues, especially Jest setup and ng-packagr requirements.

---

### Prompt 5.2: Migrate Angular Components

**Context**: Legacy Angular components and reference component patterns.

**Prompt**:
```
Analyze the legacy Angular components in:
legacy/src/app/ (or legacy/frontend/src/app/)

For each component, migrate to the library pattern:

1. Convert to standalone component (remove from NgModule)
2. Add ChangeDetectionStrategy.OnPush
3. Convert imperative state to signals:
   - Private: _items = signal<Item[]>([])
   - Public: items = this._items.asReadonly()
4. Use <ANGULAR_PREFIX>- prefix for selectors
5. Use --<ANGULAR_PREFIX>-* for CSS custom properties
6. Implement i18n with fallback chain:
   host overrides -> built-in[locale] -> built-in['en'] -> key

Generate components in:
target/frontend/projects/<angular-lib-name>/src/lib/

Include Jest tests for each component (.spec.ts files).
```

**Expected Output**: Migrated standalone components with signal-based state and tests.

**Validation**: All components use `OnPush` and `standalone: true`.

**Recovery**: If Copilot doesn't convert to signals:
```
Components should use signal-based state management.
Instead of:
  items: Item[] = [];

Use:
  private _items = signal<Item[]>([]);
  readonly items = this._items.asReadonly();

Signals enable fine-grained change detection with OnPush strategy.
```

**Lessons Learned Entry**: Record signal conversion quality, OnPush adoption, and i18n fallback chain implementation.

---

## Inter-Phase Validation

> **Purpose**: Run these validation prompts BETWEEN phases to catch architectural drift early.
> Each prompt verifies the output of the preceding phase before proceeding to the next one.
> This prevents cascading errors where a Phase 1 mistake invalidates all of Phase 2-4 work.

### Prompt G.1: Phase 1 Complete — Validate Scaffold

**When**: After completing all Phase 1 prompts, before starting Phase 2.

**Prompt**:
```
Validate the scaffold generated in Phase 1 before proceeding to core extraction.

Open and review these files in @workspace, checking each gate:

1. **Marker class** — Open the main marker class file (e.g., target/src/main/java/<PACKAGE_PATH>/<ServiceName>Module.java).
   Verify it has `@Modulithic` annotation (NOT `@SpringBootApplication`).

2. **Core package-info** — Open target/src/main/java/<PACKAGE_PATH>/core/package-info.java.
   Verify it contains `Type.OPEN` in the `@ApplicationModule` annotation.

3. **Other module package-info files** — Open each of these:
   - target/src/main/java/<PACKAGE_PATH>/persistence/package-info.java
   - target/src/main/java/<PACKAGE_PATH>/rest/package-info.java
   - target/src/main/java/<PACKAGE_PATH>/events/package-info.java
   - target/src/main/java/<PACKAGE_PATH>/autoconfigure/package-info.java
   Verify each has `allowedDependencies = {"core"}`.

4. **ModulithStructureTest** — Open target/src/test/java/<PACKAGE_PATH>/ModulithStructureTest.java.
   Verify it references the correct marker class by name.

5. **ArchitectureRulesTest** — Open target/src/test/java/<PACKAGE_PATH>/ArchitectureRulesTest.java.
   Verify the base package string is correct.

Report: PASS or FAIL for each gate. If any FAIL, fix before proceeding to Phase 2.
```

---

### Prompt G.2: Phase 2 Complete — Validate Core Extraction

**When**: After completing all Phase 2 prompts, before starting Phase 3.

**Prompt**:
```
Validate the core extraction from Phase 2 before proceeding to adapter creation.

Review the files in @workspace, checking each gate:

1. **Core isolation** — Open every `.java` file under target/src/main/java/**/core/.
   Scan import statements for these FORBIDDEN packages:
   - `jakarta.persistence`
   - `org.springframework.data`
   - `org.springframework.web`
   - `org.springframework.stereotype`
   - `org.springframework.boot`
   - `org.springframework.context`
   - `org.springframework.beans`
   ALLOWED exceptions: `org.springframework.transaction.annotation.Transactional` and `org.springframework.modulith.*`.
   Expected: zero forbidden imports.

2. **Port interfaces** — Open each file in target/src/main/java/<PACKAGE_PATH>/core/port/.
   Verify each file declares a Java `interface` (not a class).

3. **Domain model** — Open files in target/src/main/java/<PACKAGE_PATH>/core/model/.
   Verify classes use named static factory methods instead of public constructors.

4. **PII masking** — Check `toString()` methods in model classes.
   Documents (CPF/CNPJ) must be masked (e.g., `***.***.789-00`).

5. **Domain events** — If events exist in core/event/, verify they do not contain PII fields.

6. **Service class** — Open the service class in core/service/.
   Verify it has NO `@Service` or `@Component` annotation.

7. **Core unit tests** — Open test files under target/src/test/java/**/core/.
   Verify they use `@ExtendWith(MockitoExtension.class)`, NOT `@SpringBootTest`.

Report: PASS or FAIL for each gate. If core isolation fails, fix before Phase 3.
```

---

### Prompt G.3: Phase 3 Complete — Validate Adapters

**When**: After completing all Phase 3 prompts, before starting Phase 4.

**Prompt**:
```
Validate the adapters from Phase 3 before proceeding to auto-configuration.

Review the files in @workspace, checking each gate:

1. **Bridge config visibility** — Open each `*Configuration.java` file in these directories:
   - target/src/main/java/<PACKAGE_PATH>/persistence/
   - target/src/main/java/<PACKAGE_PATH>/rest/
   - target/src/main/java/<PACKAGE_PATH>/events/
   Verify each has `public class` (bridge configs MUST be public).

2. **Adapter visibility** — Open each adapter/controller/publisher class in the same directories.
   Verify they do NOT have the `public` modifier (must be package-private).
   Look for: `*Adapter.java`, `*Controller.java`, `*Publisher.java`.

3. **@ConditionalOnMissingBean** — In each bridge config, verify every `@Bean` method
   also has `@ConditionalOnMissingBean` annotation nearby.

4. **Persistable<UUID>** — Open each JPA entity in persistence/.
   Verify the class `implements Persistable<UUID>`.

5. **Entity mapper** — Verify there is a separate `*EntityMapper.java` utility class
   (NOT mapper methods directly on the JPA entity class).

6. **JSONB handling** — In JPA entity files, verify JSONB columns use
   `@JdbcTypeCode(SqlTypes.JSON)`, NOT `@Convert`.

7. **Controller test** — Open the REST controller test file.
   Verify it contains a `@SpringBootApplication` inner class (required for library modules).

Report: PASS or FAIL for each gate. Fix adapter issues before Phase 4 wiring.
```

---

### Prompt G.4: Phase 4 Complete — Validate Auto-Configuration

**When**: After completing all Phase 4 prompts, before starting Phase 5 or Verification.

**Prompt**:
```
Validate the auto-configuration from Phase 4 before final verification.

Review the files in @workspace, checking each gate:

1. **Dual-gate pattern** — Open each adapter auto-config in target/src/main/java/<PACKAGE_PATH>/autoconfigure/:
   - `*EventsAutoConfiguration.java`
   - `*PersistenceAutoConfiguration.java`
   - `*RestAutoConfiguration.java`
   Verify each has `@ConditionalOnProperty` with `name = {"enabled", "features.<name>"}` (dual-gate).

2. **Core single gate** — Open `*CoreAutoConfiguration.java`.
   Verify it uses only the master switch: `name = "<prefix>.enabled"` (no feature sub-key).

3. **No matchIfMissing = true** — Search all auto-config files for `matchIfMissing`.
   Verify the string `matchIfMissing = true` does NOT appear anywhere.

4. **Events ordering** — Open the Events auto-config class.
   Verify it has `@AutoConfiguration(before = ...)` referencing the Core auto-config.

5. **META-INF registration** — Open target/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports.
   Verify ALL auto-config classes are listed (Events, Core, Persistence, REST).

6. **Header comments** — Each auto-config class should have a structured header comment
   with ORDERING, GATE, BRIDGE, and OVERRIDABLE sections.

7. **Kebab-case properties** — Verify feature flag names use kebab-case in annotations:
   `publish-events`, `persistence-jpa`, `rest-api`.

8. **Properties class defaults** — Open the `*Properties.java` class.
   Verify all feature boolean fields default to `false`.

Report: PASS or FAIL for each gate. Fix auto-config issues before proceeding.
```

---

## Verification

### Prompt V.1: Self-Review

**Context**: All generated code in `target/`.

**Prompt**:
```
Read the migration scorecard:
#file:reference/migration/scorecard.md

Read the migration workflow rules:
#file:target/.github/instructions/migration-workflow.instructions.md

Now review ALL code you generated in target/src/ against the scorecard dimensions.

For each dimension, report:
1. PASS or FAIL
2. Evidence (file path and relevant code)
3. Fix needed (if FAIL)

Check specifically:
- [ ] Core has ZERO infrastructure imports (except @Transactional and @Modulithic)
- [ ] All modules have package-info.java with correct @ApplicationModule
- [ ] Bridge configs are public, adapters are package-private
- [ ] @ConditionalOnMissingBean on all fallback beans (core) and bridge config beans
- [ ] Adapter auto-configs use dual-gate: name = {"enabled", "features.<name>"}
- [ ] Core auto-config gated only by master switch (<prefix>.enabled)
- [ ] No matchIfMissing = true (false is the default when omitted)
- [ ] Events auto-config runs before core auto-config
- [ ] META-INF/spring/...AutoConfiguration.imports lists all auto-configs
- [ ] ModulithStructureTest compiles and references correct marker class
- [ ] No PII in events, toString(), or exceptions
- [ ] No @ComponentScan in auto-config (use @Import instead)
- [ ] Controllers are package-private
- [ ] JPA entities implement Persistable<UUID>
- [ ] Separate entity mapper class (not methods on JPA entity)
- [ ] @JdbcTypeCode(SqlTypes.JSON) used instead of @Convert for JSONB

Format: markdown table with Dimension | Status | Evidence | Fix columns
```

**Expected Output**: A scorecard review table with PASS/FAIL for each dimension.

**Validation**: All dimensions should be PASS. If any FAIL, fix before proceeding.

**Recovery**: Use the specific recovery prompts from the relevant phase above.

**Lessons Learned Entry**: Record which dimensions failed self-review and whether fixes were applied correctly.

---

### Prompt V.2: Architecture Verification Review

**Context**: Generated project in `target/`.

**Prompt**:
```
Review the generated code in @workspace to verify architecture compliance. For each check,
open the relevant files and report PASS or FAIL with evidence.

1. **Core isolation** — Open every `.java` file under target/src/main/java/**/core/.
   Scan all `import` statements. Report any imports from these FORBIDDEN packages:
   - `jakarta.persistence`
   - `org.springframework.data`
   - `org.springframework.web`
   - `org.springframework.stereotype`
   ALLOWED exceptions: `org.springframework.transaction.annotation.Transactional` and `org.springframework.modulith.*`.
   Expected: zero forbidden imports.

2. **Bridge config visibility** — Open each `*Configuration.java` file in:
   - target/src/main/java/<PACKAGE_PATH>/persistence/
   - target/src/main/java/<PACKAGE_PATH>/rest/
   - target/src/main/java/<PACKAGE_PATH>/events/
   Verify each has `public class` declaration.
   Expected: one public configuration class per adapter module.

3. **Controller visibility** — Open each `*Controller.java` in target/src/main/java/<PACKAGE_PATH>/rest/.
   Verify the class declaration does NOT have the `public` modifier (must be package-private).
   Expected: zero public controllers.

4. **Dual-gate on adapter auto-configs** — Open each auto-config in target/src/main/java/<PACKAGE_PATH>/autoconfigure/:
   - `*EventsAutoConfiguration.java`
   - `*PersistenceAutoConfiguration.java`
   - `*RestAutoConfiguration.java`
   Verify each has `@ConditionalOnProperty` with `name = {"enabled", "features.<name>"}`.
   Then open `*CoreAutoConfiguration.java` and verify it uses only `name = "<prefix>.enabled"` (no feature sub-key).

5. **Feature flag defaults** — In the same auto-config files, verify the string
   `matchIfMissing = true` does NOT appear anywhere.
   Expected: zero occurrences (false is the default when omitted).

6. **META-INF registration** — Open target/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports.
   Verify all auto-config classes are listed (Events, Core, Persistence, REST).

Report each check as PASS or FAIL with the file path and relevant line.
```

**Expected Output**: All checks PASS.

**Validation**: Zero violations across all checks.

**Lessons Learned Entry**: Record which verification checks failed and why.

---

## Lessons Learned

### Prompt L.1: Generate Migration Lessons Learned

**Context**: Completed migration in `target/`, lessons learned template.

**Prompt**:
```
Read the migration lessons template:
#file:target/MIGRATION-LESSONS.md

Review all the code you generated in target/src/ during this migration session.

For each phase (0-5), fill in the template sections honestly:
- What you generated correctly on first try
- Where you deviated from the reference patterns in #file:reference/
- What legacy patterns you found that weren't covered by the instructions
- What was ambiguous in the prompts you received

Be specific -- reference file names and line numbers where possible.
This report will be used to improve the migration framework for future services.

Fill in the Summary section:
- What Worked Well (patterns that generated clean code)
- What Needs Improvement (patterns that required multiple corrections)
- Suggested Prompt Changes (specific improvements to prompts)
- Suggested Instruction File Changes (specific improvements to instructions)
- Patterns Not Covered (legacy patterns not in current instructions)

Save the completed report as: target/MIGRATION-LESSONS.md
```

**Expected Output**: A completed `MIGRATION-LESSONS.md` with honest assessment of each phase.

**Validation**: All phase sections are filled in, summary tables have entries.

**Recovery**: If the report is too generic:
```
The lessons learned must be SPECIFIC to this migration.
Instead of "some patterns needed fixing", say:
"Phase 3 Prompt 3.1 generated a persistence adapter that used @Convert
instead of @JdbcTypeCode(SqlTypes.JSON) for the metadata column.
Suggested fix: add explicit 'use @JdbcTypeCode, not @Convert' to the prompt."
```

**Lessons Learned Entry**: This IS the lessons learned entry -- it generates the full report.

---

## Quick Reference: Prompt Checklist

Before starting each phase, verify. Run the corresponding **G.x** validation prompt between phases.

| Phase | Tier | Pre-Condition | Gate | Validation |
|-------|------|---------------|------|------------|
| 0 | All | Legacy code accessible in workspace | Tier assessment + analysis report | — |
| 1 | All | Phase 0 analysis complete | ModulithStructureTest compiles | Run **G.1** |
| 2 | All | Phase 1 scaffold validated | Zero infra imports in core/ | Run **G.2** |
| 3 | All | Phase 2 core validated | Bridge configs exist for all adapters | Run **G.3** |
| 4 | All | Phase 3 adapters validated | @Bean count == @ConditionalOnMissingBean count | Run **G.4** |
| 5 | Advanced* | Phase 4 auto-config validated | Angular build succeeds (if applicable) | — |
| V | All | All phases complete | All verification checks PASS | — |
| L | All | Verification complete | MIGRATION-LESSONS.md filled in | — |

\* Phase 5 prompts only apply to Advanced tier. For Simple/Standard tiers, Phase 5 scorecard checks auto-pass with "N/A" (no frontend detected).
