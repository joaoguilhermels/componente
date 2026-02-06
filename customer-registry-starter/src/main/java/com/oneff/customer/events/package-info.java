/**
 * Event publishing module for Customer Registry.
 * <p>
 * Bridges domain events to Spring Modulith's event publication
 * mechanism using the outbox pattern for transactional guarantees.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"core"}
)
package com.oneff.customer.events;
