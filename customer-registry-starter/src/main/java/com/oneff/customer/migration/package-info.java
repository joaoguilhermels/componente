/**
 * JSONB attribute schema migration module.
 * <p>
 * Handles versioned migration of the JSONB attributes column,
 * using PostgreSQL advisory locks for cluster-safe coordination.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"core", "persistence"}
)
package com.oneff.customer.migration;
