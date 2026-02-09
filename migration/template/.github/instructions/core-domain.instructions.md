---
applyTo: "**/core/**/*.java"
---

# Core Domain Rules

The `core/` package is the heart of the hexagonal architecture. It contains the
domain model, business logic, ports, SPIs, and events. It must have ZERO
infrastructure dependencies.

## Forbidden Imports

These imports are NOT allowed anywhere in `core/`:

```
jakarta.persistence.*
javax.persistence.*
org.springframework.data.*
org.springframework.web.*
org.springframework.jdbc.*
org.springframework.jms.*
org.springframework.kafka.*
org.springframework.amqp.*
```

The only Spring imports allowed are `org.springframework.modulith` (for module
annotations) and simple utility classes.

## Aggregate Roots

Model aggregate roots as classes (not records) with:
- Named static factory methods per variant (e.g., `Customer.createPF()`, `Customer.createPJ()`)
- Private constructor shared across factories
- Immutable ID assigned at creation (`UUID.randomUUID()`)
- Domain methods that enforce business rules and state transitions

```java
public final class Customer {
    private final UUID id;
    private String displayName;

    // Named factories per type -- NOT a generic create()
    public static Customer createPF(String cpfNumber, String displayName) {
        return create(CustomerType.PF, cpfNumber, displayName);
    }

    public static Customer createPJ(String cnpjNumber, String displayName) {
        return create(CustomerType.PJ, cnpjNumber, displayName);
    }

    private static Customer create(CustomerType type, String documentNumber, String displayName) {
        Objects.requireNonNull(displayName, "Display name must not be null");
        // validate, build Document, assign UUID
        return new Customer(UUID.randomUUID(), type, documentNumber, displayName);
    }
}
```

### Complexity Tiers

| Tier | When to Use | Example |
|------|-------------|---------|
| Simple | Single entity type, no variants | `create()` with direct validation |
| Standard | Multiple entity variants | `createPF()` / `createPJ()` with type-specific rules |
| Advanced | Lifecycle state machine | Status transitions with guard methods |

## Value Objects

Model value objects as Java records:

```java
public record Document(DocumentType type, String number) {
    public Document {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(number, "number must not be null");
    }
}
```

## Ports (`core/port/`)

Ports are interfaces that adapters implement. They define how the domain
interacts with the outside world.

```java
public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(UUID id);
    Optional<Customer> findByDocument(Document document);
}
```

## SPIs (`core/spi/`)

SPIs are extension points that the host application can implement to customize
behavior. They have default implementations.

## Events (`core/event/`)

Events are immutable records with `Objects.requireNonNull` validation on every
field. Never include PII (mask documents, omit sensitive fields).

```java
public record CustomerCreated(UUID customerId, String name, String maskedDocument) {
    public CustomerCreated {
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(maskedDocument, "maskedDocument must not be null");
    }
}
```

## Service Layer

Service pipeline: validate -> check duplicates -> enrich -> persist -> publish event.

No Spring stereotypes (`@Service`, `@Repository`, `@Component`) on domain classes.
They are wired by auto-configuration.
