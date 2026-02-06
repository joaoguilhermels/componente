/**
 * Core domain module for Customer Registry.
 * <p>
 * Contains the domain model, ports (repository/event interfaces),
 * SPIs (validator/enricher extension points), and the domain service.
 * This module has zero infrastructure dependencies.
 *
 * @see com.oneff.customer.core.model.Customer
 * @see com.oneff.customer.core.service.CustomerRegistryService
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {}
)
package com.oneff.customer.core;
