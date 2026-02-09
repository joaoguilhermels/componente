/**
 * Spring Boot auto-configuration module for Customer Registry.
 * <p>
 * Wires up all Customer Registry beans based on feature-toggle properties.
 * Secure-by-default: all features are OFF unless explicitly enabled.
 */
// Depends on ALL modules — this is the wiring layer that imports bridge configs via @Import (ADR-002).
// Feature flags gate each dependency (ADR-003). This is the only module allowed to see all others.
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"core", "persistence", "migration", "rest", "events", "observability"}
)
package com.onefinancial.customer.autoconfigure;
