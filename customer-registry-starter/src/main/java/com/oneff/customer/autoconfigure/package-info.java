/**
 * Spring Boot auto-configuration module for Customer Registry.
 * <p>
 * Wires up all Customer Registry beans based on feature-toggle properties.
 * Secure-by-default: all features are OFF unless explicitly enabled.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"core", "persistence", "migration", "rest", "events", "observability"}
)
package com.oneff.customer.autoconfigure;
