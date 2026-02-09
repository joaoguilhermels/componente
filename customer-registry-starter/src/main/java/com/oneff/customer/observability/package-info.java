/**
 * Observability module for Customer Registry.
 * <p>
 * Registers Micrometer metrics and OpenTelemetry spans
 * for customer operations and schema migrations.
 */
// Depends on core only — decorates service operations with metrics/spans. No adapter cross-deps (ADR-001).
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"core"}
)
package com.oneff.customer.observability;
