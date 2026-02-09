/**
 * Event publishing module for Customer Registry.
 * <p>
 * Bridges domain events to Spring Modulith's event publication
 * mechanism using the outbox pattern for transactional guarantees.
 */
// Depends on core only — implements CustomerEventPublisher port via Spring ApplicationEvents (ADR-001).
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"core"}
)
package com.onefinancial.customer.events;
