/**
 * Core domain module for Customer Registry.
 * <p>
 * Contains the domain model, ports (repository/event interfaces),
 * SPIs (validator/enricher extension points), and the domain service.
 * This module has zero infrastructure dependencies.
 * <p>
 * Marked as OPEN because all sub-packages (model, port, spi, service, event, exception)
 * are part of the public API that other modules need to consume.
 *
 * @see com.oneff.customer.core.model.Customer
 * @see com.oneff.customer.core.service.CustomerRegistryService
 */
// OPEN so sub-packages (model, port, spi, service, event, exception) are visible to all modules.
// Empty allowedDependencies = core depends on NOTHING (hexagonal inner ring). See ADR-001.
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.OPEN,
    allowedDependencies = {}
)
package com.oneff.customer.core;
