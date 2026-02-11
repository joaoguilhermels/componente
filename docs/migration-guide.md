# Migration Guide: Hexagonal Architecture with Spring Modulith

A step-by-step guide for migrating existing Spring Boot microservices to hexagonal
architecture enforced by Spring Modulith. Based on the OneFinancial Customer Registry reference
implementation.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Prerequisites](#2-prerequisites)
3. [Phase 1: Scaffold (1-2 days)](#3-phase-1-scaffold-1-2-days)
4. [Phase 2: Core Extraction (3-5 days)](#4-phase-2-core-extraction-3-5-days)
5. [Phase 3: Adapters (3-5 days)](#5-phase-3-adapters-3-5-days)
6. [Phase 4: Auto-Configuration (2-3 days)](#6-phase-4-auto-configuration-2-3-days)
7. [Phase 5: Frontend Migration](#7-phase-5-frontend-migration)
8. [Troubleshooting Guide](#8-troubleshooting-guide)
9. [Copilot-Assisted Migration](#9-copilot-assisted-migration)
10. [Pragmatic vs. Strict Hexagonal](#10-pragmatic-vs-strict-hexagonal)

### Related Documentation

| Document | Covers |
|----------|--------|
| [Copilot Migration Strategy](copilot-migration-strategy.md) | Step-by-step Copilot Chat workflow (workspace setup, phases 0-5) |
| [Copilot Prompts](../migration/copilot-prompts.md) | 20 ready-to-paste prompts organized by phase |
| [Migration Scorecard](../migration/scorecard.md) | 23-dimension progress tracker (13 automated + 10 manual) |
| [Migration Templates README](../migration/README.md) | Template files and usage instructions |
| [CONTRIBUTING.md](../CONTRIBUTING.md) | Development workflow, branch naming, PR process |
| [README.md](../README.md) | Architecture diagrams, REST API docs, event schemas |

---

## 1. Overview

### Why Migrate?

Most Spring Boot services start as a single package with entities, repositories, services,
and controllers mixed together. This works until the service grows, gets shared across teams,
or needs to be consumed as a library. At that point the lack of boundaries causes:

- Accidental coupling between business logic and infrastructure
- Difficulty testing domain logic in isolation
- No enforcement of module contracts -- any class can import any other class
- Infrastructure choices (JPA, REST framework) leaking into domain models

### What This Guide Covers

This guide walks through migrating a service to **hexagonal architecture** (ports and
adapters) enforced by **Spring Modulith** module boundaries. The reference implementation
is the OneFinancial Customer Registry, a Tier 3 library with full auto-configuration.

### Tiered Complexity Model

Not every service needs the same level of architectural rigor. Choose your tier based on
actual complexity, not aspiration.

```
Tier 1 (Simple)        Tier 2 (Standard)       Tier 3 (Library)
--------------------   --------------------    --------------------
< 5 entities           Business rules          Consumed as dependency
No complex rules       Validation pipeline     Multiple consumers
Single team            Domain events           Feature flags
                       Multiple adapters       SPIs for extension
                                               Auto-configuration

What you get:          What you add:           What you add:
- Module boundaries    - Full hexagonal        - Bridge configs
- ModulithStructure    - Port interfaces       - @AutoConfiguration
  Test only            - Service pipeline      - @ConditionalOnProperty
- package-info.java    - Entity mappers        - @ConditionalOnMissingBean
                                               - Fallback beans
                                               - META-INF registration
```

**Most services are Tier 1.** The reference project is Tier 3. Do not force Tier 3 patterns
on a Tier 1 service. Start with module boundaries and the structure test; escalate only when
you hit a real problem that hexagonal architecture solves.

### Architecture Overview

```
                    +-------------------------------------------+
                    |              autoconfigure/                |
                    |  @AutoConfiguration + @ConditionalOn...   |
                    |  @Import(BridgeConfigurations)            |
                    +----+-------+--------+--------+------------+
                         |       |        |        |
              +----------+  +----+---+ +--+-----+ +--------+
              |             |        | |        | |        |
         +----v----+  +----v----+ +-v---+ +----v----+ +---v------+
         |  rest/  |  |persist/ | |evts/| |observe/ | |migrate/  |
         | Bridge  |  | Bridge  | |Brdg | | Bridge  | |          |
         | Config  |  | Config  | |Cfg  | | Config  | |          |
         +----+----+  +----+----+ +-+---+ +----+----+ +----+-----+
              |             |       |          |            |
              +------+------+-------+----------+------------+
                     |
                +----v----+
                |  core/  |
                | model/  |
                | port/   |   ZERO infrastructure dependencies
                | spi/    |
                | event/  |
                | service/|
                +---------+
```

Dependency rule: arrows point **inward**. Core depends on nothing. Adapters depend on core.
Auto-configuration wires adapters to core.

> **Exception**: `@Transactional` (`org.springframework.transaction.annotation`) is
> permitted in core services because transaction boundaries are a cross-cutting concern,
> not an infrastructure coupling. See [Section 10](#10-pragmatic-vs-strict-hexagonal).

---

## 2. Prerequisites

### Versions

| Dependency          | Version  | Notes                                    |
|---------------------|----------|------------------------------------------|
| Java                | 21+      | Required for records, pattern matching   |
| Spring Boot         | 3.x      | Reference uses 3.5.9                     |
| Spring Modulith     | 1.4.x    | Reference uses 1.4.7                     |
| Maven               | 3.9+     | Or Gradle 8.x with equivalent config     |
| Docker + Compose    | Latest   | All builds run inside containers         |

### Maven Dependencies

Add the Spring Modulith test dependency to your project:

```xml
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

If using the BOM:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-bom</artifactId>
            <version>1.4.7</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Compiler Configuration

Spring Boot 3.2+ requires the `-parameters` flag for `@PathVariable` and `@RequestParam`
to work without explicit `name` attributes:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <parameters>true</parameters>
    </configuration>
</plugin>
```

### Docker Build Environment

All builds run inside Docker. Never run Java or Node locally. Here is a minimal
`docker-compose.yml` for the build environment:

```yaml
# docker/docker-compose.yml
services:
  java-build:
    build:
      context: .
      dockerfile: Dockerfile.java
    volumes:
      - ../:/workspace
      - maven-cache:/home/builder/.m2
    working_dir: /workspace

  java-build-testcontainers:
    extends:
      service: java-build
    volumes:
      - ../:/workspace
      - maven-cache:/home/builder/.m2
      - /var/run/docker.sock:/var/run/docker.sock
    environment:
      TESTCONTAINERS_RYUK_DISABLED: "true"
      TESTCONTAINERS_HOST_OVERRIDE: host.docker.internal

volumes:
  maven-cache:
```

And a `Makefile` to wrap Docker commands:

```makefile
DOCKER_COMPOSE = docker compose -f docker/docker-compose.yml
JAVA_RUN       = $(DOCKER_COMPOSE) run --rm --no-deps java-build
JAVA_RUN_TC    = $(DOCKER_COMPOSE) run --rm java-build-testcontainers

build-java:
	$(JAVA_RUN) mvn clean package -DskipTests

test-java:
	$(JAVA_RUN) mvn test

test-java-integration:
	$(JAVA_RUN_TC) mvn test -Dtest='*IntegrationTest'
```

---

## 3. Phase 1: Scaffold (1-2 days)

**Goal**: Establish module boundaries and get the structure test passing with your existing
code. No behavior changes in this phase.

### Step 1: Create the Module Marker Class

Every Spring Modulith project needs a root marker class. If your project is a standalone
application, your existing `@SpringBootApplication` class already serves this role. If you
are building a **library** (no main class), create a dedicated marker:

```java
// src/main/java/com/yourorg/yourservice/YourServiceModule.java
package com.yourorg.yourservice;

import org.springframework.modulith.Modulithic;

/**
 * Marker class for module boundary detection.
 * Used by Spring Modulith to discover module structure.
 */
@Modulithic
public final class YourServiceModule {
    private YourServiceModule() {
        // Marker class -- not instantiable
    }
}
```

From the reference project (`CustomerRegistryModule.java`):

```java
package com.onefinancial.customer;

import org.springframework.modulith.Modulithic;

@Modulithic
public final class CustomerRegistryModule {
    private CustomerRegistryModule() {}
}
```

The marker class must be in the **root package** of your module hierarchy. Spring Modulith
treats each direct sub-package as a separate module.

### Step 2: Create the Structure Test

This is the single most important artifact of the migration. It verifies that module
dependencies follow the rules you declare. Add it and run it continuously.

```java
// src/test/java/com/yourorg/yourservice/ModulithStructureTest.java
package com.yourorg.yourservice;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

class ModulithStructureTest {

    @Test
    void verifyModuleStructure() {
        ApplicationModules modules = ApplicationModules.of(YourServiceModule.class);
        modules.verify();
    }

    @Test
    void shouldHaveAtLeastOneModule() {
        ApplicationModules modules = ApplicationModules.of(YourServiceModule.class);
        assertThat(modules).isNotEmpty();
        modules.forEach(System.out::println);
    }
}
```

**Run it**: `make test-java` (or `docker compose run --rm java-build mvn test`).

The test will likely **fail** at this point because your existing code has cross-package
imports that violate the default module boundaries. That is expected and useful -- the
failures show you exactly where your coupling is.

### Step 3: Create the Core Module Package

Create a `core` package with a `package-info.java` that declares it as `Type.OPEN`:

```java
// src/main/java/com/yourorg/yourservice/core/package-info.java

/**
 * Core domain module.
 * Contains domain model, ports, SPIs, events, exceptions, and the domain service.
 * This module has ZERO infrastructure dependencies.
 *
 * Marked as OPEN because all sub-packages (model, port, spi, service, event, exception)
 * are part of the public API that other modules need to consume.
 */
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.OPEN,
    allowedDependencies = {}
)
package com.yourorg.yourservice.core;
```

Why `Type.OPEN`? By default, Spring Modulith only allows access to classes in the module's
**root package** (e.g., `com.yourorg.yourservice.core`). Sub-packages like `core.model` and
`core.port` would be hidden. Since core's sub-packages ARE the public API, `OPEN` exposes
them all.

### Step 4: Create Adapter Module Declarations

For each adapter, create a `package-info.java` that declares its allowed dependencies:

```java
// src/main/java/com/yourorg/yourservice/persistence/package-info.java

/**
 * JPA persistence adapter.
 * Implements the CustomerRepository port from the core module.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"core"}
)
package com.yourorg.yourservice.persistence;
```

```java
// src/main/java/com/yourorg/yourservice/rest/package-info.java

/**
 * REST API adapter.
 * Exposes domain operations via HTTP endpoints.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"core"}
)
package com.yourorg.yourservice.rest;
```

```java
// src/main/java/com/yourorg/yourservice/events/package-info.java

/**
 * Event publishing adapter.
 * Bridges domain events to Spring ApplicationEvents.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"core"}
)
package com.yourorg.yourservice.events;
```

### Step 5: Move Existing Code (Structure Only)

Move your existing classes into the appropriate packages. Do NOT refactor yet -- just
relocate:

```
BEFORE:                          AFTER:
com.yourorg.yourservice/         com.yourorg.yourservice/
  Customer.java                    YourServiceModule.java
  CustomerRepository.java           core/
  CustomerService.java                 package-info.java
  CustomerController.java             model/
  CustomerEntity.java                   Customer.java
                                       service/
                                         CustomerService.java
                                     persistence/
                                       package-info.java
                                       CustomerEntity.java
                                       CustomerRepository.java
                                     rest/
                                       package-info.java
                                       CustomerController.java
```

### Step 6: Run the Structure Test Again

The test will likely still fail because your service probably imports JPA types directly,
your controller imports entity classes, etc. **That is fine.** Record the failures -- they
are your Phase 2 work list.

**Expected state at end of Phase 1**: Structure test runs (may fail), module packages
exist, code is organized by module.

---

## 4. Phase 2: Core Extraction (3-5 days)

**Goal**: Make the core module infrastructure-free. All JPA, HTTP, and framework imports
must be removed from core.

### Identifying What Belongs in Core

Ask these questions for each class:

| Question | If YES -> | If NO -> |
|----------|-----------|----------|
| Does it represent a business concept? | `core/model/` | Not core |
| Does it define how to persist data? | `core/port/` (interface only) | Move implementation to adapter |
| Does it define how to publish events? | `core/port/` (interface only) | Move implementation to adapter |
| Can consumers extend this behavior? | `core/spi/` | Keep in core/service |
| Does it orchestrate business logic? | `core/service/` | Not core |
| Does it represent a domain event? | `core/event/` | Not core |
| Does it represent a business error? | `core/exception/` | Not core |

### Before/After: Extracting a JPA-Coupled Service

**BEFORE** -- Service directly depends on Spring Data JPA:

```java
package com.yourorg.yourservice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
class Customer {
    @Id
    private UUID id;
    private String name;
    private String document;
    // JPA requires no-args constructor
    protected Customer() {}
    // ... getters, setters
}

interface CustomerRepo extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByDocument(String document);
}

@Service
class CustomerService {
    @Autowired
    private CustomerRepo repo;

    public Customer register(Customer customer) {
        if (repo.findByDocument(customer.getDocument()).isPresent()) {
            throw new IllegalArgumentException("Duplicate document");
        }
        return repo.save(customer);
    }
}
```

**AFTER** -- Core module is infrastructure-free:

```java
// core/model/Customer.java -- pure domain object, no JPA annotations
package com.yourorg.yourservice.core.model;

public class Customer {
    private final UUID id;
    private String name;
    private Document document;

    public Customer(UUID id, String name, Document document) {
        this.id = id;
        this.name = name;
        this.document = document;
    }
    // ... getters, business methods
}
```

```java
// core/port/CustomerRepository.java -- interface, not Spring Data
package com.yourorg.yourservice.core.port;

import com.yourorg.yourservice.core.model.Customer;
import com.yourorg.yourservice.core.model.Document;

public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(UUID id);
    Optional<Customer> findByDocument(Document document);
    boolean existsByDocument(Document document);
}
```

```java
// core/service/CustomerService.java -- depends ONLY on ports and SPIs
package com.yourorg.yourservice.core.service;

import com.yourorg.yourservice.core.port.CustomerRepository;
import com.yourorg.yourservice.core.port.CustomerEventPublisher;
import com.yourorg.yourservice.core.spi.CustomerValidator;

public class CustomerService {

    private final List<CustomerValidator> validators;
    private final CustomerRepository repository;
    private final CustomerEventPublisher eventPublisher;

    // Constructor injection -- no @Autowired, no @Service
    public CustomerService(
            List<CustomerValidator> validators,
            CustomerRepository repository,
            CustomerEventPublisher eventPublisher) {
        this.validators = validators != null ? validators : List.of();
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Customer register(Customer customer) {
        runValidators(customer);
        checkDuplicate(customer.getDocument());
        Customer saved = repository.save(customer);
        eventPublisher.publish(CustomerCreated.of(saved.getId()));
        return saved;
    }
    // ...
}
```

Notice: the service class has **no Spring stereotype annotations** (`@Service`,
`@Component`). It is wired by the auto-configuration layer. The only Spring annotation
allowed is `@Transactional` because transaction boundaries are a cross-cutting concern.

### The Service Pipeline Pattern

From the reference project, the service follows a consistent pipeline:

```
validate all -> check duplicates -> enrich all -> persist -> publish event
```

Each step in the pipeline is delegated to an interface:
- **Validators** (`CustomerValidator` SPI): zero or more, all run before persistence
- **Enrichers** (`CustomerEnricher` SPI): zero or more, run after validation passes
- **Repository** (`CustomerRepository` port): exactly one implementation
- **Event publisher** (`CustomerEventPublisher` port): exactly one implementation

This pattern allows consumers to plug in custom behavior at each step without modifying
the service.

### Creating Port Interfaces

Ports define **what** the core needs from the outside world, not **how** it gets it.

```java
// core/port/CustomerRepository.java
package com.yourorg.yourservice.core.port;

public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(UUID id);
    Optional<Customer> findByDocument(Document document);
    boolean existsByDocument(Document document);
    CustomerPage findAll(int page, int size);
    void deleteById(UUID id);
}
```

```java
// core/port/CustomerEventPublisher.java
package com.yourorg.yourservice.core.port;

public interface CustomerEventPublisher {
    void publish(CustomerCreated event);
    void publish(CustomerUpdated event);
    void publish(CustomerStatusChanged event);
    void publish(CustomerDeleted event);
}
```

### Creating SPI Extension Points

SPIs (Service Provider Interfaces) are extension points that consumers can implement to
customize behavior. Use `@FunctionalInterface` for single-method SPIs:

```java
// core/spi/CustomerValidator.java
package com.yourorg.yourservice.core.spi;

@FunctionalInterface
public interface CustomerValidator {
    ValidationResult validate(Customer customer);
}

// core/spi/CustomerEnricher.java
package com.yourorg.yourservice.core.spi;

@FunctionalInterface
public interface CustomerEnricher {
    Customer enrich(Customer customer);
}
```

### When Ports Are Worth It vs. YAGNI

Ports add indirection. Make sure the indirection pays for itself:

| Create a port when... | Skip the port when... |
|------------------------|-----------------------|
| The module is consumed as a library | The service owns its own DB |
| You need multiple implementations (JPA, Mongo, in-memory) | Spring Data already provides the abstraction you need |
| You need an in-memory fake for fast testing | There will only ever be one implementation |
| The infrastructure choice may change | The service is Tier 1 with < 5 entities |

For **Tier 1** services, using Spring Data repositories directly in your service is fine.
Module boundaries (via `package-info.java`) already give you the main benefit.

### TDD Rhythm

```
1. Move class to new package
2. Run ModulithStructureTest -> FAIL (cross-module import)
3. Extract interface (port) in core
4. Implement adapter in persistence/rest/events package
5. Run ModulithStructureTest -> PASS
6. Write unit tests for service using mock ports
7. Run all tests -> GREEN
```

---

## 5. Phase 3: Adapters (3-5 days)

**Goal**: Create adapter implementations that satisfy the port interfaces defined in core.

### Persistence Adapter

The persistence adapter translates between domain models and JPA entities. It implements
the `CustomerRepository` port.

**Entity mapper pattern**: The domain model (`Customer`) and the JPA entity
(`CustomerEntity`) are separate classes. The adapter maps between them. This prevents JPA
annotations from leaking into the domain.

```java
// persistence/CustomerEntity.java (package-private)
package com.yourorg.yourservice.persistence;

@Entity
@Table(name = "cr_customer")
class CustomerEntity {
    @Id
    private UUID id;
    private String displayName;
    // JPA-specific: @Version, @JdbcTypeCode, etc.

    static CustomerEntity fromDomain(Customer customer) { /* ... */ }
    Customer toDomain() { /* ... */ }
}
```

```java
// persistence/CustomerJpaRepository.java (package-private)
package com.yourorg.yourservice.persistence;

interface CustomerJpaRepository extends JpaRepository<CustomerEntity, UUID> {
    Optional<CustomerEntity> findByDocumentValue(String documentValue);
}
```

```java
// persistence/CustomerRepositoryJpaAdapter.java (package-private)
package com.yourorg.yourservice.persistence;

class CustomerRepositoryJpaAdapter implements CustomerRepository {
    private final CustomerJpaRepository jpaRepository;

    CustomerRepositoryJpaAdapter(CustomerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Customer save(Customer customer) {
        CustomerEntity entity = CustomerEntity.fromDomain(customer);
        return jpaRepository.save(entity).toDomain();
    }
    // ... other methods delegate similarly
}
```

### The Bridge Configuration Pattern

This is the **key pattern** for Tier 2 and Tier 3 services. Here is the problem it solves:

1. Adapter classes (`CustomerRepositoryJpaAdapter`) are **package-private** -- they should
   not be directly visible outside the persistence module.
2. Auto-configuration classes live in the `autoconfigure` package and need to register
   these beans.
3. You **cannot** use `@ComponentScan` because it picks up `@SpringBootApplication` inner
   classes from tests.

**Solution**: Create a **public** `@Configuration` class inside the adapter package that
exposes the package-private beans. Auto-configuration then uses `@Import` to pull it in.

```java
// persistence/CustomerPersistenceConfiguration.java (PUBLIC bridge)
package com.yourorg.yourservice.persistence;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(basePackageClasses = CustomerPersistenceConfiguration.class)
@EntityScan(basePackageClasses = CustomerPersistenceConfiguration.class)
public class CustomerPersistenceConfiguration {

    @Bean
    @ConditionalOnMissingBean(CustomerRepository.class)
    public CustomerRepository customerRepositoryJpaAdapter(
            CustomerJpaRepository jpaRepository) {
        return new CustomerRepositoryJpaAdapter(jpaRepository);
    }
}
```

The bridge class is public but thin -- it just wires package-private beans. The `@Import`
in auto-configuration pulls it in cleanly:

```java
// autoconfigure/CustomerRegistryPersistenceAutoConfiguration.java
@AutoConfiguration(before = CustomerRegistryCoreAutoConfiguration.class)
@ConditionalOnProperty(
    prefix = "customer.registry",
    name = {"enabled", "features.persistence-jpa"},
    havingValue = "true"
)
@Import(CustomerPersistenceConfiguration.class)
public class CustomerRegistryPersistenceAutoConfiguration {
}
```

### Events Adapter

The events adapter bridges domain events to Spring `ApplicationEventPublisher`:

```java
// events/SpringEventPublisherAdapter.java (package-private)
package com.yourorg.yourservice.events;

class SpringEventPublisherAdapter implements CustomerEventPublisher {
    private final ApplicationEventPublisher publisher;

    SpringEventPublisherAdapter(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(CustomerCreated event) {
        publisher.publishEvent(event);
    }
    // ... other event methods
}
```

```java
// events/CustomerEventsConfiguration.java (PUBLIC bridge)
package com.yourorg.yourservice.events;

@Configuration(proxyBeanMethods = false)
public class CustomerEventsConfiguration {

    @Bean
    @ConditionalOnMissingBean(CustomerEventPublisher.class)
    public CustomerEventPublisher springEventPublisherAdapter(
            ApplicationEventPublisher applicationEventPublisher) {
        return new SpringEventPublisherAdapter(applicationEventPublisher);
    }
}
```

**Critical ordering**: The events auto-configuration MUST run BEFORE the core
auto-configuration. If core runs first, it registers the `NoOpEventPublisher` fallback
(because no `CustomerEventPublisher` bean exists yet), and the events adapter never gets
a chance to register.

```java
// This ordering is REQUIRED
@AutoConfiguration(before = CustomerRegistryCoreAutoConfiguration.class)
```

### REST Adapter

```java
// rest/CustomerController.java (package-private)
package com.yourorg.yourservice.rest;

@RestController
@RequestMapping("/api/v1/customers")
class CustomerController {
    private final CustomerRegistryService service;

    CustomerController(CustomerRegistryService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<CustomerResponse> create(
            @Valid @RequestBody CreateCustomerRequest request) {
        // Map request -> domain, call service, map domain -> response
    }
    // ...
}
```

```java
// rest/CustomerRestConfiguration.java (PUBLIC bridge)
package com.yourorg.yourservice.rest;

@Configuration(proxyBeanMethods = false)
public class CustomerRestConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "customerController")
    CustomerController customerController(CustomerRegistryService service) {
        return new CustomerController(service);
    }
}
```

### Testing Adapters: @WebMvcTest in a Library

When your project is a library (no `@SpringBootApplication`), `@WebMvcTest` does not know
how to bootstrap a context. Solution: add an inner `@SpringBootApplication` class in your
test:

```java
@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @SpringBootApplication
    static class TestApp {}

    @MockBean
    private CustomerRegistryService service;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn201WhenCustomerCreated() throws Exception {
        // ...
    }
}
```

### Troubleshooting: "Why Isn't My Bean Registered?"

```
Bean not found?
    |
    +-- Is the bridge @Configuration class PUBLIC?
    |       No -> Make it public
    |
    +-- Is it @Import-ed in the auto-configuration?
    |       No -> Add @Import(YourBridgeConfig.class)
    |
    +-- Is the auto-configuration listed in META-INF/spring/...imports?
    |       No -> Add the fully qualified class name
    |
    +-- Is the @ConditionalOnProperty satisfied?
    |       No -> Set the property in application.yml
    |
    +-- Does another auto-config run BEFORE and register a fallback?
            Yes -> Add @AutoConfiguration(before = ...) to run earlier
```

### Pattern Exceptions

The reference architecture has two modules that intentionally deviate from the standard
adapter patterns. Both are documented here so teams copying the reference know these are
deliberate exceptions, not oversights.

**`observability/` — No Bridge Configuration**

The observability module has a `CustomerObservabilityConfiguration.java` but its beans are
registered directly by the auto-configuration class rather than through the standard bridge
`@Import` pattern. This is intentional: observability beans (metrics, spans) are infrastructure
cross-cutting concerns that wrap the service, not adapters that implement a core port. They
do not need `@ConditionalOnMissingBean` overridability in the same way persistence or REST
adapters do.

**`migration/` — Depends on Core AND Persistence**

The migration module (`AttributeMigrationService`) depends on both `core` and `persistence`
because it must coordinate schema evolution with domain model changes. This is the only module
in the reference that has `allowedDependencies = {"core", "persistence"}`. This is acceptable
because:

1. Data migration is inherently tied to the storage layer being migrated
2. The module is gated by its own feature flag (`features.migrations`) and is optional
3. It does not establish a runtime dependency cycle — it only runs during schema upgrades

If your service does not need Liquibase-based data migration, omit this module entirely.

---

## 6. Phase 4: Auto-Configuration (2-3 days)

**Goal**: Wire everything together so consumers just add a dependency and set properties.

### Auto-Configuration Class Structure

Each feature gets its own `@AutoConfiguration` class with clear documentation:

```java
/**
 * ORDERING: Runs before CoreAutoConfiguration so Spring adapter registers
 *           before the NoOp fallback.
 * GATE: Disabled by default. Enable with customer.registry.features.publish-events=true
 * BRIDGE: Imports CustomerEventsConfiguration which exposes the package-private
 *         SpringEventPublisherAdapter.
 * OVERRIDABLE: Host apps can provide their own CustomerEventPublisher bean.
 */
@AutoConfiguration(before = CustomerRegistryCoreAutoConfiguration.class)
@ConditionalOnProperty(
    prefix = "customer.registry",
    name = {"enabled", "features.publish-events"},
    havingValue = "true"
)
@Import(CustomerEventsConfiguration.class)
public class CustomerRegistryEventsAutoConfiguration {
}
```

The structured header comment is mandatory. Every auto-configuration class must document:
- **ORDERING**: What it runs before/after and why
- **GATE**: What property enables it (default is always OFF)
- **BRIDGE**: What configuration it imports
- **OVERRIDABLE**: What beans consumers can replace

### Core Auto-Configuration with Fallbacks

The core auto-configuration provides fallback beans so the module works even when no
adapters are on the classpath:

```java
@AutoConfiguration
@ConditionalOnProperty(name = "customer.registry.enabled", havingValue = "true")
@EnableConfigurationProperties(CustomerRegistryProperties.class)
public class CustomerRegistryCoreAutoConfiguration {

    // Fallback: in-memory repository (no DB needed for prototyping)
    @Bean
    @ConditionalOnMissingBean
    public CustomerRepository customerRepository() {
        return new InMemoryCustomerRepository();
    }

    // Fallback: no-op event publisher (logs warnings)
    @Bean
    @ConditionalOnMissingBean
    public CustomerEventPublisher customerEventPublisher() {
        return new NoOpEventPublisher();
    }

    // The domain service -- always registered
    @Bean
    @ConditionalOnMissingBean
    public CustomerRegistryService customerRegistryService(
            List<CustomerValidator> validators,
            List<CustomerEnricher> enrichers,
            CustomerRepository repository,
            CustomerEventPublisher eventPublisher) {
        return new CustomerRegistryService(
            validators, enrichers, repository, eventPublisher);
    }
}
```

**Key principles**:
- `@ConditionalOnProperty`: Feature is OFF by default (secure-by-default)
- `@ConditionalOnMissingBean`: Every bean can be overridden by the host application
- Fallback beans: in-memory repository + no-op event publisher ensure the module works
  without infrastructure

### Property Configuration

Use `@ConfigurationProperties` with a clear prefix and sensible defaults:

```java
@ConfigurationProperties(prefix = "customer.registry")
public class CustomerRegistryProperties {
    private boolean enabled = false;        // Master switch
    private Features features = new Features();

    public static class Features {
        private boolean restApi = false;           // REST endpoints
        private boolean persistenceJpa = false;    // JPA persistence
        private boolean migrations = false;        // Liquibase migrations
        private boolean publishEvents = false;     // Event publishing
        private boolean observability = false;     // Metrics + tracing
    }
}
```

Consumer configuration in `application.yml`:

```yaml
customer:
  registry:
    enabled: true
    features:
      rest-api: true
      persistence-jpa: true
      migrations: true
      publish-events: true
      observability: true
```

### Registration in META-INF

Create the auto-configuration registration file:

```
# src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.yourorg.yourservice.autoconfigure.YourServiceCoreAutoConfiguration
com.yourorg.yourservice.autoconfigure.YourServicePersistenceAutoConfiguration
com.yourorg.yourservice.autoconfigure.YourServiceRestAutoConfiguration
com.yourorg.yourservice.autoconfigure.YourServiceEventsAutoConfiguration
com.yourorg.yourservice.autoconfigure.YourServiceObservabilityAutoConfiguration
```

Order in this file does NOT matter -- Spring resolves ordering from the `@AutoConfiguration`
`before`/`after` attributes.

### Property Gate Convention

All auto-configuration classes MUST use the **dual-gate pattern**:

```java
@ConditionalOnProperty(
    prefix = "customer.registry",
    name = {"enabled", "features.<feature-name>"},
    havingValue = "true"
)
```

This ensures that:
1. The **master switch** (`enabled`) disables all features at once
2. Each **feature flag** can be toggled independently
3. Both must be `true` for the auto-configuration to activate

Bean-level `@ConditionalOnProperty` within an auto-config (e.g., Liquibase in the persistence
auto-config) should also include the `enabled` gate for defense-in-depth, preventing bean
registration even if the parent class-level condition is somehow bypassed.

The core auto-configuration is the exception — it uses only the master switch
(`name = "enabled"`) because it provides fallback beans that must be available whenever
the registry is enabled, regardless of which features are active.

> **Note on property naming**: Feature flag property names use **kebab-case** in YAML
> (`persistence-jpa`, `publish-events`, `rest-api`). Spring Boot's relaxed binding
> automatically maps these to the camelCase fields in the `Features` class
> (`persistenceJpa`, `publishEvents`, `restApi`). Java annotations always reference
> the kebab-case form since they match the YAML keys. There is no risk of
> `matchIfMissing` appearing in kebab-case — it is a Java annotation attribute.

### CLI Verification Heuristics

The migration CLI uses static analysis heuristics (not a full Java parser) to verify
scorecard dimensions. Known limitations:

- **Comment detection**: Uses a single-pass heuristic that handles `//`, `/* */`, and
  Javadoc blocks. Deeply nested string literals containing comment delimiters (extremely
  rare in practice) may cause false positives.
- **@Bean/@ConditionalOnMissingBean pairing**: Coverage-based check within a 5-line
  effective distance. Annotations separated by large Javadoc blocks may appear unpaired
  even though they are logically associated.
- **Interface detection**: Searches for the `interface` keyword in non-comment lines.
  Interfaces inside inner classes or annotations are detected correctly, but dynamically
  generated interfaces are not.

- **@Bean/@ConditionalOnMissingBean effective distance**: The CLI considers annotations
  "paired" if `@ConditionalOnMissingBean` appears within 5 lines of a `@Bean` annotation.
  If your `@Bean` method has a large Javadoc block (> 5 lines) between the annotations,
  the CLI may report them as unpaired. Move the Javadoc above both annotations to fix this.

These heuristics are deliberately conservative — they may produce false warnings but should
not produce false passes. If a check fails unexpectedly, use `--verbose` for full details.

> **CLI Scope**: The `@ConditionalOnMissingBean` check applies to:
> - `*CoreAutoConfiguration.java` (core fallback beans)
> - `*Configuration.java` in `persistence/`, `rest/`, `events/` packages (bridge configs)
>
> Auto-configs for migration, observability, and Liquibase are **excluded** because their
> beans use different conditional patterns (`@ConditionalOnBean`, `@ConditionalOnClass`).

### Testing Auto-Configuration

Use `ApplicationContextRunner` to verify conditional behavior:

```java
class CustomerRegistryCoreAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            CustomerRegistryCoreAutoConfiguration.class));

    @Test
    void shouldNotRegisterBeansWhenDisabled() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(CustomerRegistryService.class);
        });
    }

    @Test
    void shouldRegisterFallbackBeansWhenEnabled() {
        runner.withPropertyValues("customer.registry.enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(CustomerRegistryService.class);
                assertThat(context).hasSingleBean(CustomerRepository.class);
                assertThat(context).hasSingleBean(CustomerEventPublisher.class);
            });
    }

    @Test
    void shouldAllowOverridingRepository() {
        runner.withPropertyValues("customer.registry.enabled=true")
            .withBean(CustomerRepository.class, () -> mock(CustomerRepository.class))
            .run(context -> {
                assertThat(context).hasSingleBean(CustomerRepository.class);
                // Verify it is the mock, not InMemoryCustomerRepository
            });
    }
}
```

---

## 7. Phase 5: Frontend Migration

If your service has an Angular frontend, follow these patterns from the reference project.

### Standalone Components with OnPush

All components use standalone mode and OnPush change detection:

```typescript
@Component({
  selector: 'crui-customer-search',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatFormFieldModule, MatInputModule],
  template: `...`
})
export class CustomerSearchComponent { }
```

### Signal-Based State Management

Use Angular signals instead of RxJS BehaviorSubjects for component state:

```typescript
@Injectable({ providedIn: 'root' })
export class CustomerStateService {
  // Private writable signals
  private readonly _customers = signal<Customer[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  // Public readonly signals
  readonly customers = this._customers.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
}
```

Use `asReadonly()` for the public API. Keep `WritableSignal` private with an `_` prefix.

### Configuration via InjectionToken

Allow consumers to configure the library via `InjectionToken` and a `provideXxx()` function:

```typescript
export const CUSTOMER_REGISTRY_CONFIG = new InjectionToken<CustomerRegistryConfig>(
  'CustomerRegistryConfig'
);

export function provideCustomerRegistry(
  config: Partial<CustomerRegistryConfig> = {}
): EnvironmentProviders {
  const merged = { ...DEFAULT_CONFIG, ...config };
  return makeEnvironmentProviders([
    { provide: CUSTOMER_REGISTRY_CONFIG, useValue: merged },
  ]);
}
```

### i18n with Fallback Chain

The reference project uses a three-level fallback chain for translations:

```
host app overrides -> built-in translations[locale] -> built-in translations['en'] -> key
```

This ensures the library always displays something meaningful, even when the host app
does not provide translations.

### Naming Conventions

- Angular selectors: `crui-` prefix (e.g., `crui-customer-search`)
- CSS custom properties: `--crui-*` prefix
- Avoid naming `@Input()` properties `formControl` -- it collides with Angular's
  `FormControlDirective` selector. Use `control` instead.

### Feature Flag Coverage Matrix

Not all feature flags affect all views. The table below documents which Angular
views/components are affected by each flag in the current version:

| Feature Flag | List View | Detail View | Form | Search |
|-------------|-----------|-------------|------|--------|
| `search` | - | - | - | Controls visibility |
| `addresses` | - | Controls address section | Not yet supported | - |
| `contacts` | - | Controls contact section | Not yet supported | - |
| `inlineEdit` | Reserved | Reserved | Reserved | Reserved |

**Key**:
- "Controls ..." = flag toggles visibility/behavior of that section
- "Not yet supported" = flag exists but has no effect in this view (future work)
- "Reserved" = flag is defined but not currently implemented
- "-" = flag has no effect on this view

When migrating a service with UI, map your feature flags to this matrix and document any
gaps. The `inlineEdit` flag is reserved for a future inline-editing capability.

### Public API Integration Guide

The Angular library exposes these entry points for host application integration:

**Providers** (use in `app.config.ts` or module providers):
- `provideCustomerRegistryUi(config)` — main configuration provider
- `provideCustomerRegistryI18n(overrides)` — i18n overrides
- `provideCustomerRegistryRenderers(registrations)` — custom field renderers

**Components** (use in templates):
- `CustomerListComponent` — paginated customer table
- `CustomerSearchComponent` — search form with type/status/document filters
- `CustomerFormComponent` — create/edit form with validation
- `CustomerDetailsComponent` — read-only detail view with addresses and contacts

**Services** (inject for programmatic access):
- `CustomerStateService` — signal-based state management
- `CustomerI18nService` — locale management and translation
- `CustomerRegistryApiClient` — HTTP client (configurable base URL)

**Tokens** (override for customization):
- `CUSTOMER_REGISTRY_UI_CONFIG` — full configuration object
- `CUSTOMER_I18N_OVERRIDES` — translation overrides per locale
- `CUSTOMER_UI_RENDERER_ERROR_REPORTER` — error reporting hook

---

## 8. Troubleshooting Guide

### Common Errors and Fixes

| Error | Cause | Fix |
|-------|-------|-----|
| "Module X depends on non-allowed module Y" | Missing `allowedDependencies` in `package-info.java` | Add the dependency: `allowedDependencies = {"core"}`. If the dependency should not exist, refactor to go through a core port. |
| Bean not registered at runtime | Bridge config not `@Import`-ed in auto-configuration | Add `@Import(YourBridgeConfig.class)` to the `@AutoConfiguration` class |
| Events do not override NoOp fallback | Wrong auto-configuration ordering | Add `@AutoConfiguration(before = CoreAutoConfig.class)` to events auto-config |
| `@ComponentScan` picks up test classes | Using `@ComponentScan` in auto-configuration | Replace with `@Import(BridgeConfig.class)` -- never use `@ComponentScan` in auto-config |
| `@DataJpaTest` fails with Liquibase error | `@DataJpaTest` auto-includes `LiquibaseAutoConfiguration` | Add `@TestPropertySource(properties = "spring.liquibase.enabled=false")` |
| Testcontainers fails from Docker | Missing socket mount in Docker Compose | Add `-v /var/run/docker.sock:/var/run/docker.sock` and set `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` |
| `@PathVariable` not resolved | Missing `-parameters` compiler flag | Add `<parameters>true</parameters>` to `maven-compiler-plugin` config |
| `UUID.nameUUIDFromBytes` generates wrong version | UUID v3 (MD5) vs v5 (SHA-1) confusion | Use `UUID.randomUUID()` for new entities or implement a proper v5 generator |
| PostgreSQL JSONB assertions fail | JSONB normalizes JSON (reorders keys, adds spaces) | Parse JSON before comparing, or use JSONAssert with `NON_EXTENSIBLE` mode |
| Hibernate 6 JSONB mapping fails | Using `@Convert` instead of `@JdbcTypeCode` | Use `@JdbcTypeCode(SqlTypes.JSON)` for PostgreSQL JSONB columns |
| Angular ng-packagr build fails | Empty `public-api.ts` barrel file | Ensure at least one export in `public-api.ts` |
| Jest config not found | Using `setupFilesAfterSetup` (wrong key) | Use `setupFilesAfterEnv` and ensure `ts-node` is a devDependency |

### Module Dependency Violations Decision Tree

When the `ModulithStructureTest` fails with a dependency violation:

```
Class A in module X imports Class B in module Y
    |
    +-- Should X be allowed to depend on Y?
    |       |
    |       +-- YES -> Add to allowedDependencies in X's package-info.java
    |       |
    |       +-- NO -> Is B part of the domain (model, event, exception)?
    |               |
    |               +-- YES -> Move B to core module
    |               |
    |               +-- NO -> Extract an interface in core/port/
    |                         Implement it in module Y
    |                         Inject the interface in class A
```

---

## 9. Copilot-Assisted Migration

### Setting Up Your Copilot Space

1. Include the reference project in your Copilot workspace so the AI has examples
2. Keep the `CLAUDE.md` / `.github/copilot-instructions.md` file in your project root
   with architecture rules and naming conventions
3. Include the `ModulithStructureTest` as context -- the AI can suggest fixes for
   structure violations

### AI Prompt Templates by Phase

**Phase 1 -- Create module scaffold**:
```
Create a Spring Modulith module structure for our [ServiceName] service.
The root package is [com.org.service]. We need these modules:
- core (Type.OPEN): model, port, spi, event, exception, service
- persistence: JPA adapter, depends on core only
- rest: REST controller, depends on core only
- events: event publishing, depends on core only
Create the @Modulithic marker class, package-info.java files, and
the ModulithStructureTest. Follow the patterns in the reference project.
```

**Phase 2 -- Extract core port**:
```
Extract a port interface from this JPA-coupled service:
[paste your existing service code]

The port should:
- Live in core/port/
- Have no JPA or Spring Data imports
- Use domain model types only
- Follow the pattern in CustomerRepository.java from the reference project
```

**Phase 3 -- Create bridge configuration**:
```
Create a bridge configuration for the persistence module.
The package-private adapter class is [ClassName].
It implements [PortInterface] from core/port/.
Follow the CustomerPersistenceConfiguration.java pattern:
- Public @Configuration in the adapter package
- @ConditionalOnMissingBean on the bean method
- @EnableJpaRepositories + @EntityScan with basePackageClasses
```

**Phase 4 -- Create auto-configuration**:
```
Create an auto-configuration class for the [feature] feature.
Requirements:
- Gated by customer.registry.features.[feature-name]=true
- Must run [before/after] CoreAutoConfiguration
- Imports [BridgeConfigClass]
- Include the structured header comment (ORDERING, GATE, BRIDGE, OVERRIDABLE)
- Follow CustomerRegistryEventsAutoConfiguration.java pattern
```

### What AI Catches vs. What Tests Enforce

| Concern | AI (Early Feedback) | Tests (Hard Gate) |
|---------|--------------------|--------------------|
| Module boundary violations | Suggests fixes during coding | `ModulithStructureTest.verify()` fails the build |
| Missing `@ConditionalOnMissingBean` | Warns if you forget | `ApplicationContextRunner` tests verify overridability |
| JPA imports in core | Flags during review | Compilation fails if port interface uses JPA types |
| Bridge config not `@Import`-ed | Suggests the import | Runtime: bean not found (integration test catches it) |
| Wrong auto-config ordering | Suggests `before`/`after` | Runtime: fallback bean wins (integration test catches it) |

AI provides early feedback during development. Tests are the hard gate in CI. Both are
necessary; neither is sufficient alone.

### Copilot Chat Migration Workflow

For a complete, step-by-step workflow using GitHub Copilot Chat with a multi-root VS Code
workspace (reference + legacy + target directories), see:

- **[Copilot Migration Strategy](copilot-migration-strategy.md)** -- Full process guide
  with workspace setup, phased execution, verification checklists, and troubleshooting
- **[Copilot Prompts Library](../migration/copilot-prompts.md)** -- Ready-to-paste prompts
  for each migration phase, with expected outputs and recovery instructions
- **[Migration Workflow Instructions](../migration/template/.github/instructions/migration-workflow.instructions.md)** --
  Instruction file that Copilot Chat reads automatically via `.github/instructions/`
- **[Lessons Learned Template](../migration/template/MIGRATION-LESSONS.md.template)** --
  Post-migration feedback template for continuous improvement of the migration framework

The Copilot Chat workflow follows the same phased approach (Phase 0-5) described above,
with explicit prompts designed to prevent common violations (JPA in core, public controllers,
`matchIfMissing = true`, `@ComponentScan` in auto-config, etc.).

---

## 10. Pragmatic vs. Strict Hexagonal

### Decision Matrix

| Question | If YES | If NO |
|----------|--------|-------|
| Is this consumed as a library by other teams? | Tier 3: full auto-config | Tier 1 or 2 |
| Does it have > 5 entities with complex rules? | Tier 2: full hexagonal | Tier 1: module boundaries only |
| Do you need multiple persistence implementations? | Port interfaces | Spring Data directly |
| Do consumers need to extend behavior? | SPIs (`@FunctionalInterface`) | Keep logic in service |
| Will others override your beans? | `@ConditionalOnMissingBean` | Standard `@Bean` |

### Entity Mapper: Always Needed?

**Strict hexagonal (Tier 2-3)**: Yes. Domain model and JPA entity are separate classes.
The adapter maps between them. This prevents JPA annotations from polluting the domain
and allows the domain model to evolve independently.

**Pragmatic (Tier 1)**: No. If the service owns its database and the entity IS the domain
model, using JPA annotations on the domain class is fine. The module boundaries already
prevent other modules from depending on JPA internals.

```
Tier 1: Customer.java has @Entity, @Id, @Table
        -> Simple, works fine for small services

Tier 2+: Customer.java is a pure domain class
          CustomerEntity.java has @Entity, @Id, @Table
          CustomerRepositoryAdapter maps between them
          -> Required when domain model != persistence model
```

### Bridge Configurations: When Necessary vs. Overkill

**Necessary (Tier 2-3)**:
- When you have `@AutoConfiguration` classes that need to import beans from adapter packages
- When adapter beans are package-private (good practice for encapsulation)
- When you want consumers to override any bean with `@ConditionalOnMissingBean`

**Overkill (Tier 1)**:
- When the service is a standalone application with `@SpringBootApplication`
- When component scanning already finds all beans
- When there is no auto-configuration layer

### The Tier 1 Minimal Migration

For simple services, the entire migration is:

1. Add `spring-modulith-starter-test` dependency
2. Create `package-info.java` files declaring module boundaries
3. Create `ModulithStructureTest`
4. Fix any violations the test finds (usually by moving classes to the right package)
5. Done. No ports, no adapters, no bridge configs, no auto-configuration.

This takes half a day and gives you enforced module boundaries in CI. Start here.
Escalate to Tier 2 or 3 only when you have a concrete reason.

---

## Appendix A: Complete File Listing for Reference

The reference project (OneFinancial Customer Registry) has this structure:

```
customer-registry-starter/src/main/java/com/onefinancial/customer/
    CustomerRegistryModule.java                    # @Modulithic marker

    core/
        package-info.java                          # Type.OPEN
        model/
            Customer.java, CustomerType.java, CustomerStatus.java,
            Document.java, Address.java, Contact.java, CustomerPage.java
        port/
            CustomerRepository.java                # Persistence port
            CustomerEventPublisher.java            # Event publishing port
        spi/
            CustomerValidator.java                 # Validation extension
            CustomerEnricher.java                  # Enrichment extension
            ValidationResult.java
        event/
            CustomerCreated.java, CustomerUpdated.java,
            CustomerStatusChanged.java, CustomerDeleted.java
        exception/
            CustomerNotFoundException.java
            CustomerValidationException.java
            DuplicateDocumentException.java
        service/
            CustomerRegistryService.java           # Domain service

    persistence/
        package-info.java                          # allowedDependencies = {"core"}
        CustomerPersistenceConfiguration.java      # PUBLIC bridge
        CustomerEntity.java                        # JPA entity (package-private)
        CustomerJpaRepository.java                 # Spring Data (package-private)
        CustomerRepositoryJpaAdapter.java          # Port implementation (package-private)

    rest/
        package-info.java                          # allowedDependencies = {"core"}
        CustomerRestConfiguration.java             # PUBLIC bridge
        CustomerController.java                    # REST controller (package-private)
        CustomerExceptionHandler.java              # Error handler (package-private)
        CreateCustomerRequest.java, UpdateCustomerRequest.java,
        CustomerResponse.java, CustomerPageResponse.java

    events/
        package-info.java                          # allowedDependencies = {"core"}
        CustomerEventsConfiguration.java           # PUBLIC bridge
        SpringEventPublisherAdapter.java           # Port implementation (package-private)

    observability/
        package-info.java                          # allowedDependencies = {"core"}
        CustomerObservabilityConfiguration.java    # PUBLIC bridge

    autoconfigure/
        CustomerRegistryCoreAutoConfiguration.java
        CustomerRegistryPersistenceAutoConfiguration.java
        CustomerRegistryRestAutoConfiguration.java
        CustomerRegistryEventsAutoConfiguration.java
        CustomerRegistryObservabilityAutoConfiguration.java
        CustomerRegistryProperties.java
        InMemoryCustomerRepository.java            # Fallback
        NoOpEventPublisher.java                    # Fallback

customer-registry-starter/src/main/resources/
    META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## Appendix B: Quick Reference Card

```
+------------------------------------------------------------------+
|  MIGRATION QUICK REFERENCE                                       |
+------------------------------------------------------------------+
|                                                                  |
|  TIER SELECTION                                                  |
|  < 5 entities, no complex rules    -> Tier 1 (boundaries only)  |
|  Business rules, events, adapters  -> Tier 2 (full hexagonal)   |
|  Consumed as library               -> Tier 3 (auto-config)      |
|                                                                  |
|  MODULE RULES                                                    |
|  core:          Type.OPEN, allowedDependencies = {}              |
|  adapters:      allowedDependencies = {"core"}                   |
|  autoconfigure: wires everything via @Import                     |
|                                                                  |
|  PATTERNS                                                        |
|  Bridge:    public @Config in adapter pkg -> @Import in autoconf |
|  Fallback:  InMemory + NoOp in autoconfigure pkg                 |
|  Pipeline:  validate -> deduplicate -> enrich -> persist -> emit |
|  Gate:      @ConditionalOnProperty (OFF by default)              |
|  Override:  @ConditionalOnMissingBean (on EVERY bean)            |
|  Ordering:  Adapters BEFORE core (register before fallback)      |
|                                                                  |
|  KEY FILES                                                       |
|  package-info.java          Module declaration                   |
|  *Configuration.java        Bridge config (public)               |
|  *AutoConfiguration.java    Wiring (in autoconfigure pkg)        |
|  *.imports                  META-INF registration                |
|  ModulithStructureTest.java THE test that enforces it all        |
|                                                                  |
+------------------------------------------------------------------+
```
