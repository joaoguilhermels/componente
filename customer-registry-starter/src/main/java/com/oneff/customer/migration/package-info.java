/**
 * JSONB attribute schema migration module.
 * <p>
 * Handles versioned migration of the JSONB attributes column,
 * using PostgreSQL advisory locks for cluster-safe coordination.
 */
// Depends on core + persistence — needs JPA entities to run JSONB attribute migrations.
// Only module allowed to depend on persistence (advisory lock coordination requires DB access).
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"core", "persistence"}
)
package com.oneff.customer.migration;
