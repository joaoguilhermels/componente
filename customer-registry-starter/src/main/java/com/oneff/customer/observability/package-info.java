/**
 * Observability module for Customer Registry.
 * <p>
 * Registers Micrometer metrics and OpenTelemetry spans
 * for customer operations and schema migrations.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"core"}
)
package com.oneff.customer.observability;
