/**
 * REST API module for Customer Registry.
 * <p>
 * Exposes the customer CRUD operations via a versioned REST API.
 * Gated by the {@code customer.registry.features.rest-api} property.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"core"}
)
package com.oneff.customer.rest;
