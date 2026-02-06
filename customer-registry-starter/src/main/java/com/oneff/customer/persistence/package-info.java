/**
 * JPA persistence adapter for Customer Registry.
 * <p>
 * Implements the {@code CustomerRepository} port from the core module
 * using Spring Data JPA with PostgreSQL JSONB support.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"core"}
)
package com.oneff.customer.persistence;
