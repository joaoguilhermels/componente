---
applyTo: "**/persistence/**/*.java,**/rest/**/*.java,**/events/**/*.java,**/observability/**/*.java"
---

# Adapter Module Rules

Adapters implement ports defined in `core/port/` and depend inward only.
Each adapter module is a separate package under the base package.

## Module Declaration

Each adapter package must have a `package-info.java` declaring allowed
dependencies:

```java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"core"}
)
package <BASE_PACKAGE>.persistence;
```

## Bridge Configuration Pattern

Each adapter has a public `@Configuration` class (named `<Module>Configuration`,
e.g., `CustomerPersistenceConfiguration`, `CustomerRestConfiguration`,
`CustomerEventsConfiguration`) that exposes package-private beans. Auto-config
imports this class -- NEVER uses `@ComponentScan` (which picks up test inner
classes).

```java
// Public bridge -- imported by auto-config via @Import
@Configuration
public class CustomerPersistenceConfiguration {
    @Bean
    CustomerRepository customerRepository(CustomerJpaRepository jpaRepo,
                                          CustomerEntityMapper mapper) {
        return new JpaCustomerRepositoryAdapter(jpaRepo, mapper);
    }
}
```

The adapter classes themselves (controllers, repository adapters) should be
package-private.

## Persistence Adapter

- Entity classes are separate from domain model (use a mapper)
- Use `@JdbcTypeCode(SqlTypes.JSON)` for JSONB columns (NOT `@Convert`)
- Implement `Persistable<UUID>` to avoid extra SELECT on insert
- Use `@BatchSize(size = 25)` on `@OneToMany` collections to prevent N+1
- Add `@Version` for optimistic locking

```java
class JpaCustomerRepositoryAdapter implements CustomerRepository {
    private final CustomerJpaRepository jpaRepository;
    private final CustomerEntityMapper mapper;
    // ...
}
```

## REST Adapter

- Controllers are package-private (exposed via bridge config)
- `@WebMvcTest` in library requires a `@SpringBootApplication` inner class
- Spring Boot 3.2+ requires `-parameters` compiler flag for `@PathVariable`
  and `@RequestParam` without explicit names

```java
@RestController
@RequestMapping("/api/v1/customers")
class CustomerController {
    // package-private -- wired via bridge config
}
```

## Events Adapter

- Events auto-config MUST run BEFORE core:
  `@AutoConfiguration(before = CoreAutoConfiguration.class)`
- This ensures the Spring adapter registers before the NoOp fallback
- Never include PII in published events

## Observability Adapter

- Metrics use Micrometer counters/timers
- Gate with `@ConditionalOnClass(MeterRegistry.class)`
